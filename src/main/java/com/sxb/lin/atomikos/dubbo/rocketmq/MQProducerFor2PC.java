package com.sxb.lin.atomikos.dubbo.rocketmq;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.RPCHook;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.atomikos.datasource.xa.XAResourceTransaction;
import com.atomikos.datasource.xa.XATransactionalResource;
import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.icatch.jta.TransactionManagerImp;
import com.atomikos.icatch.provider.TransactionServiceProvider;

public class MQProducerFor2PC extends TransactionMQProducer{
	
	public MQProducerFor2PC() {
		super();
	}

	public MQProducerFor2PC(String producerGroup, RPCHook rpcHook) {
		super(producerGroup, rpcHook);
	}

	public MQProducerFor2PC(String producerGroup) {
		super(producerGroup);
	}

	public  void sendMessageAfterTransaction(Message msg){
		if(!TransactionSynchronizationManager.isSynchronizationActive()){
			throw new RuntimeException("transaction not start.");
		}
		
		MQMessagesHolder mqMessagesHolder = MQProducerUtils.getMQMessagesHolder(this);
		mqMessagesHolder.addMessage(msg);
	}

	public void send2PCMessageInTransaction(Message msg){
		try {
			TransactionManager transactionManager = TransactionManagerImp.getTransactionManager();
			if(transactionManager == null || transactionManager.getTransaction() == null 
					|| transactionManager.getTransaction().getStatus() == Status.STATUS_NO_TRANSACTION){
				throw new RuntimeException("jta transaction not start.");
			}
			
			CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
			CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
			long timeOut = System.currentTimeMillis() + compositeTransaction.getTimeout() + 3000;
			XAResource xaResource = new MQXAResourceImpl(this,msg,timeOut);
			XATransactionalResource xaTransactionalResource = new MQTemporaryXATransactionalResource(msg.getTopic(),xaResource);
			TransactionServiceProvider transactionService = (TransactionServiceProvider) Configuration.getTransactionService();
			xaTransactionalResource.setRecoveryService(transactionService.getRecoveryService());
			XAResourceTransaction xaResourceTransaction = 
					(XAResourceTransaction) xaTransactionalResource.getResourceTransaction(compositeTransaction);
			xaResourceTransaction.setXAResource(xaResource);
			xaResourceTransaction.resume();
			xaResourceTransaction.suspend();
			
		} catch (SystemException e) {
			throw new RuntimeException(e);
		}
	}
}
