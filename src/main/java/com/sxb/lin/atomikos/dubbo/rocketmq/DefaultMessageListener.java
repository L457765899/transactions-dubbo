package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.List;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

public class DefaultMessageListener implements MessageListenerConcurrently,MessageListenerOrderly{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMessageListener.class);
	
	private ApplicationEventPublisher publisher;
	
	public DefaultMessageListener(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	public ConsumeOrderlyStatus consumeMessage(List<MessageExt> msgs,
			ConsumeOrderlyContext context) {
		
		MQEvent event = new MQEvent(this, msgs.get(0).getTopic(), msgs, context);
		try {
			publisher.publishEvent(event);
			return ConsumeOrderlyStatus.SUCCESS;
		} catch (Throwable e) {
			this.printLog(event, e);
		}
		
		return ConsumeOrderlyStatus.SUSPEND_CURRENT_QUEUE_A_MOMENT;
	}

	public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
			ConsumeConcurrentlyContext context) {
		
		MQEvent event = new MQEvent(this, msgs.get(0).getTopic(), msgs, context);
		try {
			publisher.publishEvent(event);
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		} catch (Throwable e) {
			this.printLog(event, e);
		}
		
		return ConsumeConcurrentlyStatus.RECONSUME_LATER;
	}

	protected void printLog(MQEvent event, Throwable e) {
		LOGGER.error(event.getFirstMsgId() + ":" + e.getMessage(), e);
	}
}
