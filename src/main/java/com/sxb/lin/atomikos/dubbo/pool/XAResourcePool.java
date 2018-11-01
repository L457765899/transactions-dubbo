package com.sxb.lin.atomikos.dubbo.pool;

import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.lin.atomikos.dubbo.pool.recover.RecoverXAResource;
import com.sxb.lin.atomikos.dubbo.pool.recover.UniqueResource;
import com.sxb.lin.atomikos.dubbo.rocketmq.MQProducerFor2PC;
import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class XAResourcePool implements Runnable{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(XAResourcePool.class);

	private Map<String,UniqueResource> uniqueResourceMapping;
	
	private Map<Xid,XAResourceHolder> cachePool = new ConcurrentHashMap<Xid, XAResourceHolder>();
	
	private ScheduledExecutorService scheduledExecutorService;
	
	
	public XAResourcePool(Map<String, UniqueResource> uniqueResourceMapping) {
		super();
		this.uniqueResourceMapping = uniqueResourceMapping;
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
	
	protected RecoverXAResource getRecoverXAResource(String uniqueResourceName) throws SQLException{
		if(uniqueResourceMapping == null){
			return null;
		}
		UniqueResource uniqueResource = uniqueResourceMapping.get(uniqueResourceName);
		if(uniqueResource == null){
			return null;
		}
		
		return uniqueResource.getRecoverXAResource();
	}
	
	protected void closeRecoverXAResource(RecoverXAResource recoverXAResource){
		if(recoverXAResource != null){
			recoverXAResource.close();
		}
	}
	
	public Xid[] recover(int flag, String uniqueResourceName) throws XAException {
		
		RecoverXAResource recoverXAResource = null;
		try {
			recoverXAResource = this.getRecoverXAResource(uniqueResourceName);
			if(recoverXAResource != null){
				XAResource xaResource = recoverXAResource.getXAResource();
				return xaResource.recover(flag);
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			this.closeRecoverXAResource(recoverXAResource);
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
			RecoverXAResource recoverXAResource = null;
			try {
				recoverXAResource = this.getRecoverXAResource(uniqueResourceName);
				if(recoverXAResource != null){
					XAResource xaResource = recoverXAResource.getXAResource();
					xaResource.commit(xid, onePhase);
				}else{
					if(uniqueResourceName.startsWith(MQProducerFor2PC.MQ_UNIQUE_TOPIC_NO_PREPARE_PREFIX)){
						return;
					}
					throw new XAException("XAResourceHolder or XAConnection is not exist.");
				}
			} catch(SQLFeatureNotSupportedException e){
				throw new XAException(e.getMessage());
			} catch (SQLException e) {
				LOGGER.error(e.getMessage(), e);
			} finally {
				this.closeRecoverXAResource(recoverXAResource);
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
			RecoverXAResource recoverXAResource = null;
			try {
				recoverXAResource = this.getRecoverXAResource(uniqueResourceName);
				if(recoverXAResource != null){
					XAResource xaResource = recoverXAResource.getXAResource();
					xaResource.rollback(xid);
				}else{
					if(uniqueResourceName.startsWith(MQProducerFor2PC.MQ_UNIQUE_TOPIC_NO_PREPARE_PREFIX)){
						return;
					}
					throw new XAException("XAResourceHolder or XAConnection is not exist.");
				}
			} catch(SQLFeatureNotSupportedException e){
				throw new XAException(e.getMessage());
			} catch (SQLException e) {
				LOGGER.error(e.getMessage(), e);
			} finally {
				this.closeRecoverXAResource(recoverXAResource);
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
