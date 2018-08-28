package com.sxb.lin.atomikos.dubbo.pool.recover;

import java.sql.SQLException;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomikos.jdbc.AtomikosDataSourceBean;

public class DataSourceResource implements UniqueResource{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceResource.class);
	
	private DataSource dataSource;
	
	private XADataSource xaDataSource;
	
	private String uniqueResourceName;
	
	public DataSourceResource(String uniqueResourceName, DataSource dataSource) {
		this.dataSource = dataSource;
		this.uniqueResourceName = uniqueResourceName;
		if(dataSource instanceof XADataSource){
			this.xaDataSource = (XADataSource) dataSource;
		}else if(dataSource instanceof AtomikosDataSourceBean){
			this.xaDataSource = ((AtomikosDataSourceBean) dataSource).getXaDataSource();
		}
	}

	public RecoverXAResource getRecoverXAResource() {
		if(xaDataSource == null){
			return null;
		}
		
		try {
			XAConnection xaConnection = xaDataSource.getXAConnection();
			return new JdbcRecoverXAResource(xaConnection);
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return null;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public XADataSource getXaDataSource() {
		return xaDataSource;
	}

	public String getUniqueResourceName() {
		return uniqueResourceName;
	}
}
