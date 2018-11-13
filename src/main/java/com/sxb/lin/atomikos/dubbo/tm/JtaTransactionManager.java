package com.sxb.lin.atomikos.dubbo.tm;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.springframework.transaction.NestedTransactionNotSupportedException;
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
import com.sxb.lin.atomikos.dubbo.spring.XAAnnotationInfo;
import com.sxb.lin.atomikos.dubbo.spring.XAInvocationLocal;


public class JtaTransactionManager extends org.springframework.transaction.jta.JtaTransactionManager {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition) 
			throws NotSupportedException,SystemException {
		
		XAAnnotationInfo info = XAInvocationLocal.info();
		if(info.isNoXA()){
			this.doJtaBegin(txObject, definition, false);
			return;
		}
		
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			boolean isActive = definition.isReadOnly() ? false : true;
			this.doJtaBegin(txObject, definition, isActive);
		}else{
			if(current.getIsActive() != null && current.getIsActive().booleanValue() == false){
				boolean isActive = definition.isReadOnly() ? false : true;
				this.doJtaBegin(txObject, definition, isActive);
				return;
			}
			
			if(definition.isReadOnly()){
				throw new NotSupportedException("dubbo xa transaction not supported ReadOnly.");
			}
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
				throw new NestedTransactionNotSupportedException("dubbo xa transaction not supported PROPAGATION_NESTED.");
			}
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW){
				throw new NestedTransactionNotSupportedException("dubbo xa transaction not supported PROPAGATION_REQUIRES_NEW.");
			}
			
			current.active();
		}
		
	}
	
	private void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition, boolean isActive) 
			throws NotSupportedException, SystemException{
		super.doJtaBegin(txObject, definition);
		this.newInitiatorXATransactionLocal(isActive);
	}
	
	protected void newInitiatorXATransactionLocal(boolean isActive) {
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
		local.setActive(isActive);
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

	@Override
	protected boolean isExistingTransaction(Object transaction) {
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current != null && current.getIsActive() != null 
				&& current.getIsActive().booleanValue() == true){
			XAAnnotationInfo info = XAInvocationLocal.info();
			if(info.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED){
				throw new NestedTransactionNotSupportedException(
						"dubbo xa transaction not supported PROPAGATION_NOT_SUPPORTED.");
			}
			if(info.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
				throw new NestedTransactionNotSupportedException(
						"dubbo xa transaction not supported PROPAGATION_NESTED.");
			}
			if(info.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW){
				throw new NestedTransactionNotSupportedException(
						"dubbo xa transaction not supported PROPAGATION_REQUIRES_NEW.");
			}
			return true;
		}
		
		return super.isExistingTransaction(transaction);
	}
	
	
}
