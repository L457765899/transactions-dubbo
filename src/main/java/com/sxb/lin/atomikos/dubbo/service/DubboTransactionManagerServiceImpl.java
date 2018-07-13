package com.sxb.lin.atomikos.dubbo.service;

import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

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

public class DubboTransactionManagerServiceImpl implements DubboTransactionManagerService{
	
	private static final Logger LOGGER = LoggerFactory.createLogger(DubboTransactionManagerServiceImpl.class);
	
	DubboTransactionManagerServiceImpl(){
		
	}
	
	private TransactionalResource findTransactionalResource(String remoteAddress,String uniqueResourceName) {
		
		TransactionalResource ret = null;
		
		synchronized (Configuration.class) {
			ret = (TransactionalResource) Configuration.getResource(uniqueResourceName);
			if (ret == null || ret.isClosed()) {
				ret = new DubboXATransactionalResource(remoteAddress,uniqueResourceName);
				Configuration.addResource(ret);
			}
		}

		return ret;
	}

	public StartXid enlistResource(String remoteAddress,String tid,String localAddress,String uniqueResourceName) 
			throws SystemException, RollbackException {
		
		TransactionManagerImp transactionManager = (TransactionManagerImp) TransactionManagerImp.getTransactionManager();
		Transaction transaction = transactionManager.getTransaction(tid);
		CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
		CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction(tid);
		DubboXAResourceImpl xaResource = new DubboXAResourceImpl(localAddress);
		
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

		TransactionalResource res = this.findTransactionalResource(localAddress,uniqueResourceName);
		XAResourceTransaction restx = (XAResourceTransaction) res.getResourceTransaction(compositeTransaction);
		restx.setXAResource(xaResource);
		restx.resume();
		
		StartXid startXid = xaResource.getStartXid();
		
		restx.suspend();
		
		return startXid;
	}

	public int prepare(String remoteAddress, Xid xid) throws XAException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void commit(String remoteAddress, Xid xid, boolean onePhase) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public void rollback(String remoteAddress, Xid xid) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public Xid[] recover(String remoteAddress, int flag) throws XAException {
		// TODO Auto-generated method stub
		return null;
	}

}
