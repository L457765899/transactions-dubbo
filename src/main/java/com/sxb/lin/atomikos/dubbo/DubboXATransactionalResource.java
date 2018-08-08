package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;
import com.atomikos.icatch.config.Configuration;

public class DubboXATransactionalResource extends XATransactionalResource{
	
	private String uniqueResourceName;
	
	private long expiresTime;

	public DubboXATransactionalResource(String uniqueResourceName, long expiresTime) {
		super(uniqueResourceName);
		this.uniqueResourceName = uniqueResourceName;
		this.expiresTime = expiresTime;
	}

	@Override
	protected synchronized XAResource refreshXAConnection() throws ResourceException {
		return new DubboXAResourceImpl(uniqueResourceName);
	}

	@Override
	public boolean isClosed() throws ResourceException {
		return expiresTime < System.currentTimeMillis();
	}

	public long getExpiresTime() {
		return expiresTime;
	}

	public void setExpiresTime(long expiresTime) {
		this.expiresTime = expiresTime;
	}

	@Override
	public void recover() {
		super.recover();
		synchronized (uniqueResourceName.intern()) {
			if(this.isClosed()){
				Configuration.removeResource(uniqueResourceName);
			}
		}
	}

}
