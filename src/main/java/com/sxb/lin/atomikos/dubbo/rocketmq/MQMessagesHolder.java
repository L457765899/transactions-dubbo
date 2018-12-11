package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.ArrayList;
import java.util.List;

import org.apache.rocketmq.common.message.Message;
import org.springframework.transaction.support.ResourceHolderSupport;

public class MQMessagesHolder extends ResourceHolderSupport{

	private List<Message> messages;
	
	private boolean async;
	
	private boolean beforeCommit;

	public MQMessagesHolder() {
		this.messages = new ArrayList<Message>();
	}
	
	public void addMessage(Message msg){
		this.messages.add(msg);
	}
	
	public void removeMessage(Message msg){
		this.messages.remove(msg);
	}

	@Override
	public void reset() {
		super.reset();
		messages = null;
		async = false;
		beforeCommit = false;
	}
	
	public boolean isEmpty(){
		if(messages == null || messages.size() == 0){
			return true;
		}
		
		return false;
	}

	public List<Message> getMessages() {
		return messages;
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
