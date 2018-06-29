package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class XAResourceImpl implements XAResource{

	public void commit(Xid xid, boolean onePhase) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public void end(Xid xid, int flags) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public void forget(Xid xid) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public int getTransactionTimeout() throws XAException {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		// TODO Auto-generated method stub
		return false;
	}

	public int prepare(Xid xid) throws XAException {
		// TODO Auto-generated method stub
		return 0;
	}

	public Xid[] recover(int flag) throws XAException {
		// TODO Auto-generated method stub
		return null;
	}

	public void rollback(Xid xid) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public boolean setTransactionTimeout(int seconds) throws XAException {
		// TODO Auto-generated method stub
		return false;
	}

	public void start(Xid xid, int flags) throws XAException {
		// TODO Auto-generated method stub
		
	}

}
