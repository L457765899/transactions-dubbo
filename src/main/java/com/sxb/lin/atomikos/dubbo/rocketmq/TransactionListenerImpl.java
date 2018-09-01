package com.sxb.lin.atomikos.dubbo.rocketmq;

import java.lang.reflect.Field;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;

import com.atomikos.icatch.config.Configuration;
import com.atomikos.recovery.CoordinatorLogEntry;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.RecoveryLog;
import com.atomikos.recovery.Repository;
import com.atomikos.recovery.imp.RecoveryLogImp;

public class TransactionListenerImpl implements TransactionListener {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TransactionListenerImpl.class);
	
	private Repository repository;

	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		return LocalTransactionState.UNKNOW;
	}

	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		String timeOut = msg.getProperty("XA_TIME_OUT");
		String tid = msg.getProperty("XA_GLOBAL_TRANSACTION_ID");
		if(tid != null && timeOut != null){
			if(this.getRepository() != null){
				try {
					CoordinatorLogEntry coordinatorLogEntry = this.getRepository().get(tid);
					if(coordinatorLogEntry != null){
						if(coordinatorLogEntry.wasCommitted){
							LOGGER.info(msg.getTransactionId() + " check local transaction is commit.");
							return LocalTransactionState.COMMIT_MESSAGE;
						}else{
							LOGGER.error(msg.getTransactionId() + " check local transaction is rollback.");
							return LocalTransactionState.ROLLBACK_MESSAGE;
						}
					}else{
						if(Long.parseLong(timeOut) < System.currentTimeMillis()){
							LOGGER.error(msg.getTransactionId() + " check local transaction is time out,so rollback.");
							return LocalTransactionState.ROLLBACK_MESSAGE;
						}
					}
				} catch (LogReadException e) {
					LOGGER.error(e.getMessage(), e);
				}
			}
		}
		LOGGER.warn(msg.getTransactionId() + " check local transaction is unknow.");
		return LocalTransactionState.UNKNOW;
	}

	public Repository getRepository() {
		if(repository != null){
			return repository;
		}
		RecoveryLog recoveryLog = Configuration.getRecoveryLog();
		if(recoveryLog == null){
			return null;
		}
		Field findField = ReflectionUtils.findField(RecoveryLogImp.class, "repository");
		findField.setAccessible(true);
		try {
			this.repository = (Repository) findField.get(recoveryLog);
		} catch (IllegalArgumentException e) {
			LOGGER.error(e.getMessage(), e);
		} catch (IllegalAccessException e) {
			LOGGER.error(e.getMessage(), e);
		}
		
		return repository;
	}
}
