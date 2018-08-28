package com.sxb.lin.atomikos.dubbo.pool;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

public class JdbcXAResourceHolder extends XAResourceHolder{
	
	private XAConnection xaConnection;
	
	private Connection connection;
	
	public JdbcXAResourceHolder(String dubboUniqueResourceName, String uuid,
			XAConnection xaConnection, Connection connection, XAResource xaResource) {
		super(dubboUniqueResourceName, uuid, xaResource);
		this.xaConnection = xaConnection;
		this.connection = connection;
	}
	
	public XAConnection getXaConnection() {
		return xaConnection;
	}

	public Connection getConnection() {
		return connection;
	}
	
	@Override
	protected void disconnect() {
		try {
			if(connection != null){
				Connection unwrap = connection.unwrap(Connection.class);
				unwrap.close();
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(),e);
		}
		this.doClose();
	}
	
	@Override
	protected void doClose() {
		try {
			if(connection != null){
				connection.close();
			}
			if(xaConnection != null){
				xaConnection.close();
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(),e);
		} finally {
			this.clear();
		}
	}

	@Override
	protected void clear() {
		super.clear();
		connection = null;
		xaConnection = null;
	}
	
}
