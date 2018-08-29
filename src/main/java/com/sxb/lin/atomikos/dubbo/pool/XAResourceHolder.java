package com.sxb.lin.atomikos.dubbo.pool;

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


public abstract class XAResourceHolder {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(XAResourceHolder.class);
	
	protected final static int XA_UNKNOWN = 0;
	
	protected final static int XA_START = 1;
	
	protected final static int XA_END = 2;
	
	protected final static int XA_PREPARE = 3;
	
	protected final static int XA_COMMIT = 4;
	
	protected final static int XA_ROLLBACK = 5;
	
	protected final static int CLOSE = 6;
	
	private String dubboUniqueResourceName;
	
	private XAResource xaResource;
	
	private StartXid startXid;
	
	private volatile int currentStatus;
	
	private String uuid;
	
	private String tmAddress;
	
	public XAResourceHolder(String dubboUniqueResourceName, String uuid, XAResource xaResource) {
		this.uuid = uuid;
		this.dubboUniqueResourceName = dubboUniqueResourceName;
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
			throw new XAException("xaResource can not xa satrt,currentStatus value " 
					+ this.getStrCurrentStatus(currentStatus) + " is error.");
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
			throw new XAException("xaResource can not xa end,currentStatus value " 
					+ this.getStrCurrentStatus(currentStatus) + " is error.");
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
			throw new XAException("xaResource can not xa prepare,currentStatus value " 
					+ this.getStrCurrentStatus(currentStatus) + " is error.");
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
			throw new XAException("xaResource can not xa commit,currentStatus value " 
					+ this.getStrCurrentStatus(currentStatus) + " is error.");
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
			throw new XAException("xaResource can not xa rollback,currentStatus value " 
					+ this.getStrCurrentStatus(currentStatus) + " is error.");
		}
	}
	
	public String getUuid() {
		return uuid;
	}
	
	public String getTmAddress() {
		return tmAddress;
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
	
	public String getDubboUniqueResourceName() {
		return dubboUniqueResourceName;
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
	
	protected String getStrCurrentStatus(int currentStatus){
		String str = null;
		switch (currentStatus) {
		case XA_UNKNOWN:
			str = "XA_UNKNOWN";
			break;
		case XA_START:
			str = "XA_START";
			break;
		case XA_END:
			str = "XA_END";
			break;
		case XA_PREPARE:
			str = "XA_PREPARE";
			break;
		case XA_COMMIT:
			str = "XA_COMMIT";
			break;
		case XA_ROLLBACK:
			str = "XA_ROLLBACK";
			break;
		case CLOSE:
			str = "CLOSE";
			break;
		default:
			str = "ERROR";
			break;
		}
		return str;
	}
	
	protected void clear(){
		currentStatus = CLOSE;
		xaResource = null;
		startXid = null;
		dubboUniqueResourceName = null;
		uuid = null;
		tmAddress = null;
	}
	
	protected abstract void disconnect();
	
	protected abstract void doClose();
}
