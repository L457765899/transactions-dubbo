package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.Collection;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

public interface MQMessagesSendLog {

	void sendSuccess(Message msg,SendResult sendResult);
	
	void sendOnException(Message msg,Throwable throwable);
	
	void sendSuccess(Collection<Message> msgs,SendResult sendResult);
	
	void sendOnException(Collection<Message> msgs,Throwable throwable);
	
	void firstSendSuccess(Message msg,SendResult sendResult);
	
	void firstSendOnException(Message msg,Throwable throwable);
	
	void secondSendSuccess(Message msg,SendResult sendResult,LocalTransactionState localTransactionState);
	
	void secondSendOnException(Message msg,SendResult sendResult,LocalTransactionState localTransactionState,Throwable throwable);
	
}
