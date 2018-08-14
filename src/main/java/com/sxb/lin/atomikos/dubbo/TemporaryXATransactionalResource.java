package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;

public class TemporaryXATransactionalResource extends XATransactionalResource{
	
	private String uniqueResourceName;

	TemporaryXATransactionalResource(String uniqueResourceName) {
		super(uniqueResourceName);
		this.uniqueResourceName = uniqueResourceName;
	}

	@Override
	protected XAResource refreshXAConnection() throws ResourceException {
		return new DubboXAResourceImpl(uniqueResourceName);
	}

}
