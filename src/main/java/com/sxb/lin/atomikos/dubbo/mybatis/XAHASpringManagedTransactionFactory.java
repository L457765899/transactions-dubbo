package com.sxb.lin.atomikos.dubbo.mybatis;

import javax.sql.DataSource;

import org.apache.ibatis.session.TransactionIsolationLevel;
import org.apache.ibatis.transaction.Transaction;

public class XAHASpringManagedTransactionFactory extends XASpringManagedTransactionFactory{
	
	private String dubboUniqueResourceName;

	public XAHASpringManagedTransactionFactory(String dubboUniqueResourceName) {
		super(dubboUniqueResourceName);
		this.dubboUniqueResourceName = dubboUniqueResourceName;
	}

	@Override
	public Transaction newTransaction(DataSource dataSource,TransactionIsolationLevel level, boolean autoCommit) {
		XAHASpringManagedTransaction transaction = new XAHASpringManagedTransaction(dataSource);
		transaction.setDubboUniqueResourceName(dubboUniqueResourceName);
		return transaction;
	}
}
