package com.sxb.lin.atomikos.dubbo.rocketmq;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

public class MQNOResourceImpl implements XAResource{

	private MQProducerFor2PC producer;
	
	private MQMessagesHolder mqmHolder;
	
	public MQNOResourceImpl(MQProducerFor2PC producer, MQMessagesHolder mqmHolder) {
		this.producer = producer;
		this.mqmHolder = mqmHolder;
	}

	public int prepare(Xid xid) throws XAException {
        return XAResource.XA_OK;
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		MQProducerUtils.send(this.producer, this.mqmHolder);
	}
	
	public void rollback(Xid xid) throws XAException {
		this.mqmHolder.reset();
	}
	
	public void start(Xid xid, int flags) throws XAException {
		//do nothing
	}

	public void end(Xid xid, int flags) throws XAException {
		//do nothing
	}
	
	public Xid[] recover(int flag) throws XAException {
		return null;
	}

	public void forget(Xid xid) throws XAException {
		//do nothing
	}
	
	public boolean setTransactionTimeout(int seconds) throws XAException {
		//do nothing
		return false;
	}

	public int getTransactionTimeout() throws XAException {
		//do nothing
		return 0;
	}

	public boolean isSameRM(XAResource xares) throws XAException {
		return this == xares;
	}
}
