package com.sxb.lin.atomikos.dubbo.pool;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.XAConnection;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;
import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class XAResourceHolder {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(XAResourceHolder.class);

	private final static int XA_UNKNOWN = 0;
	
	private final static int XA_START = 1;
	
	private final static int XA_END = 2;
	
	private final static int XA_PREPARE = 3;
	
	private final static int XA_COMMIT = 4;
	
	private final static int XA_ROLLBACK = 5;
	
	private final static int CLOSE = 6;
	
	private String dubboUniqueResourceName;
	
	private XAConnection xaConnection;
	
	private Connection connection;
	
	private XAResource xaResource;
	
	private StartXid startXid;
	
	private volatile int currentStatus;
	
	private String uuid;
	
	private String tmAddress;
	
	public XAResourceHolder(String dubboUniqueResourceName, 
			XAConnection xaConnection, Connection connection, XAResource xaResource) {
		String connStr = connection.toString();
		this.uuid = UUID.nameUUIDFromBytes(connStr.getBytes()).toString();
		this.dubboUniqueResourceName = dubboUniqueResourceName;
		this.xaConnection = xaConnection;
		this.connection = connection;
		this.xaResource = xaResource;
		this.currentStatus = XA_UNKNOWN;
		this.tmAddress = ParticipantXATransactionLocal.current().getTmAddress();
	}
	
	public synchronized void start() throws XAException, SystemException, RollbackException {
		if(this.currentStatus == XA_START){
			return;
		}else if(this.currentStatus == XA_UNKNOWN){
			ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
			DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();
			this.startXid = instance.enlistResource(current.getTmAddress(), dubboUniqueResourceName, 
					current.getTid(), instance.getLocalAddress());
			this.xaResource.start(this.startXid.getXid(), this.startXid.getFlags());
			this.currentStatus = XA_START;
			
			XAResourcePool xaResourcePool = instance.getXaResourcePool();
			xaResourcePool.addXAResourceHolder(this);
		}else{
			throw new XAException("xaResource can not xa satrt,currentStatus value "+currentStatus+" is error.");
		}
	}
	
	public synchronized void end() throws XAException{
		if(this.currentStatus == XA_END){
			return;
		}else if(this.currentStatus == XA_START){
			this.xaResource.end(this.startXid.getXid(), XAResource.TMSUCCESS);
			this.currentStatus = XA_END;
		}else if(this.currentStatus == XA_UNKNOWN){
			this.close();
		}else{
			throw new XAException("xaResource can not xa end,currentStatus value "+currentStatus+" is error.");
		}
	}
	
	public synchronized int prepare(Xid xid) throws XAException {
		if(!xid.equals(this.startXid.getXid())){
			throw new XAException("xaResource can not xa prepare,xid are not same start xid.");
		}
		if(this.currentStatus == XA_PREPARE){
			return XAResource.XA_OK;
		}else if(this.currentStatus == XA_END){
			int prepare = this.xaResource.prepare(xid);
			this.currentStatus = XA_PREPARE;
			return prepare;
		}else{
			throw new XAException("xaResource can not xa prepare,currentStatus value "+currentStatus+" is error.");
		}
	}
	
	public synchronized void commit(Xid xid, boolean onePhase) throws XAException {
		if(!xid.equals(this.startXid.getXid())){
			throw new XAException("xaResource can not xa commit,xid are not same start xid.");
		}
		if(this.currentStatus == XA_COMMIT){
			return;
		}else if(this.currentStatus == XA_PREPARE){
			if(onePhase){
				throw new XAException("xaResource can not xa commit one phase,xa prepare already executed.");
			}
			this.xaResource.commit(xid, onePhase);
			this.currentStatus = XA_COMMIT;
		}else if(this.currentStatus == XA_END){
			if(!onePhase){
				throw new XAException("xaResource can not xa commit,xa prepare are not execute.");
			}
			this.xaResource.commit(xid, onePhase);
			this.currentStatus = XA_COMMIT;
		}else{
			throw new XAException("xaResource can not xa commit,currentStatus value "+currentStatus+" is error.");
		}
	}
	
	public synchronized void rollback(Xid xid) throws XAException {
		if(!xid.equals(this.startXid.getXid())){
			throw new XAException("xaResource can not xa rollback,xid are not same start xid.");
		}
		if(this.currentStatus == XA_ROLLBACK){
			return;
		}else if(this.currentStatus == XA_PREPARE){
			this.xaResource.rollback(xid);
			this.currentStatus = XA_ROLLBACK;
		}else if(this.currentStatus == XA_END){
			this.xaResource.rollback(xid);
			this.currentStatus = XA_ROLLBACK;
		}else{
			throw new XAException("xaResource can not xa rollback,currentStatus value "+currentStatus+" is error.");
		}
	}

	public String getDubboUniqueResourceName() {
		return dubboUniqueResourceName;
	}

	public XAConnection getXaConnection() {
		return xaConnection;
	}

	public Connection getConnection() {
		return connection;
	}

	public XAResource getXaResource() {
		return xaResource;
	}

	public int getCurrentStatus() {
		return currentStatus;
	}

	public StartXid getStartXid() {
		return startXid;
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getTmAddress() {
		return tmAddress;
	}

	public synchronized void close() {
		if(currentStatus == CLOSE){
			return;
		}else if(currentStatus == XA_START){
			try {
				this.xaResource.end(this.startXid.getXid(), XAResource.TMSUCCESS);
				this.currentStatus = XA_END;
				this.xaResource.rollback(this.startXid.getXid());
				this.currentStatus = XA_ROLLBACK;
				LOGGER.error("XAResource currentStatus is XA_START,status is error,maybe the transaction is not over.");
				this.doClose();
			} catch (XAException e) {
				LOGGER.error(e.getMessage(),e);
				this.disconnect();
			}
		}else if(currentStatus == XA_END){
			try {
				this.xaResource.rollback(this.startXid.getXid());
				this.currentStatus = XA_ROLLBACK;
				LOGGER.error("XAResource currentStatus is XA_END,status is error,Maybe the transaction is not over.");
				this.doClose();
			} catch (XAException e) {
				LOGGER.error(e.getMessage(),e);
				this.disconnect();
			}
		}else if(currentStatus == XA_PREPARE){
			LOGGER.error("XAResource currentStatus is XA_PREPARE,the transaction is not over,please check atomikos recover().");
			this.disconnect();
		}else{
			this.doClose();
		}
	}
	
	private void disconnect() {
		try {
			if(connection != null){
				Connection unwrap = connection.unwrap(Connection.class);
				unwrap.close();
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(),e);
		}
		this.doClose();
	}
	
	private void doClose() {
		try {
			if(connection != null){
				connection.close();
			}
			if(xaConnection != null){
				xaConnection.close();
			}
		} catch (SQLException e) {
			LOGGER.error(e.getMessage(),e);
		} finally {
			currentStatus = CLOSE;
			connection = null;
			xaConnection = null;
			xaResource = null;
			startXid = null;
			dubboUniqueResourceName = null;
			uuid = null;
			tmAddress = null;
		}
	}
}
