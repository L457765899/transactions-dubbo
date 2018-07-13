package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;

public class DubboXATransactionalResource extends XATransactionalResource{
	
	private String remoteAddress;

	public DubboXATransactionalResource(String servername,String remoteAddress) {
		super(servername);
		this.remoteAddress = remoteAddress;
	}

	@Override
	protected synchronized XAResource refreshXAConnection() throws ResourceException {
		return new DubboXAResourceImpl(remoteAddress);
	}

}
