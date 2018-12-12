package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public abstract class MQProducerUtils {
	
	public static MQMessagesHolder getMQMessagesHolder(MQProducerFor2PC producer) {
		MQMessagesHolder mqmHolder = (MQMessagesHolder) TransactionSynchronizationManager.getResource(producer);
		return mqmHolder;
	}
	
	public static MQMessagesHolder getMQMessagesHolderToLocal(MQProducerFor2PC producer){
		
		MQMessagesHolder mqmHolder = getMQMessagesHolder(producer);
		if(mqmHolder != null){
			return mqmHolder;
		}
		
		mqmHolder = new MQMessagesHolder();
		TransactionSynchronizationManager.registerSynchronization(new LocalMQMessagesSynchronization(producer, mqmHolder));
		TransactionSynchronizationManager.bindResource(producer, mqmHolder);
		return mqmHolder;
	}
	
	public static MQMessagesHolder getMQMessagesHolderToDubbo(MQProducerFor2PC producer){
		
		MQMessagesHolder mqmHolder = getMQMessagesHolder(producer);
		if(mqmHolder != null){
			return mqmHolder;
		}
		
		mqmHolder = new MQMessagesHolder();
		TransactionSynchronizationManager.registerSynchronization(new DubboMQMessagesSynchronization(producer));
		TransactionSynchronizationManager.bindResource(producer, mqmHolder);
		return mqmHolder;
	}
	
	public static void sendAsync(MQProducerFor2PC producer, final Message msg) {
		final MQMessagesSendLog messagesSendLog = producer.getMessagesSendLog();
		try {
			producer.send(msg, new SendCallback() {
				
				public void onSuccess(SendResult sendResult) {
					messagesSendLog.sendSuccess(msg, sendResult);
				}
				
				public void onException(Throwable e) {
					messagesSendLog.sendAsyncOnException(msg, e);
				}
				
			});
		} catch (MQClientException e) {
			messagesSendLog.sendOnException(msg, e);
			throw new RuntimeException(e);
		} catch (RemotingException e) {
			messagesSendLog.sendOnException(msg, e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			messagesSendLog.sendOnException(msg, e);
			throw new RuntimeException(e);
		}
	}
	
	public static void send(MQProducerFor2PC producer, Message msg) {
		MQMessagesSendLog messagesSendLog = producer.getMessagesSendLog();
		try {
			SendResult sendResult = producer.send(msg);
			messagesSendLog.sendSuccess(msg, sendResult);
		} catch (MQClientException e) {
			messagesSendLog.sendOnException(msg, e);
			throw new RuntimeException(e);
		} catch (RemotingException e) {
			messagesSendLog.sendOnException(msg, e);
			throw new RuntimeException(e);
		} catch (MQBrokerException e) {
			messagesSendLog.sendOnException(msg, e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			messagesSendLog.sendOnException(msg, e);
			throw new RuntimeException(e);
		}
	}
	
	public static void send(MQProducerFor2PC producer, List<Message> msgs) {
		MQMessagesSendLog messagesSendLog = producer.getMessagesSendLog();
		try {
			SendResult sendResult = producer.send(msgs);
			messagesSendLog.sendSuccess(msgs, sendResult);
		} catch (MQClientException e) {
			messagesSendLog.sendOnException(msgs, e);
			throw new RuntimeException(e);
		} catch (RemotingException e) {
			messagesSendLog.sendOnException(msgs, e);
			throw new RuntimeException(e);
		} catch (MQBrokerException e) {
			messagesSendLog.sendOnException(msgs, e);
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			messagesSendLog.sendOnException(msgs, e);
			throw new RuntimeException(e);
		}
	}
	
	public static void sendBeforeCommit(MQProducerFor2PC producer, MQMessagesHolder mqmHolder){
		if(!mqmHolder.isEmpty()){
			List<MQMessageHolder> messages = mqmHolder.getMessages();
			Map<String,List<Message>> topicMap = new HashMap<String, List<Message>>();
			List<MQMessageHolder> removeMessages = new ArrayList<MQMessageHolder>();
			
			for(MQMessageHolder msgHolder : messages) {
				if(!msgHolder.isBeforeCommit()) {
					continue;
				}
				
				removeMessages.add(msgHolder);
				Message msg = msgHolder.getMessage();
				
				if(msgHolder.isAsync()) {
					sendAsync(producer, msg);
				}else {
					String topic = msg.getTopic();
					List<Message> list = topicMap.get(topic);
					if(list == null){
						list = new ArrayList<Message>();
						topicMap.put(topic, list);
					}
					list.add(msg);
				}
			}
			
			if(removeMessages.size() > 0) {
				mqmHolder.removeMessage(removeMessages);
			}
			
			if(topicMap.size() > 0) {
				for(List<Message> list : topicMap.values()){
					if(list.size() == 1) {
						send(producer, list.get(0));
					}else {
						send(producer, list);
					}
				}
			}
		}
	}
	
	public static void sendAfterCommit(MQProducerFor2PC producer, MQMessagesHolder mqmHolder){
		if(!mqmHolder.isEmpty()){
			List<MQMessageHolder> messages = mqmHolder.getMessages();
			Map<String,List<Message>> topicMap = new HashMap<String, List<Message>>();
			
			for(MQMessageHolder msgHolder : messages) {
				Message msg = msgHolder.getMessage();
				if(msgHolder.isAsync()) {
					sendAsync(producer, msg);
				}else {
					String topic = msg.getTopic();
					List<Message> list = topicMap.get(topic);
					if(list == null){
						list = new ArrayList<Message>();
						topicMap.put(topic, list);
					}
					list.add(msg);
				}
			}
			
			if(topicMap.size() > 0) {
				for(List<Message> list : topicMap.values()){
					if(list.size() == 1) {
						send(producer, list.get(0));
					}else {
						send(producer, list);
					}
				}
			}
		}
		mqmHolder.reset();
	}
	
	private static class LocalMQMessagesSynchronization extends TransactionSynchronizationAdapter {
		
		private MQProducerFor2PC producer;
		
		private MQMessagesHolder mqmHolder;
		
		private boolean holderActive = true;

		public LocalMQMessagesSynchronization(MQProducerFor2PC producer,MQMessagesHolder mqmHolder) {
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
		public void beforeCommit(boolean readOnly) {
			sendBeforeCommit(this.producer, this.mqmHolder);
		}

		@Override
		public void beforeCompletion() {
			TransactionSynchronizationManager.unbindResource(this.producer);
			this.holderActive = false;
		}
		
		@Override
		public void afterCommit() {
			sendAfterCommit(this.producer, this.mqmHolder);
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
	
	private static class DubboMQMessagesSynchronization extends TransactionSynchronizationAdapter {
		
		private MQProducerFor2PC producer;
		
		private boolean holderActive = true;

		public DubboMQMessagesSynchronization(MQProducerFor2PC producer) {
			this.producer = producer;
		}

		@Override
		public int getOrder() {
			return DataSourceUtils.CONNECTION_SYNCHRONIZATION_ORDER;
		}

		@Override
		public void beforeCompletion() {
			TransactionSynchronizationManager.unbindResource(this.producer);
			this.holderActive = false;
		}

		@Override
		public void afterCompletion(int status) {
			if (this.holderActive) {
				TransactionSynchronizationManager.unbindResourceIfPossible(this.producer);
				this.holderActive = false;
			}
		}
		
	}
}
