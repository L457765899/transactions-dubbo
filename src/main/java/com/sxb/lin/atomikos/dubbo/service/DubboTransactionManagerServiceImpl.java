package com.sxb.lin.atomikos.dubbo.service;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.springframework.util.StringUtils;

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
	
	private DubboXATransactionalResource dubboXATransactionalResource;
	
	private String localAddress;
		
	DubboTransactionManagerServiceImpl(XAResourcePool xaResourcePool,
			DubboXATransactionalResource dubboXATransactionalResource){
		this.xaResourcePool = xaResourcePool;
		this.dubboXATransactionalResource = dubboXATransactionalResource;
	}
	
	private TransactionalResource findOrCreateTransactionalResource(String uniqueResourceName,long timeout) {
		RecoverableResource resource = Configuration.getResource(uniqueResourceName);
		TransactionalResource ret = null;
		if(resource == null){
			ret = dubboXATransactionalResource.createTransactionalResource(uniqueResourceName,timeout);
		} else {
			ret = (TransactionalResource) resource;
		}
		return ret;
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
		TransactionalResource res = this.findOrCreateTransactionalResource(uniqueResourceName,timeout);
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
		return xaResourcePool.recover(flag, uniqueResourceName);
	}

	public long ping(String remoteAddress) {
		if(StringUtils.hasLength(remoteAddress) && remoteAddress.equals(localAddress)){
			long currentTimeMillis = System.currentTimeMillis();
			LOGGER.logWarning("ping("+remoteAddress+") return "+currentTimeMillis);
			return currentTimeMillis;
		} else {
			return -1;
		}
	}

	public String getLocalAddress() {
		return localAddress;
	}

	public void setLocalAddress(String localAddress) {
		this.localAddress = localAddress;
	}

}
