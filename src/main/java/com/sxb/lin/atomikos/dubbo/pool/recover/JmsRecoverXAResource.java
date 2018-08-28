package com.sxb.lin.atomikos.dubbo.pool.recover;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XASession;
import javax.transaction.xa.XAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsRecoverXAResource implements RecoverXAResource{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JmsRecoverXAResource.class);
	
	private XAConnection xaConnection;
	
	private Connection closeConnection;
	
	private XASession xaSession;
	
	private XAResource xaResource;
	
	public JmsRecoverXAResource(XAConnection xaConnection,Connection closeConnection) throws JMSException {
		this.xaConnection = xaConnection;
		this.closeConnection = closeConnection;
		this.xaSession = xaConnection.createXASession();
		this.xaResource = this.xaSession.getXAResource();
	}

	public void close() {
		try {
			this.xaSession.close();
			this.closeConnection.close();
		} catch (JMSException e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			this.xaConnection = null;
			this.closeConnection = null;
			this.xaSession = null;
			this.xaResource = null;
		}
	}

	public XAResource getXAResource() {
		return this.xaResource;
	}

	public XAConnection getXaConnection() {
		return xaConnection;
	}

	public XASession getXaSession() {
		return xaSession;
	}

	public XAResource getXaResource() {
		return xaResource;
	}
}
