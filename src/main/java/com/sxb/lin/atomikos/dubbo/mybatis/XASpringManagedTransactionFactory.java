package com.sxb.lin.atomikos.dubbo.mybatis;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.util.StringUtils;

public class XASpringManagedTransactionFactory extends SpringManagedTransactionFactory{

	private String dubboUniqueResourceName;
	
	public XASpringManagedTransactionFactory(String dubboUniqueResourceName) {
		super();
		if(!StringUtils.hasLength(dubboUniqueResourceName)){
			throw new CannotCreateTransactionException("dubboUniqueResourceName not allowed to be empty.");
		}
		this.dubboUniqueResourceName = dubboUniqueResourceName;
	}

	@Override
	public Transaction newTransaction(DataSource dataSource,TransactionIsolationLevel level, boolean autoCommit) {
		XASpringManagedTransaction xaSpringManagedTransaction = new XASpringManagedTransaction(dataSource);
		xaSpringManagedTransaction.setDubboUniqueResourceName(dubboUniqueResourceName);
		return xaSpringManagedTransaction;
	}

	
}
