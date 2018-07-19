package com.sxb.lin.atomikos.dubbo.mybatis;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.XAConnection;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.springframework.transaction.support.ResourceHolderSupport;

import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;
import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class XAConnectionHolder extends ResourceHolderSupport{
	
	private final static int XA_UNKNOWN = 0;
	
	private final static int XA_START = 1;
	
	private final static int XA_END = 2;
	
	private final static int XA_PREPARE = 3;
	
	private final static int XA_COMMIT = 4;
	
	private final static int XA_ROLLBACK = 5;
	
	private String dubboUniqueResourceName;
	
	private XAConnection xaConnection;
	
	private Connection connection;
	
	private XAResource xaResource;
	
	private int currentStatus;
	
	private Xid xid;
	
	public XAConnectionHolder(XAConnection xaConnection,
			String dubboUniqueResourceName) throws SQLException {
		super();
		this.dubboUniqueResourceName = dubboUniqueResourceName;
		this.currentStatus = XA_UNKNOWN;
		this.set(xaConnection);
	}
	
	private void set(XAConnection xaConnection) throws SQLException {
		this.xaConnection = xaConnection;
		if(xaConnection != null){
			this.connection = xaConnection.getConnection();
			this.xaResource = xaConnection.getXAResource();
			this.start();
		}
	}
	
	private void start() throws SQLException {
		//调用发起者tm获取xid
		//xa start
		try {
			ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
			DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();
			StartXid startXid = instance.enlistResource(current.getTmAddress(), current.getTid(), 
					instance.getLocalAddress(), dubboUniqueResourceName);
			this.xid = startXid.getXid();
			this.xaResource.start(this.xid, startXid.getFlags());
			this.currentStatus = XA_START;
		} catch (SystemException e) {
			throw new SQLException("transaction manager is SystemException.",e);
		} catch (RollbackException e) {
			throw new SQLException("transaction manager is STATUS_ROLLING_BACK.",e);
		} catch (XAException e) {
			throw new SQLException("start xaResource whith XAException.",e);
		} 
	}
	
	void end() throws XAException{
		//xa end
		if(this.currentStatus == XA_START){
			this.xaResource.end(this.xid, XAResource.TMSUCCESS);
		}else if(this.currentStatus == XA_UNKNOWN){
			//not start
		}else{
			throw new XAException("currentStatus is error,is not XA_START,can not xa end.");
		}
	}

	public boolean hasXAConnection() {
		return (this.xaConnection != null);
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

	public void setXaConnection(XAConnection xaConnection,
			String dubboUniqueResourceName) throws SQLException {
		this.dubboUniqueResourceName = dubboUniqueResourceName;
		this.currentStatus = XA_UNKNOWN;
		this.set(xaConnection);
	}

	@Override
	public void reset() {
		super.reset();
		this.dubboUniqueResourceName = null;
		this.xaConnection = null;
		this.connection = null;
		this.xaResource = null;
		this.currentStatus = XA_UNKNOWN;
		this.xid = null;
	}

	
}
