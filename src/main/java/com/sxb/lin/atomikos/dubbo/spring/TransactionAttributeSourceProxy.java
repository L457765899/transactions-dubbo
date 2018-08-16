package com.sxb.lin.atomikos.dubbo.spring;

import java.lang.reflect.Method;

import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.interceptor.TransactionAttribute;
import org.springframework.transaction.interceptor.TransactionAttributeSource;

public class TransactionAttributeSourceProxy implements TransactionAttributeSource{
	
	private TransactionAttributeSource transactionAttributeSource;

	public TransactionAttribute getTransactionAttribute(Method method,Class<?> targetClass) {
		TransactionAttribute transactionAttribute = transactionAttributeSource.getTransactionAttribute(method, targetClass);
		if(transactionAttribute != null){
			int propagationBehavior = transactionAttribute.getPropagationBehavior();
			if(propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRED
				|| propagationBehavior == TransactionDefinition.PROPAGATION_REQUIRES_NEW
				|| propagationBehavior == TransactionDefinition.PROPAGATION_NESTED){
				XAInvocationLocal xaInvocationLocal = new XAInvocationLocal();
				xaInvocationLocal.setMethod(method);
				xaInvocationLocal.setTargetClass(targetClass);
				xaInvocationLocal.bindToThread();
			}
		}
		return transactionAttribute;
	}
	
	public TransactionAttributeSource getTransactionAttributeSource() {
		return transactionAttributeSource;
	}

	public void setTransactionAttributeSource(
			TransactionAttributeSource transactionAttributeSource) {
		this.transactionAttributeSource = transactionAttributeSource;
	}

	
}
