package com.sxb.lin.atomikos.dubbo.mybatis;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class XAHASpringManagedTransaction extends XASpringManagedTransaction{
	
	private static final Log LOGGER = LogFactory.getLog(XAHASpringManagedTransaction.class);
	
	private Connection connection;

	public XAHASpringManagedTransaction(DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public Connection getConnection() throws SQLException {
		if(connection == null){
			this.connection = super.getConnection();
		}
		return connection;
	}

	@Override
	public void close() throws SQLException {
		try {
			if(connection != null){
				if(!TransactionSynchronizationManager.isSynchronizationActive()){
					if(connection.isReadOnly()){
						connection.setReadOnly(false);
					}
				}else if(!TransactionSynchronizationManager.isActualTransactionActive()){
					if(connection.isReadOnly()){
						connection.setReadOnly(false);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("ha reset read only error:" + e.getMessage(), e);
		}
			
		super.close();
	}
}
