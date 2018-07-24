package com.sxb.lin.atomikos.dubbo.service;

import java.io.Serializable;

import javax.transaction.xa.Xid;

public class DubboXid implements Xid,Serializable{
	
	private static final long serialVersionUID = 1L;

	private int formatId;
	
    private byte[] branchQualifier;
    
    private byte[] globalTransactionId;
    
    private String branchQualifierStr;
    
    private String globalTransactionIdStr;
	
	public int getFormatId() {
		return formatId;
	}

	public void setFormatId(int formatId) {
		this.formatId = formatId;
	}

	public byte[] getBranchQualifier() {
		return branchQualifier;
	}

	public void setBranchQualifier(byte[] branchQualifier) {
		this.branchQualifier = branchQualifier;
	}

	public byte[] getGlobalTransactionId() {
		return globalTransactionId;
	}

	public void setGlobalTransactionId(byte[] globalTransactionId) {
		this.globalTransactionId = globalTransactionId;
	}

	public String getBranchQualifierStr() {
		return branchQualifierStr;
	}

	public void setBranchQualifierStr(String branchQualifierStr) {
		this.branchQualifierStr = branchQualifierStr;
	}

	public String getGlobalTransactionIdStr() {
		return globalTransactionIdStr;
	}

	public void setGlobalTransactionIdStr(String globalTransactionIdStr) {
		this.globalTransactionIdStr = globalTransactionIdStr;
	}

	@Override
	public boolean equals (Object obj){
    	if (this == obj)
			return true;
		if (obj instanceof DubboXid) {
			DubboXid xid = (DubboXid) obj;
			return xid.getBranchQualifierStr().equals(getBranchQualifierStr()) 
					&& xid.getGlobalTransactionIdStr().equals(getGlobalTransactionIdStr());
		}
		return false;
    }
	
	@Override
	public String toString() {
        return getBranchQualifierStr() + getGlobalTransactionIdStr();
    }

	@Override
	public int hashCode() {
        return toString ().hashCode ();
    }
}
