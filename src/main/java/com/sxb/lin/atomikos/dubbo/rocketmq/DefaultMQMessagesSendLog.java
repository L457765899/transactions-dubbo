package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.Collection;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultMQMessagesSendLog implements MQMessagesSendLog {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMQMessagesSendLog.class);

	
	public void sendSuccess(Message msg, SendResult sendResult) {
		if(sendResult.getSendStatus() == SendStatus.SEND_OK){
			
		}else{
			LOGGER.error("sendMessageAfterTransaction fail " + sendResult.toString());
		}
	}
	
	public void sendSuccess(Collection<Message> msgs, SendResult sendResult) {
		if(sendResult.getSendStatus() == SendStatus.SEND_OK){
			
		}else{
			LOGGER.error("sendMessageAfterTransaction fail " + sendResult.toString());
		}
	}
	
	
	public void sendOnException(Message msg, Throwable throwable) {
		
	}
	
	public void sendOnException(Collection<Message> msgs, Throwable throwable) {
		
	}
	
	public void sendAsyncOnException(Message msg, Throwable throwable) {
		LOGGER.error("send async message error." + throwable.getMessage(), throwable);
	}
	
	
	public void firstSendSuccess(Message msg, SendResult sendResult) {
		if(sendResult.getSendStatus() == SendStatus.SEND_OK){
			
		}else{
			LOGGER.error("prepare send fail " + sendResult.toString());
		}
	}
	
	public void secondSendSuccess(Message msg, SendResult sendResult,
			LocalTransactionState localTransactionState) {
		
	}
	
	
	public void firstSendOnException(Message msg, Throwable throwable) {
		LOGGER.error(throwable.getMessage(), throwable);
	}

	public void secondSendOnException(Message msg, SendResult sendResult,
			LocalTransactionState localTransactionState, Throwable throwable) {
		LOGGER.warn("local transaction execute " + localTransactionState + ", but end broker transaction failed", throwable);
	}
	
}
