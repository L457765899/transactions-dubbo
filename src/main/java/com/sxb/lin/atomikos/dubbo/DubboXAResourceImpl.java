package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.springframework.util.Assert;

import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;
import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class DubboXAResourceImpl implements XAResource{
	
	private StartXid startXid;
	
	private String tid;
	
	private String remoteAddress;
	
	private String uniqueResourceName;
	
	public DubboXAResourceImpl(String remoteAddress,String uniqueResourceName) {
		this.remoteAddress = remoteAddress;
		this.uniqueResourceName = uniqueResourceName;
	}

	public DubboXAResourceImpl(String remoteAddress,String tid,String uniqueResourceName) {
		this.tid = tid;
		this.remoteAddress = remoteAddress;
		this.uniqueResourceName = uniqueResourceName;
	}
	
	public int prepare(Xid xid) throws XAException {
		Assert.notNull(tid, "xa prepare tid not null.");
		return DubboTransactionManagerServiceProxy.getInstance().prepare(remoteAddress, xid, tid, uniqueResourceName);
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		Assert.notNull(tid, "xa commit tid not null.");
		DubboTransactionManagerServiceProxy.getInstance().commit(remoteAddress, xid, onePhase, tid, uniqueResourceName);
	}
	
	public void rollback(Xid xid) throws XAException {
		Assert.notNull(tid, "xa rollback tid not null.");
		DubboTransactionManagerServiceProxy.getInstance().rollback(remoteAddress, xid, tid, uniqueResourceName);
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
		return DubboTransactionManagerServiceProxy.getInstance().recover(remoteAddress, flag, uniqueResourceName);
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
