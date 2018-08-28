package com.sxb.lin.atomikos.dubbo.pool.recover;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XAConnection;
import javax.jms.XAConnectionFactory;

import org.apache.activemq.pool.PooledConnection;
import org.apache.activemq.pool.PooledConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectionFactoryResource implements UniqueResource{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionFactoryResource.class);
	
	private String uniqueResourceName;
	
	private ConnectionFactory connectionFactory;
	
	public ConnectionFactoryResource(String uniqueResourceName, ConnectionFactory connectionFactory) {
		this.uniqueResourceName = uniqueResourceName;
		this.connectionFactory = connectionFactory;
	}

	public RecoverXAResource getRecoverXAResource() {
		if(connectionFactory == null){
			return null;
		}
		
		if(connectionFactory instanceof PooledConnectionFactory){
			PooledConnectionFactory pooledConnectionFactory = (PooledConnectionFactory) connectionFactory;
			try {
				PooledConnection pooledConnection = (PooledConnection) pooledConnectionFactory.createConnection();
				XAConnection connection = (XAConnection) pooledConnection.getConnection();
				return new JmsRecoverXAResource(connection,pooledConnection);
			} catch (JMSException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}else if (connectionFactory instanceof XAConnectionFactory){
			XAConnectionFactory xaConnectionFactory = (XAConnectionFactory) connectionFactory;
			try {
				XAConnection connection = xaConnectionFactory.createXAConnection();
				return new JmsRecoverXAResource(connection,connection);
			} catch (JMSException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
		return null;
	}

	public String getUniqueResourceName() {
		return uniqueResourceName;
	}

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}
	
}
