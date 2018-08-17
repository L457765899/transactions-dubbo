package com.sxb.lin.atomikos.dubbo.tm;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;

import javax.sql.XADataSource;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.JdbcTransactionObjectSupport;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.HeuristicCompletionException;
import org.springframework.transaction.NestedTransactionNotSupportedException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.ParticipantLogEntry;
import com.atomikos.recovery.RecoveryLog;
import com.atomikos.recovery.TxState;
import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.annotation.XA;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;
import com.sxb.lin.atomikos.dubbo.spring.InitiatorXADataSourceUtils;
import com.sxb.lin.atomikos.dubbo.spring.XAConnectionHolder;
import com.sxb.lin.atomikos.dubbo.spring.XAInvocationLocal;

public class DataSourceTransactionManager extends org.springframework.jdbc.datasource.DataSourceTransactionManager 
	implements TerminatedCommittingTransaction{

	private static final long serialVersionUID = 1L;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceTransactionManager.class);
	
	private transient UserTransaction userTransaction;
	
	private transient TransactionManager transactionManager;

	@Override
	protected void doBegin(Object transaction, TransactionDefinition definition) {
		try {
			ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
			if(current == null){
				this.checkInitiatorXATransactionLocal();
				if(this.isUseJta()){
					this.doJtaBegin(transaction, definition);
					this.newInitiatorXATransactionLocal();
				}else{
					super.doBegin(transaction, definition);
				}
			}else{
				if(current.getIsActive() != null && current.getIsActive().booleanValue() == false){
					this.checkInitiatorXATransactionLocal();
					if(this.isUseJta()){
						this.doJtaBegin(transaction, definition);
						this.newInitiatorXATransactionLocal();
					}else{
						super.doBegin(transaction, definition);
					}
					return;
				}
				
				if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
					throw new NestedTransactionNotSupportedException("dubbo xa transaction not supported PROPAGATION_NESTED.");
				}
				if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW){
					throw new NestedTransactionNotSupportedException("dubbo xa transaction not supported PROPAGATION_REQUIRES_NEW.");
				}
				current.active();
			}
		} catch (NotSupportedException e) {
			throw new CannotCreateTransactionException("JTA failure on begin", e);
		} catch(SystemException e) {
			throw new CannotCreateTransactionException("JTA failure on begin", e);
		} catch (SQLException e) {
			throw new CannotCreateTransactionException("JTA failure on begin", e);
		} 
	}
	
	protected void doJtaBegin(Object transaction, TransactionDefinition definition) 
			throws SQLException, NotSupportedException, SystemException {
		JdbcTransactionObjectSupport txObject = (JdbcTransactionObjectSupport) transaction;
		if (txObject.hasConnectionHolder()) {
			throw new CannotCreateTransactionException("can not create transaction,xa connection already exists");
		}
		
		int timeout = determineTimeout(definition);
		if (timeout != TransactionDefinition.TIMEOUT_DEFAULT) {
			userTransaction.setTransactionTimeout(timeout);
		}
		userTransaction.begin();
		
		XADataSource xaDataSource = (XADataSource) this.getDataSource();
		txObject.setConnectionHolder(InitiatorXADataSourceUtils.getXAConnection(xaDataSource));
	}
	
	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			if(this.isUseInitiatorXATransactionLocal()){
				this.doJtaCommit();
			}else{
				super.doCommit(status);
			}
		}
	}
	
	protected void doJtaCommit(){
		try {
			int jtaStatus = userTransaction.getStatus();
			if (jtaStatus == Status.STATUS_NO_TRANSACTION) {
				// Should never happen... would have thrown an exception before
				// and as a consequence led to a rollback, not to a commit call.
				// In any case, the transaction is already fully cleaned up.
				throw new UnexpectedRollbackException("JTA transaction already completed - probably rolled back");
			}
			if (jtaStatus == Status.STATUS_ROLLEDBACK) {
				// Only really happens on JBoss 4.2 in case of an early timeout...
				// Explicit rollback call necessary to clean up the transaction.
				// IllegalStateException expected on JBoss; call still necessary.
				try {
					userTransaction.rollback();
				}
				catch (IllegalStateException ex) {
					if (logger.isDebugEnabled()) {
						logger.debug("Rollback failure with transaction already marked as rolled back: " + ex);
					}
				}
				throw new UnexpectedRollbackException("JTA transaction already rolled back (probably due to a timeout)");
			}
			userTransaction.commit();
		}
		catch (RollbackException ex) {
			throw new UnexpectedRollbackException(
					"JTA transaction unexpectedly rolled back (maybe due to a timeout)", ex);
		}
		catch (HeuristicMixedException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_MIXED, ex);
		}
		catch (HeuristicRollbackException ex) {
			throw new HeuristicCompletionException(HeuristicCompletionException.STATE_ROLLED_BACK, ex);
		}
		catch (IllegalStateException ex) {
			throw new TransactionSystemException("Unexpected internal transaction state", ex);
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on commit", ex);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			if(this.isUseInitiatorXATransactionLocal()){
				this.doJtaRollback();
			}else{
				super.doRollback(status);
			}
		}
	}
	
	protected void doJtaRollback(){
		try {
			int jtaStatus = userTransaction.getStatus();
			if (jtaStatus != Status.STATUS_NO_TRANSACTION) {
				try {
					userTransaction.rollback();
				}
				catch (IllegalStateException ex) {
					if (jtaStatus == Status.STATUS_ROLLEDBACK) {
						// Only really happens on JBoss 4.2 in case of an early timeout...
						if (logger.isDebugEnabled()) {
							logger.debug("Rollback failure with transaction already marked as rolled back: " + ex);
						}
					}
					else {
						throw new TransactionSystemException("Unexpected internal transaction state", ex);
					}
				}
			}
		}
		catch (SystemException ex) {
			throw new TransactionSystemException("JTA failure on rollback", ex);
		}
	}
	
	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			if(this.isUseInitiatorXATransactionLocal()){
				this.restoreThreadLocalStatus();
			}else{
				super.doCleanupAfterCompletion(transaction);
			}
		}
	}
	
	@Override
	protected void prepareSynchronization(DefaultTransactionStatus status,
			TransactionDefinition definition) {
		super.prepareSynchronization(status, definition);
		if (status.isNewSynchronization()) {
			XADataSource xaDataSource = (XADataSource) this.getDataSource();
			InitiatorXADataSourceUtils.registerSynchronization(xaDataSource);
		}
	}

	@Override
	protected boolean shouldCommitOnGlobalRollbackOnly() {
		return true;
	}
	
	@Override
	protected void prepareForCommit(DefaultTransactionStatus status) {
		JdbcTransactionObjectSupport transaction = (JdbcTransactionObjectSupport) status.getTransaction();
		XADataSource xaDataSource = (XADataSource) this.getDataSource();
		Object resource = TransactionSynchronizationManager.getResource(xaDataSource);
		if(resource instanceof XAConnectionHolder){
			XAConnectionHolder holder = (XAConnectionHolder) resource;
			transaction.setConnectionHolder(holder);
		}
	}

	protected boolean isUseJta(){
		XAInvocationLocal current = XAInvocationLocal.current();
		if(current != null){
			Method method = current.getMethod();
			try {
				Method methodImpl = current.getTargetClass().getMethod(method.getName(), method.getParameterTypes());
				XA annotation = methodImpl.getAnnotation(XA.class);
				current.clear();
				if(annotation != null){
					return true;
				}
			} catch (NoSuchMethodException e) {
				LOGGER.error(e.getMessage(),e);
			} catch (SecurityException e) {
				LOGGER.error(e.getMessage(),e);
			}
		}
		return false;
	}
	
	private void restoreThreadLocalStatus(){
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			current.restoreThreadLocalStatus();
		}
	}
	
	protected boolean isUseInitiatorXATransactionLocal(){
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			return true;
		}
		return false;
	}
	
	protected void checkInitiatorXATransactionLocal() {
		if(this.isUseInitiatorXATransactionLocal()){
			throw new CannotCreateTransactionException("can not begin,dubbo xa transaction already exists.");
		}
	}

	protected void newInitiatorXATransactionLocal() {
		DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();
		CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
		CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
		
		String tid = compositeTransaction.getTid();
		
		InitiatorXATransactionLocal local = new InitiatorXATransactionLocal();
		local.setTid(tid);
		local.setTmAddress(instance.getLocalAddress());
		local.bindToThread();
	}

	public void terminated(String tid) {
		RecoveryLog log = Configuration.getRecoveryLog();
		try {
			Collection<ParticipantLogEntry> entries = log.getCommittingParticipants();
			for (ParticipantLogEntry entry : entries) {
				if(tid.equals(entry.coordinatorId) && entry.expires < System.currentTimeMillis()){
					ParticipantLogEntry terminatedEntry = new ParticipantLogEntry(
							entry.coordinatorId,entry.uri,entry.expires,entry.resourceName,TxState.TERMINATED);
					log.terminated(terminatedEntry);
				}
			}
		} catch (LogReadException e) {
			LOGGER.error("DataSourceTransactionManager terminated committing transaction error", e);
		}
	}
	
	@Override
	public void afterPropertiesSet() {
		if (getDataSource() == null) {
			throw new IllegalArgumentException("Property 'dataSource' is required");
		}
		if (!(getDataSource() instanceof XADataSource)) {
			throw new IllegalArgumentException("Property 'dataSource' must be supported xaDataSource");
		}
		if (getUserTransaction() == null) {
			throw new IllegalArgumentException("Property 'userTransaction' is required");
		}
		if (getTransactionManager() == null) {
			throw new IllegalArgumentException("Property 'transactionManager' is required");
		}
	}

	public UserTransaction getUserTransaction() {
		return userTransaction;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	public TransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

}
