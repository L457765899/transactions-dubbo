package com.sxb.lin.atomikos.dubbo.pool.recover;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JdbcRecoverXAResource implements RecoverXAResource{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JdbcRecoverXAResource.class);
	
	private XAConnection xaConnection;
	
	private XAResource xaResource;
	
	public JdbcRecoverXAResource(XAConnection xaConnection) throws SQLException {
		this.xaConnection = xaConnection;
		this.xaResource = xaConnection.getXAResource();
	}

	public void close() {
		try {
			this.xaConnection.close();
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			this.xaConnection = null;
			this.xaResource = null;
		}
	}

	public XAResource getXAResource() {
		return this.xaResource;
	}

	public XAConnection getXaConnection() {
		return this.xaConnection;
	}
}
