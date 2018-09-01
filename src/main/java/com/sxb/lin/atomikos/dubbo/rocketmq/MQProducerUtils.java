package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.List;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public abstract class MQProducerUtils {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MQProducerUtils.class);

	public static MQMessagesHolder getMQMessagesHolder(MQProducerFor2PC producer){
		
		MQMessagesHolder mqmHolder = (MQMessagesHolder) TransactionSynchronizationManager.getResource(producer);
		if(mqmHolder != null){
			return mqmHolder;
		}
		
		mqmHolder = new MQMessagesHolder();
		TransactionSynchronizationManager.registerSynchronization(new MQMessagesSynchronization(producer, mqmHolder));
		TransactionSynchronizationManager.bindResource(producer, mqmHolder);
		return mqmHolder;
	}
	
	public static void send(MQProducerFor2PC producer, List<Message> messages){
		try {
			
			SendResult sendResult = null;
			if(messages.size() == 1){
				sendResult = producer.send(messages.get(0));
			} else{
				sendResult = producer.send(messages);
			}
			if(sendResult.getSendStatus() != SendStatus.SEND_OK){
				LOGGER.error("sendMessageAfterTransaction fail " + sendResult.toString());
			}
		} catch (MQClientException e) {
			LOGGER.error("sendMessageAfterTransaction error." + e.getMessage(), e);
		} catch (RemotingException e) {
			LOGGER.error("sendMessageAfterTransaction error." + e.getMessage(), e);
		} catch (MQBrokerException e) {
			LOGGER.error("sendMessageAfterTransaction error." + e.getMessage(), e);
		} catch (InterruptedException e) {
			LOGGER.error("sendMessageAfterTransaction error." + e.getMessage(), e);
		}
	}
	
	private static class MQMessagesSynchronization extends TransactionSynchronizationAdapter {
		
		private MQProducerFor2PC producer;
		
		private MQMessagesHolder mqmHolder;
		
		private boolean holderActive = true;

		public MQMessagesSynchronization(MQProducerFor2PC producer,MQMessagesHolder mqmHolder) {
			this.producer = producer;
			this.mqmHolder = mqmHolder;
		}

		@Override
		public int getOrder() {
			return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER;
		}

		@Override
		public void suspend() {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResource(producer);
			}
		}

		@Override
		public void resume() {
			if (this.holderActive) {
				TransactionSynchronizationManager.bindResource(this.producer, this.mqmHolder);
			}
		}

		@Override
		public void beforeCompletion() {
			TransactionSynchronizationManager.unbindResource(this.producer);
			this.holderActive = false;
		}
		
		@Override
		public void afterCommit() {
			if (this.holderActive) {
				if(!this.mqmHolder.isEmpty()){
					List<Message> messages = this.mqmHolder.getMessages();
					send(producer, messages);
				}
			}
		}

		@Override
		public void afterCompletion(int status) {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResourceIfPossible(this.producer);
				this.holderActive = false;
			}
			this.mqmHolder.reset();
		}
		
		
	}
}
