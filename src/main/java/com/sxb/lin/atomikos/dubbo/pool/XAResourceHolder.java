package com.sxb.lin.atomikos.dubbo.pool;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.sxb.lin.atomikos.dubbo.service.StartXid;


public interface XAResourceHolder {
	
	public final static int XA_UNKNOWN = 0;
	
	public final static int XA_START = 1;
	
	public final static int XA_END = 2;
	
	public final static int XA_PREPARE = 3;
	
	public final static int XA_COMMIT = 4;
	
	public final static int XA_ROLLBACK = 5;
	
	public final static int CLOSE = 6;
	
	void start() throws XAException, SystemException, RollbackException;
	
	void end() throws XAException;
	
	int prepare(Xid xid) throws XAException;
	
	void commit(Xid xid, boolean onePhase) throws XAException;
	
	void rollback(Xid xid) throws XAException;
	
	String getUuid();
	
	StartXid getStartXid();
	
	String getTmAddress();
	
	void close();
}
