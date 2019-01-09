package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.List;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.ApplicationEventPublisher;

public class DefaultMessageListenerConcurrently extends AbstractMessageListener 
		implements MessageListenerConcurrently{

	public DefaultMessageListenerConcurrently(ApplicationEventPublisher publisher) {
		super(publisher);
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
}
