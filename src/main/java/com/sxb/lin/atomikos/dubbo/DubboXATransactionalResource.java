package com.sxb.lin.atomikos.dubbo;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;

public class DubboXATransactionalResource extends XATransactionalResource{

	public DubboXATransactionalResource(String servername) {
		super(servername);
	}

	@Override
	protected synchronized XAResource refreshXAConnection() throws ResourceException {
		return null;
	}

}
