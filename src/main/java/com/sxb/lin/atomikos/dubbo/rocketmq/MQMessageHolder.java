package com.sxb.lin.atomikos.dubbo.rocketmq;

import org.apache.rocketmq.common.message.Message;

public class MQMessageHolder {

	private Message message;
	
	private boolean async;
	
	private boolean beforeCommit;

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public boolean isBeforeCommit() {
		return beforeCommit;
	}

	public void setBeforeCommit(boolean beforeCommit) {
		this.beforeCommit = beforeCommit;
	}
	
}
