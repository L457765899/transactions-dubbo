package com.sxb.lin.atomikos.dubbo.mybatis;

import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

public abstract class XADataSourceUtils {

	public static XAConnection getXAConnection(XADataSource xaDataSource) throws SQLException{
		return doGetXAConnection(xaDataSource);
	}
	
	public static XAConnection doGetXAConnection(XADataSource xaDataSource) throws SQLException{
		Assert.notNull(xaDataSource, "No XADataSource specified");
		
		XAConnectionHolder conHolder = (XAConnectionHolder) TransactionSynchronizationManager.getResource(xaDataSource);
		if (conHolder != null && (conHolder.hasXAConnection() || conHolder.isSynchronizedWithTransaction())) {
			conHolder.requested();
			if (!conHolder.hasXAConnection()) {
				conHolder.setXaConnection(xaDataSource.getXAConnection());
			}
			return conHolder.getXaConnection();
		}
		
		XAConnection xaConnection = xaDataSource.getXAConnection();
		
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
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
		}

		return xaConnection;
		
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
		public void suspend() {
			// TODO Auto-generated method stub
			super.suspend();
		}

		@Override
		public void resume() {
			// TODO Auto-generated method stub
			super.resume();
		}

		@Override
		public void beforeCompletion() {
			// TODO Auto-generated method stub
			super.beforeCompletion();
		}

		@Override
		public void afterCompletion(int status) {
			// TODO Auto-generated method stub
			super.afterCompletion(status);
		}
		
	}
}
