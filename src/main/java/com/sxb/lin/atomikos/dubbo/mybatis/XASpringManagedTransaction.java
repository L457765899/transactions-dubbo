package com.sxb.lin.atomikos.dubbo.mybatis;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.mybatis.spring.transaction.SpringManagedTransaction;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.sxb.lin.atomikos.dubbo.AtomikosDubboException;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.spring.jdbc.XAConnectionHolder;
import com.sxb.lin.atomikos.dubbo.spring.jdbc.XADataSourceUtils;

public class XASpringManagedTransaction extends SpringManagedTransaction{
	
	private XADataSource xaDataSource;
	
	private XAConnectionHolder xaConnectionHolder;
	
	private XAConnection xaConnection;
	
	private Connection connection;
	
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
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			return super.getConnection();
		}else{
			if(connection == null){
				this.openConnection();
			}
			return connection;
		}
	}
	
	private void openConnection() throws SQLException {
		this.xaConnectionHolder = XADataSourceUtils.getXAConnection(xaDataSource,dubboUniqueResourceName);
		this.xaConnection = this.xaConnectionHolder.getXaConnection();
		this.connection = this.xaConnectionHolder.getConnection();
	}

	@Override
	public void commit() throws SQLException {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			super.commit();
		}
	}

	@Override
	public void rollback() throws SQLException {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			super.rollback();
		}
	}

	@Override
	public void close() throws SQLException {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			super.close();
		}else{
			XADataSourceUtils.releaseConnection(this.xaConnection, this.xaDataSource);
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
