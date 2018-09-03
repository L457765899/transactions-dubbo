package com.sxb.lin.atomikos.dubbo.service;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

public interface DubboTransactionManagerService {
	
	final static int ADD_TIME = 3000;

	StartXid enlistResource(String remoteAddress,String uniqueResourceName,String tid,
			String localAddress) throws SystemException, RollbackException;
	
	int prepare(String remoteAddress, String uniqueResourceName, Xid xid) throws XAException;
	
	void commit(String remoteAddress, String uniqueResourceName, Xid xid, boolean onePhase) throws XAException;
	
	void rollback(String remoteAddress, String uniqueResourceName, Xid xid) throws XAException;
	
	Xid[] recover(String remoteAddress, String uniqueResourceName, int flag) throws XAException;
	
	long ping(String remoteAddress);
	
	Boolean wasCommitted(String remoteAddress, String tid, String uri);
}
