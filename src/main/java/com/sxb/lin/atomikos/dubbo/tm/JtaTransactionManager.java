package com.sxb.lin.atomikos.dubbo.tm;

import java.util.Collection;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.ParticipantLogEntry;
import com.atomikos.recovery.RecoveryLog;
import com.atomikos.recovery.TxState;
import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;


public class JtaTransactionManager extends org.springframework.transaction.jta.JtaTransactionManager 
	implements TerminatedCommittingTransaction{

	private static final long serialVersionUID = 1L;

	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition) 
			throws NotSupportedException,SystemException {
		
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			this.checkInitiatorXATransactionLocal();
			super.doJtaBegin(txObject, definition);
			this.newInitiatorXATransactionLocal();
		}else{
			if(current.getIsActive() != null && current.getIsActive().booleanValue() == false){
				this.checkInitiatorXATransactionLocal();
				super.doJtaBegin(txObject, definition);
				this.newInitiatorXATransactionLocal();
				return;
			}
			
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_NESTED.");
			}
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_REQUIRES_NEW.");
			}
			current.active();
		}
		
	}
	
	protected void checkInitiatorXATransactionLocal() throws NotSupportedException{
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			throw new NotSupportedException("can not begin,dubbo xa transaction already exists.");
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
	
	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			super.doCommit(status);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			super.doRollback(status);
		}
	}
	
	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			try {
				super.doCleanupAfterCompletion(transaction);
			} finally {
				this.restoreThreadLocalStatus();
			}
		}
	}

	private void restoreThreadLocalStatus(){
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			current.restoreThreadLocalStatus();
		}
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
			logger.error("JtaTransactionManager terminated committing transaction error", e);
		}
	}
	
}
