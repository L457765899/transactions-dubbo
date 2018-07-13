package com.sxb.lin.atomikos.dubbo.mybatis;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAResource;

import org.mybatis.spring.transaction.SpringManagedTransaction;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.sxb.lin.atomikos.dubbo.AtomikosDubboException;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;

public class XASpringManagedTransaction extends SpringManagedTransaction{
	
	private XADataSource xaDataSource;
	
	private XAConnection xaConnection;
	
	private Connection connection;
	
	private XAResource xaResource;
	
	private String dubboUniqueResourceName;

	public XASpringManagedTransaction(DataSource dataSource) {
		super(dataSource);
		this.setXADataSource(dataSource);
	}

	protected void setXADataSource(DataSource dataSource){
		if(dataSource instanceof XADataSource){
			xaDataSource = (XADataSource) dataSource;
		}else if(dataSource instanceof AtomikosDataSourceBean){
			xaDataSource = ((AtomikosDataSourceBean) dataSource).getXaDataSource();
		}else{
			throw new AtomikosDubboException("support dubbo xa transaction,must use xaDataSource.");
		}
	}

	@Override
	public Connection getConnection() throws SQLException {
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			return super.getConnection();
		}else{
			if(connection == null){
				this.openConnection();
			}
			return connection;
		}
	}
	
	private void openConnection() throws SQLException {
		this.xaConnection = XADataSourceUtils.getXAConnection(xaDataSource);
		this.connection = this.xaConnection.getConnection();
		this.xaResource = this.xaConnection.getXAResource();
	}

	@Override
	public void commit() throws SQLException {
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			super.commit();
		}
	}

	@Override
	public void rollback() throws SQLException {
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			super.rollback();
		}
	}

	@Override
	public void close() throws SQLException {
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			super.close();
		}else{
			
		}
	}

	public XADataSource getXaDataSource() {
		return xaDataSource;
	}

	public void setXaDataSource(XADataSource xaDataSource) {
		this.xaDataSource = xaDataSource;
	}

	public String getDubboUniqueResourceName() {
		return dubboUniqueResourceName;
	}

	public void setDubboUniqueResourceName(String dubboUniqueResourceName) {
		this.dubboUniqueResourceName = dubboUniqueResourceName;
	}
	
}
