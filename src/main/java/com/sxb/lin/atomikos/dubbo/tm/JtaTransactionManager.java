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
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;


public class JtaTransactionManager extends org.springframework.transaction.jta.JtaTransactionManager{

	private static final long serialVersionUID = 1L;
	
	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition) 
			throws NotSupportedException,SystemException {
		
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			super.doJtaBegin(txObject, definition);
			this.newInitiatorXATransactionLocal();
		}else{
			if(current.getIsActive() != null && current.getIsActive().booleanValue() == false){
				super.doJtaBegin(txObject, definition);
				this.newInitiatorXATransactionLocal();
				return;
			}
			
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_NESTED.");
			}
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_REQUIRES_NEW.");
			}
			current.active();
			
			
//			//调用发起者tm,xa start
//			try {
//				StartXid startXid = instance.enlistResource(current.getTmAddress(), current.getTid(), 
//						instance.getLocalAddress(), dubboUniqueResourceName);
//				
//			} catch (RollbackException e) {
//				throw new CannotCreateTransactionException("transaction manager is STATUS_ROLLING_BACK.",e);
//			}
		}
		
	}
	
	protected void newInitiatorXATransactionLocal() {
		DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();
		CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
		CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
		
		String tid = compositeTransaction.getTid();
		
		InitiatorXATransactionLocal local = new InitiatorXATransactionLocal();
		local.setTid(tid);
		local.setTmAddress(instance.getLocalAddress());
		local.bindToThread();
	}
	
	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			try {
				super.doCommit(status);
			} finally {
				this.restoreThreadLocalStatus();
			}
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		if(!ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			try {
				super.doRollback(status);
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
