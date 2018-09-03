package com.sxb.lin.atomikos.dubbo.rocketmq;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.apache.rocketmq.client.Validators;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.impl.producer.DefaultMQProducerImpl;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageAccessor;
import org.apache.rocketmq.common.message.MessageConst;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;

public class MQXAResourceImpl implements XAResource{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MQXAResourceImpl.class);
	
	public final static String XA_TM_ADDRESS = "XA_TM_ADDRESS";
	
	public final static String XA_FORMAT_ID = "XA_FORMAT_ID";
	
	public final static String XA_GLOBAL_TRANSACTION_ID = "XA_GLOBAL_TRANSACTION_ID";
	
	public final static String XA_BRANCH_QUALIFIER = "XA_BRANCH_QUALIFIER";
	
	
	private MQProducerFor2PC producer;
	
	private Message msg;
	
	private SendResult sendResult;
	
	private boolean isPrepare;
	
	private boolean isCommit;
	
	private long timeOut;
	
	public MQXAResourceImpl(MQProducerFor2PC producer, Message msg, long timeOut) {
		this.producer = producer;
		this.msg = msg;
		this.isPrepare = false;
		this.isCommit = false;
		this.timeOut = timeOut;
	}

	public int prepare(Xid xid) throws XAException {
		if(isPrepare){
			throw new XAException(XAException.XAER_DUPID);
		}
		
		MessageAccessor.putProperty(msg, MessageConst.PROPERTY_TRANSACTION_PREPARED, "true");
        MessageAccessor.putProperty(msg, MessageConst.PROPERTY_PRODUCER_GROUP, producer.getProducerGroup());
        
        MQMessagesSendLog messagesSendLog = producer.getMessagesSendLog();
        try {
        	sendResult = producer.getDefaultMQProducerImpl().send(msg);
        	isPrepare = true;
        	messagesSendLog.firstSendSuccess(msg, sendResult);
        } catch (Exception e) {
        	messagesSendLog.firstSendOnException(msg, e);
        	throw new XAException(XAException.XAER_RMERR);
        }
        
        switch (sendResult.getSendStatus()) {
        case SEND_OK:
        	return XAResource.XA_OK;
        case FLUSH_DISK_TIMEOUT:
        	throw new XAException(XAException.XAER_RMFAIL);
        case FLUSH_SLAVE_TIMEOUT:
        	throw new XAException(XAException.XAER_RMFAIL);
        case SLAVE_NOT_AVAILABLE:
        	throw new XAException(XAException.XAER_RMFAIL);
        default:
        	throw new XAException(XAException.XAER_RMERR);
    	}
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		if(sendResult == null){
			return;
		}
		if(isCommit){
			return;
		}
		DefaultMQProducerImpl defaultMQProducerImpl = producer.getDefaultMQProducerImpl();
    	LocalTransactionState localTransactionState = LocalTransactionState.COMMIT_MESSAGE;
    	MQMessagesSendLog messagesSendLog = producer.getMessagesSendLog();
        try {
        	defaultMQProducerImpl.endTransaction(sendResult, LocalTransactionState.COMMIT_MESSAGE, null);
        	isCommit = true;
        	messagesSendLog.secondSendSuccess(msg, sendResult, localTransactionState);
        } catch (Exception e) {
        	if(this.timeOut > System.currentTimeMillis()){
        		throw new XAException(XAException.XAER_RMERR);
        	}else{
        		messagesSendLog.secondSendOnException(msg, sendResult, localTransactionState, e);
        	}
        }
	}
	
	public void rollback(Xid xid) throws XAException {
		if(sendResult == null){
			return;
		}
		DefaultMQProducerImpl defaultMQProducerImpl = producer.getDefaultMQProducerImpl();
        LocalTransactionState localTransactionState = LocalTransactionState.ROLLBACK_MESSAGE;
        MQMessagesSendLog messagesSendLog = producer.getMessagesSendLog();
        try {
        	defaultMQProducerImpl.endTransaction(sendResult, localTransactionState, null);
        	messagesSendLog.secondSendSuccess(msg, sendResult, localTransactionState);
        } catch (Exception e) {
        	if(this.timeOut > System.currentTimeMillis()){
        		throw new XAException(XAException.XAER_RMERR);
        	}else{
        		messagesSendLog.secondSendOnException(msg, sendResult, localTransactionState, e);
        	}
        }
	}
	
	public void start(Xid xid, int flags) throws XAException {
		try {
			Validators.checkMessage(msg, producer);
		} catch (MQClientException e) {
			LOGGER.error(e.getMessage(), e);
			throw new XAException(XAException.XAER_INVAL);
		}
		
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			msg.putUserProperty(XA_TM_ADDRESS, current.getTmAddress());
		}else{
			ParticipantXATransactionLocal pcurrent = ParticipantXATransactionLocal.current();
			msg.putUserProperty(XA_TM_ADDRESS, pcurrent.getTmAddress());
		}
		msg.putUserProperty(XA_FORMAT_ID, xid.getFormatId() + "");
		msg.putUserProperty(XA_GLOBAL_TRANSACTION_ID, new String(xid.getGlobalTransactionId()));
		msg.putUserProperty(XA_BRANCH_QUALIFIER, new String(xid.getBranchQualifier()));
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
