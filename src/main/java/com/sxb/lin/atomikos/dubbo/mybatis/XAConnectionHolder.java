package com.sxb.lin.atomikos.dubbo.mybatis;

import javax.sql.XAConnection;

import org.springframework.transaction.support.ResourceHolderSupport;

public class XAConnectionHolder extends ResourceHolderSupport{
	
	private XAConnection xaConnection;
	
	public XAConnectionHolder(XAConnection xaConnection) {
		super();
		this.xaConnection = xaConnection;
	}

	public boolean hasXAConnection() {
		return (this.xaConnection != null);
	}

	public XAConnection getXaConnection() {
		return xaConnection;
	}

	public void setXaConnection(XAConnection xaConnection) {
		this.xaConnection = xaConnection;
	}

}
