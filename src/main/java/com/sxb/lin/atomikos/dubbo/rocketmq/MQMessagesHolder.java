package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.ArrayList;
import java.util.List;

import org.apache.rocketmq.common.message.Message;
import org.springframework.transaction.support.ResourceHolderSupport;

public class MQMessagesHolder extends ResourceHolderSupport{

	private List<Message> messages;

	public MQMessagesHolder() {
		this.messages = new ArrayList<Message>();
	}
	
	public void addMessage(Message msg){
		this.messages.add(msg);
	}

	@Override
	public void reset() {
		super.reset();
		messages = null;
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
	
}
