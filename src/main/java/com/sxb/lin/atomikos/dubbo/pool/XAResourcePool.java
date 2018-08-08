package com.sxb.lin.atomikos.dubbo.pool;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
	
	private Map<Xid,XAResourceHolder> cachePool = new ConcurrentHashMap<Xid, XAResourceHolder>();
	
	private ScheduledExecutorService scheduledExecutorService;
	
	
	public XAResourcePool(Map<String, DataSource> dataSourceMapping) {
		super();
		this.dataSourceMapping = dataSourceMapping;
		this.scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
		scheduledExecutorService.scheduleAtFixedRate(this, 30, 30, TimeUnit.SECONDS);
	}
	
	public void addXAResourceHolder(XAResourceHolder xaResourceHolder){
		cachePool.put(xaResourceHolder.getStartXid().getXid(), xaResourceHolder);
	}
	
	public void removeXAResourceHolder(XAResourceHolder xaResourceHolder){
		this.cachePool.remove(xaResourceHolder.getStartXid().getXid());
	}
	
	public List<XAResourceHolder> getDisconnectedHolderByTmAddress(String tmAddress){
		Set<Entry<Xid, XAResourceHolder>> set = cachePool.entrySet();
		List<XAResourceHolder> list = new ArrayList<XAResourceHolder>();
		for(Entry<Xid, XAResourceHolder> entry : set){
			XAResourceHolder xaResourceHolder = entry.getValue();
			if(xaResourceHolder.getTmAddress().equals(tmAddress)){
				list.add(xaResourceHolder);
			}
		}
		return list;
	}
	
	protected XAConnection getXAConnection(String uniqueResourceName) throws SQLException{
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
		
		return xaDataSource.getXAConnection();
	}
	
	protected void closeXAConnection(XAConnection xaConnection){
		if(xaConnection != null){
			try {
				xaConnection.close();
			} catch (SQLException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}
	
	public Xid[] recover(int flag, String uniqueResourceName) throws XAException {
		
		XAConnection xaConnection = null;
		try {
			xaConnection = this.getXAConnection(uniqueResourceName);
			if(xaConnection != null){
				XAResource xaResource = xaConnection.getXAResource();
				return xaResource.recover(flag);
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			this.closeXAConnection(xaConnection);
		}
		
		return null;
	}
	
	public int prepare(Xid xid) throws XAException {
		XAResourceHolder xaResourceHolder = this.cachePool.get(xid);
		if(xaResourceHolder != null){
			return xaResourceHolder.prepare(xid);
		}else{
			throw new XAException("XAResourceHolder is not exist.");
		}
	}
	
	public void commit(Xid xid, boolean onePhase, String uniqueResourceName) throws XAException {
		XAResourceHolder xaResourceHolder = this.cachePool.get(xid);
		if(xaResourceHolder != null){
			xaResourceHolder.commit(xid, onePhase);
			this.removeXAResourceHolder(xaResourceHolder);
			xaResourceHolder.close();
		}else{
			XAConnection xaConnection = null;
			try {
				xaConnection = this.getXAConnection(uniqueResourceName);
				if(xaConnection != null){
					XAResource xaResource = xaConnection.getXAResource();
					xaResource.commit(xid, onePhase);
				}else{
					throw new XAException("XAResourceHolder or XAConnection is not exist.");
				}
			} catch (SQLException e) {
				LOGGER.error(e.getMessage(), e);
			} finally {
				this.closeXAConnection(xaConnection);
			}
		}
	}
	
	public void rollback(Xid xid,String uniqueResourceName) throws XAException {
		XAResourceHolder xaResourceHolder = this.cachePool.get(xid);
		if(xaResourceHolder != null){
			xaResourceHolder.rollback(xid);
			this.removeXAResourceHolder(xaResourceHolder);
			xaResourceHolder.close();
		}else{
			XAConnection xaConnection = null;
			try {
				xaConnection = this.getXAConnection(uniqueResourceName);
				if(xaConnection != null){
					XAResource xaResource = xaConnection.getXAResource();
					xaResource.rollback(xid);
				}else{
					throw new XAException("XAResourceHolder or XAConnection is not exist.");
				}
			} catch (SQLException e) {
				LOGGER.error(e.getMessage(), e);
			} finally {
				this.closeXAConnection(xaConnection);
			}
		}
	}

	public void run() {
		Set<Entry<Xid, XAResourceHolder>> set = cachePool.entrySet();
		LOGGER.info(set.size()+" size XAResourceHolder has been check is time out.");
		for(Entry<Xid, XAResourceHolder> entry : set){
			XAResourceHolder xaResourceHolder = entry.getValue();
			StartXid startXid = xaResourceHolder.getStartXid();
			long startTime = startXid.getStartTime();
			long timeout = startXid.getTimeout();
			long expireTime = startTime + timeout;
			if(expireTime < System.currentTimeMillis()){
				LOGGER.error("UUID " + xaResourceHolder.getUuid() + " is expired,it will be close.");
				this.removeXAResourceHolder(xaResourceHolder);
				xaResourceHolder.close();
			}else{
				long remaining = expireTime - System.currentTimeMillis();
				LOGGER.info("UUID " + xaResourceHolder.getUuid() + " is not expired,"+remaining+" milliseconds remaining.");
			}
		}
	}
}
