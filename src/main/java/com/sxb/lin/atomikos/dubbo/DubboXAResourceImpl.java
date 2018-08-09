package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;
import com.sxb.lin.atomikos.dubbo.service.DubboXid;
import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class DubboXAResourceImpl implements XAResource{
	
	private StartXid startXid;
	
	private String remoteAddress;
	
	private String uniqueResourceName;
	
	public DubboXAResourceImpl(String uniqueResourceName) {
		this(null,uniqueResourceName);
	}

	public DubboXAResourceImpl(String remoteAddress, String uniqueResourceName) {
		this.remoteAddress = remoteAddress;
		this.uniqueResourceName = uniqueResourceName;
	}
	
	public int prepare(Xid xid) throws XAException {
		return DubboTransactionManagerServiceProxy.getInstance().prepare(remoteAddress, uniqueResourceName, xid);
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		DubboTransactionManagerServiceProxy.getInstance().commit(remoteAddress, uniqueResourceName, xid, onePhase);
	}
	
	public void rollback(Xid xid) throws XAException {
		DubboTransactionManagerServiceProxy.getInstance().rollback(remoteAddress, uniqueResourceName, xid);
	}
	
	public void start(Xid xid, int flags) throws XAException {
		DubboXid dubboXid = new DubboXid();
		dubboXid.setFormatId(xid.getFormatId());
		dubboXid.setBranchQualifier(xid.getBranchQualifier());
		dubboXid.setGlobalTransactionId(xid.getGlobalTransactionId());
		dubboXid.setBranchQualifierStr(new String(xid.getBranchQualifier()));
		dubboXid.setGlobalTransactionIdStr(new String(xid.getGlobalTransactionId()));
		
		startXid = new StartXid();
		startXid.setXid(dubboXid);
		startXid.setFlags(flags);
	}

	public void end(Xid xid, int flags) throws XAException {
		//do nothing
	}
	
	public Xid[] recover(int flag) throws XAException {
		return DubboTransactionManagerServiceProxy.getInstance().recover(remoteAddress, uniqueResourceName, flag);
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
