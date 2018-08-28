package com.sxb.lin.atomikos.dubbo.pool;

import javax.transaction.xa.XAResource;

import com.sxb.lin.atomikos.dubbo.spring.jms.XAJmsResourceHolder;

public class JmsXAResourceHolder extends XAResourceHolder{
	
	private XAJmsResourceHolder xaJmsResourceHolder;
	
	public JmsXAResourceHolder(String dubboUniqueResourceName, 
			String uuid, XAResource xaResource, XAJmsResourceHolder xaJmsResourceHolder) {
		super(dubboUniqueResourceName, uuid, xaResource);
		this.xaJmsResourceHolder = xaJmsResourceHolder;
	}

	@Override
	protected void disconnect() {
		this.doClose();
	}

	@Override
	protected void doClose() {
		try {
			xaJmsResourceHolder.closeAll();
		} finally {
			this.clear();
		}
	}

	@Override
	protected void clear() {
		super.clear();
		this.xaJmsResourceHolder = null;
	}
	
	
}
