package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.ArrayList;
import java.util.List;

import org.apache.rocketmq.common.message.Message;
import org.springframework.transaction.support.ResourceHolderSupport;

public class MQMessagesHolder extends ResourceHolderSupport{

	private List<MQMessageHolder> messages;

	public MQMessagesHolder() {
		this.messages = new ArrayList<MQMessageHolder>();
	}
	
	public void addMessage(Message msg, boolean async, boolean beforeCommit){
		MQMessageHolder msgHolder = new MQMessageHolder();
		msgHolder.setMessage(msg);
		msgHolder.setAsync(async);
		msgHolder.setBeforeCommit(beforeCommit);
		this.messages.add(msgHolder);
	}
	
	public void removeMessage(Message msg){
		for(MQMessageHolder msgHolder : messages) {
			if(msgHolder.getMessage() == msg) {
				this.messages.remove(msgHolder);
			}
		}
	}
	
	public void removeMessage(List<MQMessageHolder> msgHolders){
		this.messages.removeAll(msgHolders);
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

	public List<MQMessageHolder> getMessages() {
		return messages;
	}
}
