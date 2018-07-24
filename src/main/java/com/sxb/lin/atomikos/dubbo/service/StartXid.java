package com.sxb.lin.atomikos.dubbo.service;

import java.io.Serializable;

import javax.transaction.xa.Xid;

public class StartXid implements Serializable{

	private static final long serialVersionUID = 1L;
	
	private Xid xid;
	
	private int flags;
	
	private long startTime;
	
	private long timeout;
	
	private String tmAddress;

	public int getFlags() {
		return flags;
	}

	public void setFlags(int flags) {
		this.flags = flags;
	}

	public long getStartTime() {
		return startTime;
	}

	public void setStartTime(long startTime) {
		this.startTime = startTime;
	}

	public long getTimeout() {
		return timeout;
	}

	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	public String getTmAddress() {
		return tmAddress;
	}

	public void setTmAddress(String tmAddress) {
		this.tmAddress = tmAddress;
	}

	public Xid getXid() {
		return xid;
	}

	public void setXid(Xid xid) {
		this.xid = xid;
	}

}
