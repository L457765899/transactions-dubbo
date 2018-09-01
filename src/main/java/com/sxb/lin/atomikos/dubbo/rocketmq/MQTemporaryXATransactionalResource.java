package com.sxb.lin.atomikos.dubbo.rocketmq;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.xa.XATransactionalResource;

public class MQTemporaryXATransactionalResource extends XATransactionalResource{
	
	private XAResource xaResource;

	public MQTemporaryXATransactionalResource(String uniqueResourceName,XAResource xaResource) {
		super(uniqueResourceName);
		this.xaResource = xaResource;
	}

	@Override
	protected XAResource refreshXAConnection() throws ResourceException {
		return xaResource;
	}

	@Override
	public void recover() {
		
	}

}
