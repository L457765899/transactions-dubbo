package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.List;

import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeOrderlyContext;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.context.ApplicationEvent;

public class MQEvent extends ApplicationEvent {

	private static final long serialVersionUID = 1L;
	
	private String topic;
	
	private List<MessageExt> msgs;
	
	private ConsumeConcurrentlyContext concurrentlyContext;
	
	private ConsumeOrderlyContext orderlyContext;
	
	private boolean isOrder;

	public MQEvent(Object source,String topic, List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
		super(source);
		this.topic = topic;
		this.msgs = msgs;
		this.concurrentlyContext = context;
		this.isOrder = false;
	}

	public MQEvent(Object source,String topic, List<MessageExt> msgs, ConsumeOrderlyContext context) {
		super(source);
		this.topic = topic;
		this.msgs = msgs;
		this.orderlyContext = context;
		this.isOrder = true;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public List<MessageExt> getMsgs() {
		return msgs;
	}

	public void setMsgs(List<MessageExt> msgs) {
		this.msgs = msgs;
	}

	public ConsumeConcurrentlyContext getConcurrentlyContext() {
		return concurrentlyContext;
	}

	public void setConcurrentlyContext(
			ConsumeConcurrentlyContext concurrentlyContext) {
		this.concurrentlyContext = concurrentlyContext;
	}

	public ConsumeOrderlyContext getOrderlyContext() {
		return orderlyContext;
	}

	public void setOrderlyContext(ConsumeOrderlyContext orderlyContext) {
		this.orderlyContext = orderlyContext;
	}

	public boolean isOrder() {
		return isOrder;
	}

	public void setOrder(boolean isOrder) {
		this.isOrder = isOrder;
	}
	
	public String getFirstMsgId() {
		if(msgs != null && msgs.size() > 0) {
			return msgs.get(0).getMsgId();
		}
		return null;
	}
}
