package com.sxb.lin.atomikos.dubbo.mybatis;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.transaction.support.TransactionSynchronizationManager;

public class XAHASpringManagedTransaction extends XASpringManagedTransaction{
	
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
		super.close();
	}
}
