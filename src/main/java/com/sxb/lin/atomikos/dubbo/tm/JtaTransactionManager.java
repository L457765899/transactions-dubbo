package com.sxb.lin.atomikos.dubbo.tm;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.transaction.support.DefaultTransactionStatus;

import com.alibaba.dubbo.rpc.RpcContext;
import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.LocalConfig;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;


public class JtaTransactionManager extends org.springframework.transaction.jta.JtaTransactionManager{

	private static final long serialVersionUID = 1L;

	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition) 
			throws NotSupportedException,SystemException {
		
		if(LocalConfig.getProtocolPort() == null){
			throw new CannotCreateTransactionException("@LocalConfig must be set protocol port.");
		}
		
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			super.doJtaBegin(txObject, definition);
			
			RpcContext context = RpcContext.getContext();
			CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
			CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
			String localHost = context.getLocalHost();
			String tid = compositeTransaction.getTid();
			
			InitiatorXATransactionLocal local = new InitiatorXATransactionLocal();
			local.setTid(tid);
			local.setTmAddress(localHost + ":" + LocalConfig.getProtocolPort());
			local.bindToThread();
		}else{
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_NESTED.");
			}
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_REQUIRES_NEW.");
			}
			
			
			//调用发起者tm,xa start
		}
		
	}

	@Override
	protected void doCommit(DefaultTransactionStatus status) {
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			try {
				super.doCommit(status);
			} finally {
				this.restoreThreadLocalStatus();
			}
		}else{
			//xa end
			
			//有可能不会执行xa end,在xa prepare之前先判断是否xa end
		}
	}

	@Override
	protected void doRollback(DefaultTransactionStatus status) {
		ParticipantXATransactionLocal current = ParticipantXATransactionLocal.current();
		if(current == null){
			try {
				super.doRollback(status);
			} finally {
				this.restoreThreadLocalStatus();
			}
		}else{
			//xa end
		}
	}

	private void restoreThreadLocalStatus(){
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			current.restoreThreadLocalStatus();
		}
	}
}
