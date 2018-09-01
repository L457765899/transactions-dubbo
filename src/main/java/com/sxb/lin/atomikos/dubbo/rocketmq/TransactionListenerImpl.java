package com.sxb.lin.atomikos.dubbo.rocketmq;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;

public class TransactionListenerImpl implements TransactionListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionListenerImpl.class);
	
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		return LocalTransactionState.UNKNOW;
	}

	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		String tmAddress = msg.getProperty(MQXAResourceImpl.XA_TM_ADDRESS);
		String tid = msg.getProperty(MQXAResourceImpl.XA_GLOBAL_TRANSACTION_ID);
		if(tid == null || tmAddress == null){
			LOGGER.error(msg.getTransactionId() + " check local transaction is rollback.");
			return LocalTransactionState.ROLLBACK_MESSAGE;
		}
		
		DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();			
		Boolean wasCommitted = instance.wasCommitted(tmAddress, tid);
		if(wasCommitted != null){
			if(wasCommitted.booleanValue()){
				LOGGER.info(msg.getTransactionId() + " check local transaction is commit.");
				return LocalTransactionState.COMMIT_MESSAGE;
			}else{
				LOGGER.error(msg.getTransactionId() + " check local transaction is rollback.");
				return LocalTransactionState.ROLLBACK_MESSAGE;
			}
		}
		
		LOGGER.warn(msg.getTransactionId() + " check local transaction is unknow.");
		return LocalTransactionState.UNKNOW;
	}
}
