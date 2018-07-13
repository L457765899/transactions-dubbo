package com.sxb.lin.atomikos.dubbo.service;

import java.io.Serializable;

import javax.transaction.xa.Xid;

public class StartXid implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private Xid xid;
	
	private int flags;

	public Xid getXid() {
		return xid;
	}

	public void setXid(Xid xid) {
		this.xid = xid;
	}

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

}
