package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;
import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class DubboXAResourceImpl implements XAResource{
	
	private StartXid startXid;
	
	private String remoteAddress;

	public DubboXAResourceImpl(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
	
	public int prepare(Xid xid) throws XAException {
		return DubboTransactionManagerServiceProxy.getInstance().prepare(remoteAddress, xid);
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		DubboTransactionManagerServiceProxy.getInstance().commit(remoteAddress, xid, onePhase);
	}
	
	public void rollback(Xid xid) throws XAException {
		DubboTransactionManagerServiceProxy.getInstance().rollback(remoteAddress, xid);
	}
	
	public void start(Xid xid, int flags) throws XAException {
		startXid = new StartXid();
		startXid.setXid(xid);
		startXid.setFlags(flags);
	}

	public void end(Xid xid, int flags) throws XAException {
		//do nothing
	}
	
	public Xid[] recover(int flag) throws XAException {
		return DubboTransactionManagerServiceProxy.getInstance().recover(remoteAddress, flag);
	}

	public void forget(Xid xid) throws XAException {
		//do nothing
	}
	
	public boolean setTransactionTimeout(int seconds) throws XAException {
		//do nothing
		return false;
	}

	public int getTransactionTimeout() throws XAException {
		//do nothing
		return 0;
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		return this == xares;
	}

	public StartXid getStartXid() {
		return startXid;
	}
}
