package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.List;

import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerOrderly;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.ApplicationEventPublisher;

public class DefaultMessageListenerOrderly extends AbstractMessageListener 
		implements MessageListenerOrderly{

	public DefaultMessageListenerOrderly(ApplicationEventPublisher publisher) {
		super(publisher);
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
}
