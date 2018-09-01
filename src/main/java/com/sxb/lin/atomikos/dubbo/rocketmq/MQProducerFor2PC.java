package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.util.UUID;

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
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.pool.MQXAResourceHolder;
import com.sxb.lin.atomikos.dubbo.pool.XAResourceHolder;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;

public class MQProducerFor2PC extends TransactionMQProducer{
	
	public final static String MQ_UNIQUE_TOPIC_PREFIX = "Rocket_MQ_Unique_";
	
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
		if(ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			MQMessagesHolder mqMessagesHolder = MQProducerUtils.getMQMessagesHolderToDubbo(this);
			mqMessagesHolder.addMessage(msg);
			
			XAResource xaResource = new MQNOResourceImpl(this, mqMessagesHolder);
			XAResourceHolder xaResourceHolder = new MQXAResourceHolder(
					MQ_UNIQUE_TOPIC_PREFIX + msg.getTopic(), UUID.randomUUID().toString(), xaResource);
			
			try {
				xaResourceHolder.start();
				xaResourceHolder.end();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}else{
			if(!TransactionSynchronizationManager.isSynchronizationActive()){
				throw new RuntimeException("transaction not start.");
			}
			
			MQMessagesHolder mqMessagesHolder = MQProducerUtils.getMQMessagesHolderToLocal(this);
			mqMessagesHolder.addMessage(msg);
		}
	}

	public void send2PCMessageInTransaction(Message msg){
		if(ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			
			ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
			XAResource xaResource = new MQXAResourceImpl(this, msg, Long.parseLong(current.getTimeOut()));
			XAResourceHolder xaResourceHolder = new MQXAResourceHolder(
					MQ_UNIQUE_TOPIC_PREFIX + msg.getTopic(), UUID.randomUUID().toString(), xaResource);
			
			try {
				xaResourceHolder.start();
				xaResourceHolder.end();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			
		}else{
			try {
				
				TransactionManager transactionManager = TransactionManagerImp.getTransactionManager();
				if(transactionManager == null || transactionManager.getTransaction() == null 
						|| transactionManager.getTransaction().getStatus() == Status.STATUS_NO_TRANSACTION){
					throw new RuntimeException("jta transaction not start.");
				}
				
				CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
				CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
				long timeOut = System.currentTimeMillis() + compositeTransaction.getTimeout() + DubboTransactionManagerService.ADD_TIME;
				XAResource xaResource = new MQXAResourceImpl(this,msg,timeOut);
				XATransactionalResource xaTransactionalResource = 
						new MQTemporaryXATransactionalResource(MQ_UNIQUE_TOPIC_PREFIX + msg.getTopic(),xaResource);
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
}
