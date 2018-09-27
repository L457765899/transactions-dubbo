package com.sxb.lin.atomikos.dubbo.tm;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;


public class JtaTransactionManager extends org.springframework.transaction.jta.JtaTransactionManager {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition) 
			throws NotSupportedException,SystemException {
		
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			super.doJtaBegin(txObject, definition);
			this.newInitiatorXATransactionLocal(definition.isReadOnly());
		}else{
			if(definition.isReadOnly()){
				if(current.isActive()){
					throw new NotSupportedException("dubbo xa transaction not supported ReadOnly.");
				}
				super.doJtaBegin(txObject, definition);
				this.newInitiatorXATransactionLocal(true);
				return;
			}
			
			if(current.getIsActive() != null && current.getIsActive().booleanValue() == false){
				super.doJtaBegin(txObject, definition);
				this.newInitiatorXATransactionLocal(false);
				return;
			}
			
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_NESTED.");
			}
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_REQUIRES_NEW.");
			}
			current.active();
		}
		
	}
	
	protected void newInitiatorXATransactionLocal(boolean isReadOnly) {
		DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();
		if(!instance.isInit()){
			return;
		}
		CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
		CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
		
		String tid = compositeTransaction.getTid();
		long time = compositeTransaction.getTimeout() + System.currentTimeMillis() + DubboTransactionManagerService.ADD_TIME;
		
		InitiatorXATransactionLocal local = new InitiatorXATransactionLocal();
		local.setTid(tid);
		local.setTmAddress(instance.getLocalAddress());
		local.setTimeOut(time + "");
		local.setReadOnly(isReadOnly);
		local.bindToThread();
	}
	
	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			super.doCommit(status);
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			super.doRollback(status);
		}
	}
	
	@Override
	protected void doCleanupAfterCompletion(Object transaction) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			try {
				super.doCleanupAfterCompletion(transaction);
			} finally {
				this.restoreThreadLocalStatus();
			}
		}
	}

	private void restoreThreadLocalStatus(){
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			current.restoreThreadLocalStatus();
		}
	}
	
}
