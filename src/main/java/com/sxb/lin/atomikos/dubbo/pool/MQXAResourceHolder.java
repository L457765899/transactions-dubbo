package com.sxb.lin.atomikos.dubbo.pool;

import javax.transaction.xa.XAResource;

public class MQXAResourceHolder extends XAResourceHolder{

	public MQXAResourceHolder(String dubboUniqueResourceName, String uuid, XAResource xaResource) {
		super(dubboUniqueResourceName, uuid, xaResource);
	}

	@Override
	protected void disconnect() {
		this.doClose();
	}

	@Override
	protected void doClose() {
		this.clear();
	}
	
}
