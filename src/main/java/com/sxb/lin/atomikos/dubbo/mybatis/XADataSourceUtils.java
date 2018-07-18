package com.sxb.lin.atomikos.dubbo.mybatis;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public abstract class XADataSourceUtils {
	
	private static final Log logger = LogFactory.getLog(XADataSourceUtils.class);

	public static XAConnectionHolder getXAConnection(XADataSource xaDataSource) throws SQLException{
		return doGetXAConnection(xaDataSource);
	}
	
	public static XAConnectionHolder doGetXAConnection(XADataSource xaDataSource) throws SQLException{
		Assert.notNull(xaDataSource, "No XADataSource specified");
		
		XAConnectionHolder conHolder = (XAConnectionHolder) TransactionSynchronizationManager.getResource(xaDataSource);
		if (conHolder != null && conHolder.hasXAConnection()) {
			conHolder.requested();
			if (!conHolder.hasXAConnection()) {
				conHolder.setXaConnection(xaDataSource.getXAConnection());
			}
			return conHolder;
		}
		
		XAConnection xaConnection = xaDataSource.getXAConnection();
		XAConnectionHolder holderToUse = conHolder;
		if (holderToUse == null) {
			holderToUse = new XAConnectionHolder(xaConnection);
		}else {
			holderToUse.setXaConnection(xaConnection);
		}
		holderToUse.requested();
		TransactionSynchronizationManager.registerSynchronization(
				new XAConnectionSynchronization(holderToUse, xaDataSource));
		holderToUse.setSynchronizedWithTransaction(true);
		if (holderToUse != conHolder) {
			TransactionSynchronizationManager.bindResource(xaDataSource, holderToUse);
		}
		
		return holderToUse;
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
			XAConnectionHolder conHolder = (XAConnectionHolder) TransactionSynchronizationManager.getResource(xaDataSource);
			if (conHolder != null && connectionEquals(conHolder, xaConnection)) {
				conHolder.released();
				return;
			}
		}
		doCloseConnection(xaConnection);
	}
	
	public static void doCloseConnection(XAConnection xaConnection) throws SQLException{
		xaConnection.close();
	}
	
	private static boolean connectionEquals(XAConnectionHolder conHolder,XAConnection xaConnection){
		if (!conHolder.hasXAConnection()) {
			return false;
		}
		XAConnection xaConn = conHolder.getXaConnection();
		return (xaConn == xaConnection) || xaConn.equals(xaConnection);
	}
	
	private static class XAConnectionSynchronization extends TransactionSynchronizationAdapter {
		
		private final XAConnectionHolder connectionHolder;

		private final XADataSource xaDataSource;

		private boolean holderActive = true;

		public XAConnectionSynchronization(XAConnectionHolder connectionHolder,
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
				if (this.connectionHolder.hasXAConnection()) {
					releaseConnection(this.connectionHolder.getXaConnection(), this.xaDataSource);
				}
			}
		}

		@Override
		public void afterCompletion(int status) {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResourceIfPossible(this.xaDataSource);
				this.holderActive = false;
				if (this.connectionHolder.hasXAConnection()) {
					releaseConnection(this.connectionHolder.getXaConnection(), this.xaDataSource);
					this.connectionHolder.setXaConnection(null);
				}
			}
			this.connectionHolder.reset();
		}
		
	}
}
