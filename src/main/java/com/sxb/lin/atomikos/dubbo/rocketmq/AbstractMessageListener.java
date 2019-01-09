package com.sxb.lin.atomikos.dubbo.rocketmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;

public abstract class AbstractMessageListener {
	
	protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractMessageListener.class);
	
	protected ApplicationEventPublisher publisher;
	
	public AbstractMessageListener(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	protected void printLog(MQEvent event, Throwable e) {
		LOGGER.error(event.getFirstMsgId() + ":" + e.getMessage(), e);
	}
}
