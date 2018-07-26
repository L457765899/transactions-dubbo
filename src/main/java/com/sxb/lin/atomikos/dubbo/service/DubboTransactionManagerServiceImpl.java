package com.sxb.lin.atomikos.dubbo.service;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.atomikos.datasource.RecoverableResource;
import com.atomikos.datasource.TransactionalResource;
import com.atomikos.datasource.xa.XAResourceTransaction;
import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.icatch.jta.TransactionManagerImp;
import com.atomikos.logging.Logger;
import com.atomikos.logging.LoggerFactory;
import com.sxb.lin.atomikos.dubbo.DubboXAResourceImpl;
import com.sxb.lin.atomikos.dubbo.DubboXATransactionalResource;
import com.sxb.lin.atomikos.dubbo.pool.XAResourcePool;

public class DubboTransactionManagerServiceImpl implements DubboTransactionManagerService{
	
	private static final Logger LOGGER = LoggerFactory.createLogger(DubboTransactionManagerServiceImpl.class);
	
	private XAResourcePool xaResourcePool;
		
	DubboTransactionManagerServiceImpl(XAResourcePool xaResourcePool){
		this.xaResourcePool = xaResourcePool;
	}
	
	private TransactionalResource findTransactionalResource(String remoteAddress,String uniqueResourceName,
			long startTime,long timeout) {
		
		long expiresTime = startTime + timeout;
		synchronized (uniqueResourceName.intern()) {
			RecoverableResource resource = Configuration.getResource(uniqueResourceName);
			TransactionalResource ret = null;
			if(resource == null){
				ret = new DubboXATransactionalResource(uniqueResourceName, remoteAddress, expiresTime);
				Configuration.addResource(ret);
			} else if (resource instanceof DubboXATransactionalResource){
				DubboXATransactionalResource dret = (DubboXATransactionalResource) resource;
				if(dret.isClosed()){
					ret = new DubboXATransactionalResource(uniqueResourceName, remoteAddress, expiresTime);
					Configuration.addResource(ret);
				}else{
					if(expiresTime > dret.getExpiresTime()){
						dret.setExpiresTime(expiresTime);
					}
					ret = dret;
				}
			} else {
				ret = (TransactionalResource) resource;
			}
			
			return ret;
		}
		
	}

	public StartXid enlistResource(String remoteAddress,String tid,String localAddress,String uniqueResourceName) 
			throws SystemException, RollbackException {
		
		TransactionManagerImp transactionManager = (TransactionManagerImp) TransactionManagerImp.getTransactionManager();
		Transaction transaction = transactionManager.getTransaction(tid);
		CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
		CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction(tid);
		DubboXAResourceImpl xaResource = new DubboXAResourceImpl(localAddress,tid,uniqueResourceName);
		
		int status = transaction.getStatus();
		switch (status) {
		case Status.STATUS_MARKED_ROLLBACK:
		case Status.STATUS_ROLLEDBACK:
		case Status.STATUS_ROLLING_BACK:
			String msg = "Transaction rollback - enlisting more resources is useless.";
			LOGGER.logWarning(msg);
			throw new javax.transaction.RollbackException(msg);
		case Status.STATUS_COMMITTED:
		case Status.STATUS_PREPARED:
		case Status.STATUS_UNKNOWN:
			msg = "Enlisting more resources is no longer permitted: transaction is in state "
					+ compositeTransaction.getState();
			LOGGER.logWarning(msg);
			throw new IllegalStateException(msg);
		}

		long startTime = System.currentTimeMillis();
		long timeout = compositeTransaction.getTimeout() + 3000;
		TransactionalResource res = this.findTransactionalResource(localAddress,uniqueResourceName,startTime,timeout);
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

	public int prepare(String remoteAddress, Xid xid, String tid, String uniqueResourceName) throws XAException {
		return xaResourcePool.prepare(xid);
	}

	public void commit(String remoteAddress, Xid xid, boolean onePhase, String tid, String uniqueResourceName) throws XAException {
		xaResourcePool.commit(xid, onePhase);
	}

	public void rollback(String remoteAddress, Xid xid, String tid, String uniqueResourceName) throws XAException {
		xaResourcePool.rollback(xid);
	}

	public Xid[] recover(String remoteAddress, int flag, String uniqueResourceName) throws XAException {
		return xaResourcePool.recover(flag, uniqueResourceName);
	}

	public int ping(String remoteAddress) {
		return 0;
	}

}
