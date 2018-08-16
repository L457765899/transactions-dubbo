package com.sxb.lin.atomikos.dubbo.spring;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import org.springframework.transaction.support.ResourceHolderSupport;

import com.sxb.lin.atomikos.dubbo.pool.XAResourceHolder;

public class XAConnectionHolder extends ResourceHolderSupport{
	
	private String dubboUniqueResourceName;
	
	private XAConnection xaConnection;
	
	private Connection connection;
	
	private XAResource xaResource;
	
	private XAResourceHolder xaResourceHolder;
	
	public XAConnectionHolder(XAConnection xaConnection,
			String dubboUniqueResourceName) throws SQLException {
		super();
		this.dubboUniqueResourceName = dubboUniqueResourceName;
		this.set(xaConnection);
	}
	
	private void set(XAConnection xaConnection) throws SQLException {
		this.xaConnection = xaConnection;
		if(xaConnection != null){
			this.connection = xaConnection.getConnection();
			this.xaResource = xaConnection.getXAResource();
			this.xaResourceHolder = new XAResourceHolder(dubboUniqueResourceName, xaConnection, connection, xaResource);
			try {
				this.xaResourceHolder.start();
			} catch (XAException e) {
				throw new SQLException(e);
			} catch (SystemException e) {
				throw new SQLException(e);
			} catch (RollbackException e) {
				throw new SQLException(e);
			}
		}
	}

	public boolean hasXAConnection() {
		return (this.xaConnection != null);
	}

	public XAConnection getXaConnection() {
		return xaConnection;
	}
	
	public Connection getConnection() {
		return connection;
	}

	public XAResource getXaResource() {
		return xaResource;
	}

	public void setXaConnection(XAConnection xaConnection,
			String dubboUniqueResourceName) throws SQLException {
		this.dubboUniqueResourceName = dubboUniqueResourceName;
		this.set(xaConnection);
	}
	
	public void close() throws XAException{
		this.xaResourceHolder.end();
	}

	@Override
	public void reset() {
		super.reset();
		this.dubboUniqueResourceName = null;
		this.xaConnection = null;
		this.connection = null;
		this.xaResource = null;
		this.xaResourceHolder = null;
	}

	
}
