package com.sxb.lin.atomikos.dubbo.spring;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public abstract class InitiatorXADataSourceUtils {

	private static final Log logger = LogFactory.getLog(InitiatorXADataSourceUtils.class);

	public static InitiatorXAConnectionHolder getXAConnection(XADataSource xaDataSource) 
			throws SQLException{
		return doGetXAConnection(xaDataSource);
	}
	
	public static InitiatorXAConnectionHolder doGetXAConnection(XADataSource xaDataSource) 
			throws SQLException{
		XAConnection xaConnection = xaDataSource.getXAConnection();
		InitiatorXAConnectionHolder initiatorXAConnectionHolder = new InitiatorXAConnectionHolder(xaConnection);
		initiatorXAConnectionHolder.start();
		TransactionSynchronizationManager.bindResource(xaDataSource, initiatorXAConnectionHolder);
		return initiatorXAConnectionHolder;
	}
	
	public static void registerSynchronization(XADataSource xaDataSource){
		Object resource = TransactionSynchronizationManager.getResource(xaDataSource);
		if(resource instanceof InitiatorXAConnectionHolder){
			InitiatorXAConnectionHolder initiatorXAConnectionHolder = (InitiatorXAConnectionHolder) resource;
			if(!initiatorXAConnectionHolder.isSynchronizedWithTransaction()){
				initiatorXAConnectionHolder.setSynchronizedWithTransaction(true);
				TransactionSynchronizationManager.registerSynchronization(
						new InitiatorXAConnectionSynchronization(initiatorXAConnectionHolder,xaDataSource));
			}
		}
	}
	
	public static void releaseConnection(XAConnection xaConnection,XADataSource xaDataSource){
		try {
			doReleaseConnection(xaConnection, xaDataSource);
		}
		catch (SQLException ex) {
			logger.debug("Could not close JDBC Connection", ex);
		}
		catch (Throwable ex) {
			logger.debug("Unexpected exception on closing JDBC Connection", ex);
		}
	}
	
	public static void doReleaseConnection(XAConnection xaConnection,XADataSource xaDataSource) throws SQLException{
		if(xaConnection == null){
			return;
		}
		if(xaDataSource != null){
			InitiatorXAConnectionHolder conHolder = (InitiatorXAConnectionHolder) TransactionSynchronizationManager.getResource(xaDataSource);
			if (conHolder != null && connectionEquals(conHolder, xaConnection)) {
				conHolder.released();
				return;
			}
		}
	}
	
	public static void endConnection(InitiatorXAConnectionHolder connectionHolder) throws XAException{
		if(connectionHolder != null){
			connectionHolder.end();
		}
	}
	
	public static void closeConnection(InitiatorXAConnectionHolder connectionHolder) throws SQLException{
		Connection connection = connectionHolder.getConnection();
		connection.close();
		XAConnection xaConnection = connectionHolder.getXaConnection();
		xaConnection.close();
	}
	
	private static boolean connectionEquals(InitiatorXAConnectionHolder conHolder,XAConnection xaConnection){
		if (!conHolder.hasConnection()) {
			return false;
		}
		XAConnection xaConn = conHolder.getXaConnection();
		return (xaConn == xaConnection) || xaConn.equals(xaConnection);
	}
	
	private static class InitiatorXAConnectionSynchronization extends TransactionSynchronizationAdapter {
		
		private final InitiatorXAConnectionHolder connectionHolder;

		private final XADataSource xaDataSource;

		private boolean holderActive = true;

		public InitiatorXAConnectionSynchronization(
				InitiatorXAConnectionHolder connectionHolder,
				XADataSource xaDataSource) {
			super();
			this.connectionHolder = connectionHolder;
			this.xaDataSource = xaDataSource;
		}
		
		@Override
		public int getOrder() {
			return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER;
		}

		@Override
		public void beforeCompletion() {
			if (!this.connectionHolder.isOpen()) {
				TransactionSynchronizationManager.unbindResource(this.xaDataSource);
				this.holderActive = false;
			}
				
			if (this.connectionHolder.hasConnection()) {
				releaseConnection(this.connectionHolder.getXaConnection(), this.xaDataSource);
				try {
					endConnection(this.connectionHolder);
				} catch (XAException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}

		@Override
		public void afterCompletion(int status) {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResourceIfPossible(this.xaDataSource);
				this.holderActive = false;
			}
			
			if (this.connectionHolder.hasConnection()) {
				try {
					closeConnection(connectionHolder);
				} catch (SQLException e) {
					logger.error(e.getMessage(), e);
				}
			}
			
			this.connectionHolder.reset();
		}
	}
}
