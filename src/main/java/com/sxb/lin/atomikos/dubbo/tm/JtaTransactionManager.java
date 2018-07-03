package com.sxb.lin.atomikos.dubbo.tm;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.jta.JtaTransactionObject;
import org.springframework.util.StringUtils;

import com.alibaba.dubbo.rpc.RpcContext;
import com.atomikos.icatch.CompositeTransaction;
import com.atomikos.icatch.CompositeTransactionManager;
import com.atomikos.icatch.config.Configuration;
import com.sxb.lin.atomikos.dubbo.LocalConfig;
import com.sxb.lin.atomikos.dubbo.XATransactionLocal;
import com.sxb.lin.atomikos.dubbo.filter.XATransactionFilter;


public class JtaTransactionManager extends org.springframework.transaction.jta.JtaTransactionManager{

	private static final long serialVersionUID = 1L;

	@Override
	protected void doJtaBegin(JtaTransactionObject txObject, TransactionDefinition definition) 
			throws NotSupportedException,SystemException {
		
		if(LocalConfig.getProtocolPort() == null){
			throw new NotSupportedException("@LocalConfig must be set protocol port.");
		}
		
		RpcContext context = RpcContext.getContext();
		String tmAddress = context.getAttachment(XATransactionFilter.XA_TM_ADDRESS_KEY);
		String tid = context.getAttachment(XATransactionFilter.XA_TID_KEY);
		if(StringUtils.isEmpty(tmAddress) && StringUtils.isEmpty(tid)){
			super.doJtaBegin(txObject, definition);
			
			CompositeTransactionManager compositeTransactionManager = Configuration.getCompositeTransactionManager();
			CompositeTransaction compositeTransaction = compositeTransactionManager.getCompositeTransaction();
			String localHost = context.getLocalHost();
			tid = compositeTransaction.getTid();
			
			XATransactionLocal local = new XATransactionLocal();
			local.setTid(tid);
			local.setTmAddress(localHost + ":" + LocalConfig.getProtocolPort());
			local.bindToThread();
		}else{
			if(definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED){
				throw new NotSupportedException("dubbo xa transaction not supported PROPAGATION_NESTED.");
			}
			//调用发起者tm
		}
		
	}

}
