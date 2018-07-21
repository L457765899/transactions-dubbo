package com.sxb.lin.atomikos.dubbo.pool;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomikos.jdbc.AtomikosDataSourceBean;
import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class XAResourcePool implements Runnable{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(XAResourcePool.class);

	private Map<String,DataSource> dataSourceMapping;
	
	private Map<Xid,XAResourceHolder> pool = new ConcurrentHashMap<Xid, XAResourceHolder>();
	
	private List<XAResourceHolder> xaList = new Vector<XAResourceHolder>();
	
	private ScheduledExecutorService scheduledExecutorService;
	
	
	public XAResourcePool(Map<String, DataSource> dataSourceMapping) {
		super();
		this.dataSourceMapping = dataSourceMapping;
		this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutorService.scheduleAtFixedRate(this, 30, 60, TimeUnit.SECONDS);
	}
	
	public void addXAResourceHolder(XAResourceHolder xaResourceHolder){
		pool.put(xaResourceHolder.getStartXid().getXid(), xaResourceHolder);
		xaList.add(xaResourceHolder);
	}
	
	public void removeXAResourceHolder(XAResourceHolder xaResourceHolder){
		this.pool.remove(xaResourceHolder.getStartXid().getXid());
		this.xaList.remove(xaResourceHolder);
	}
	
	public Xid[] recover(int flag, String uniqueResourceName) throws XAException {
		
		if(dataSourceMapping == null){
			return null;
		}
		DataSource dataSource = dataSourceMapping.get(uniqueResourceName);
		XADataSource xaDataSource = null;
		if(dataSource instanceof XADataSource){
			xaDataSource = (XADataSource) dataSource;
		}else if(dataSource instanceof AtomikosDataSourceBean){
			xaDataSource = ((AtomikosDataSourceBean) dataSource).getXaDataSource();
		}
		if(xaDataSource == null){
			return null;
		}
		
		XAConnection xaConnection = null;
		try {
			xaConnection = xaDataSource.getXAConnection();
			XAResource xaResource = xaConnection.getXAResource();
			return xaResource.recover(flag);
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			if(xaConnection != null){
				try {
					xaConnection.close();
				} catch (SQLException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
		
		return null;
	}
	
	public int prepare(Xid xid) throws XAException {
		XAResourceHolder xaResourceHolder = this.pool.get(xid);
		return xaResourceHolder.prepare(xid);
	}
	
	public void commit(Xid xid, boolean onePhase) throws XAException {
		XAResourceHolder xaResourceHolder = this.pool.get(xid);
		xaResourceHolder.commit(xid, onePhase);
		this.removeXAResourceHolder(xaResourceHolder);
		xaResourceHolder.close();
	}
	
	public void rollback(Xid xid) throws XAException {
		XAResourceHolder xaResourceHolder = this.pool.get(xid);
		xaResourceHolder.rollback(xid);
		this.removeXAResourceHolder(xaResourceHolder);
		xaResourceHolder.close();
	}

	public void run() {
		for(XAResourceHolder xaResourceHolder : xaList){
			StartXid startXid = xaResourceHolder.getStartXid();
			long startTime = startXid.getStartTime();
			long timeout = startXid.getTimeout();
			long expireTime = startTime + timeout;
			if(expireTime < System.currentTimeMillis()){
				this.removeXAResourceHolder(xaResourceHolder);
				xaResourceHolder.close();
			}
		}
	}
}
