package com.sxb.lin.atomikos.dubbo.tm;

public interface TerminatedCommittingTransaction {
	
	void terminated(String tid);
	
	String convertToMysqlXid(String globalTransactionId,String branchQualifier);
}
