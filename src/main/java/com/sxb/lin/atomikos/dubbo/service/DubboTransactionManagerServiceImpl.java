package com.sxb.lin.atomikos.dubbo.service;

import java.lang.reflect.Field;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import com.atomikos.datasource.TransactionalResource;
import com.atomikos.datasource.xa.XAResourceTransaction;
import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.icatch.jta.TransactionManagerImp;
import com.atomikos.recovery.CoordinatorLogEntry;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.ParticipantLogEntry;
import com.atomikos.recovery.RecoveryLog;
import com.atomikos.recovery.Repository;
import com.atomikos.recovery.TxState;
import com.atomikos.recovery.imp.RecoveryLogImp;
import com.sxb.lin.atomikos.dubbo.DubboXAResourceImpl;
import com.sxb.lin.atomikos.dubbo.DubboXATransactionalResource;
import com.sxb.lin.atomikos.dubbo.pool.XAResourcePool;

public class DubboTransactionManagerServiceImpl implements DubboTransactionManagerService{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DubboTransactionManagerServiceImpl.class);
	
	private XAResourcePool xaResourcePool;
	
	private DubboXATransactionalResource dubboXATransactionalResource;
	
	private String localAddress;
	
	private Repository repository;
		
	DubboTransactionManagerServiceImpl(XAResourcePool xaResourcePool,
			DubboXATransactionalResource dubboXATransactionalResource){
		this.xaResourcePool = xaResourcePool;
		this.dubboXATransactionalResource = dubboXATransactionalResource;
	}
	
	private Xid[] converXids(Xid[] xids){
		if(xids != null){
			int lenth = xids.length;
			DubboXid[] dubboXids = new DubboXid[lenth];
			for(int i = 0;i < lenth;i++){
				Xid xid = xids[i];
				DubboXid dubboXid = new DubboXid();
				dubboXid.setFormatId(xid.getFormatId());
				dubboXid.setBranchQualifier(xid.getBranchQualifier());
				dubboXid.setGlobalTransactionId(xid.getGlobalTransactionId());
				dubboXid.setBranchQualifierStr(new String(xid.getBranchQualifier()));
				dubboXid.setGlobalTransactionIdStr(new String(xid.getGlobalTransactionId()));
				dubboXids[i] = dubboXid;
			}
			return dubboXids;
		}
		return xids;
	}

	public StartXid enlistResource(String remoteAddress, String uniqueResourceName, String tid, 
			String localAddress) throws SystemException, RollbackException {
		
		TransactionManagerImp transactionManager = (TransactionManagerImp) TransactionManagerImp.getTransactionManager();
		Transaction transaction = transactionManager.getTransaction(tid);
		CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
		CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction(tid);
		DubboXAResourceImpl xaResource = new DubboXAResourceImpl(localAddress, uniqueResourceName);
		
		int status = transaction.getStatus();
		switch (status) {
		case Status.STATUS_MARKED_ROLLBACK:
		case Status.STATUS_ROLLEDBACK:
		case Status.STATUS_ROLLING_BACK:
			String msg = "Transaction rollback - enlisting more resources is useless.";
			LOGGER.warn(msg);
			throw new javax.transaction.RollbackException(msg);
		case Status.STATUS_COMMITTED:
		case Status.STATUS_PREPARED:
		case Status.STATUS_UNKNOWN:
			msg = "Enlisting more resources is no longer permitted: transaction is in state "
					+ compositeTransaction.getState();
			LOGGER.warn(msg);
			throw new IllegalStateException(msg);
		}

		long startTime = System.currentTimeMillis();
		long timeout = compositeTransaction.getTimeout() + DubboTransactionManagerService.ADD_TIME;
		TransactionalResource res = dubboXATransactionalResource.findOrCreateTransactionalResource(uniqueResourceName,startTime + timeout);
		XAResourceTransaction restx = (XAResourceTransaction) res.getResourceTransaction(compositeTransaction);
		restx.setXAResource(xaResource);
		restx.resume();

		StartXid startXid = xaResource.getStartXid();
		startXid.setStartTime(startTime);
		startXid.setTimeout(timeout);
		startXid.setTmAddress(remoteAddress);
		
		restx.suspend();
		
		return startXid;
	}

	public int prepare(String remoteAddress, String uniqueResourceName, Xid xid) throws XAException {
		if(StringUtils.hasLength(remoteAddress) && remoteAddress.equals(localAddress)){
			return xaResourcePool.prepare(xid);
		}else{
			throw new XAException("remoteAddress " + remoteAddress + " is error,can not prepare.");
		}
	}

	public void commit(String remoteAddress, String uniqueResourceName, Xid xid, boolean onePhase) throws XAException {
		if(remoteAddress == null){
			xaResourcePool.commit(xid, onePhase, uniqueResourceName);
		} else if(StringUtils.hasLength(remoteAddress) && remoteAddress.equals(localAddress)){
			xaResourcePool.commit(xid, onePhase, uniqueResourceName);
		} else {
			throw new XAException("remoteAddress " + remoteAddress + " is error,can not commit.");
		}
	}

	public void rollback(String remoteAddress, String uniqueResourceName, Xid xid) throws XAException {
		if(remoteAddress == null){
			xaResourcePool.rollback(xid, uniqueResourceName);
		} else if(StringUtils.hasLength(remoteAddress) && remoteAddress.equals(localAddress)){
			xaResourcePool.rollback(xid, uniqueResourceName);
		} else {
			throw new XAException("remoteAddress " + remoteAddress + " is error,can not rollback.");
		}
	}

	public Xid[] recover(String remoteAddress, String uniqueResourceName, int flag) throws XAException {
		Xid[] xids = xaResourcePool.recover(flag, uniqueResourceName);
		return this.converXids(xids);
	}

	public long ping(String remoteAddress) {
		if(StringUtils.hasLength(remoteAddress) && remoteAddress.equals(localAddress)){
			long currentTimeMillis = System.currentTimeMillis();
			LOGGER.warn("ping("+remoteAddress+") return "+currentTimeMillis);
			return currentTimeMillis;
		} else {
			return -1;
		}
	}

	public Boolean wasCommitted(String remoteAddress, String tid, String uri) {
		try {
			if(this.getRepository() != null){
				CoordinatorLogEntry coordinatorLogEntry = this.getRepository().get(tid);
				if(coordinatorLogEntry != null){
					if(coordinatorLogEntry.wasCommitted){
						for(ParticipantLogEntry entry : coordinatorLogEntry.participants){
							if(entry.coordinatorId.equals(tid) && entry.uri.equals(uri) && entry.state == TxState.COMMITTING){
								ParticipantLogEntry terminatedEntry = new ParticipantLogEntry(
										entry.coordinatorId,entry.uri,entry.expires,entry.resourceName,TxState.TERMINATED);
								this.getRecoveryLog().terminated(terminatedEntry);
							}
						}
					}
					return coordinatorLogEntry.wasCommitted;
				}
			}
		} catch (LogReadException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}
	
	public Repository getRepository() {
		if(repository != null){
			return repository;
		}
		RecoveryLog recoveryLog = this.getRecoveryLog();
		if(recoveryLog == null){
			return null;
		}
		Field findField = ReflectionUtils.findField(RecoveryLogImp.class, "repository");
		findField.setAccessible(true);
		try {
			this.repository = (Repository) findField.get(recoveryLog);
		} catch (IllegalArgumentException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return repository;
	}
	
	public RecoveryLog getRecoveryLog(){
		RecoveryLog recoveryLog = Configuration.getRecoveryLog();
		if(recoveryLog == null){
			return null;
		}
		return recoveryLog;
	}
}
