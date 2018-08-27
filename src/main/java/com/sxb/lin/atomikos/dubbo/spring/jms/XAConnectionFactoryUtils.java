package com.sxb.lin.atomikos.dubbo.spring.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;
import javax.jms.XASession;

import org.apache.activemq.pool.PooledConnection;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.springframework.transaction.support.ResourceHolderSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public abstract class XAConnectionFactoryUtils {

	public static Session doGetTransactionalSession(ConnectionFactory connectionFactory, 
			boolean startConnection, JmsTemplate jmsTemplate) throws JMSException {
		
		Assert.notNull(connectionFactory, "ConnectionFactory must not be null");
		
		XAJmsResourceHolder resourceHolder =
				(XAJmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
		
		if (resourceHolder != null) {
			Session session = resourceHolder.getSession();
			if (session != null) {
				if (startConnection) {
					Connection con = resourceHolder.getConnection();
					con.start();
				}
				return session;
			}
		}
		
		XASession session = null;
		Connection connection = null;
		try {
			if(connectionFactory instanceof PooledConnectionFactory){
				PooledConnection pooledConnection = (PooledConnection) connectionFactory.createConnection();
				connection = pooledConnection;
				XAConnection con = (XAConnection) pooledConnection.getConnection();
				session = con.createXASession();
				resourceHolder = new XAJmsResourceHolder(connectionFactory, con, session);
				
				if (startConnection) {
					con.start();
				}
			}else if (connectionFactory instanceof XAConnectionFactory){
				XAConnection con = ((XAConnectionFactory)connectionFactory).createXAConnection();
				connection = con;
				session = con.createXASession();
				resourceHolder = new XAJmsResourceHolder(connectionFactory, con, session);
				
				if (startConnection) {
					con.start();
				}
			}else {
				throw new JMSException(
						"ConnectionFactory must not be PooledConnectionFactory set XAConnection or XAConnectionFactory.");
			}
		}
		catch (JMSException ex) {
			if (session != null) {
				try {
					session.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			if (connection != null) {
				try {
					connection.close();
				}
				catch (Throwable ex2) {
					// ignore
				}
			}
			throw ex;
		}
		
		TransactionSynchronizationManager.registerSynchronization(
				new XAJmsResourceSynchronization(resourceHolder, connectionFactory));
		resourceHolder.setSynchronizedWithTransaction(true);
		TransactionSynchronizationManager.bindResource(connectionFactory, resourceHolder);
		
		return session;
	}
	
	private static class XAJmsResourceSynchronization extends ResourceHolderSynchronization<XAJmsResourceHolder, Object> {

		public XAJmsResourceSynchronization(XAJmsResourceHolder resourceHolder, Object resourceKey) {
			super(resourceHolder, resourceKey);
		}

		@Override
		protected void releaseResource(XAJmsResourceHolder resourceHolder, Object resourceKey) {
			resourceHolder.closeAll();
		}
	}
}
