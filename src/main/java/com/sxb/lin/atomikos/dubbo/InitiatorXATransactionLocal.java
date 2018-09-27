package com.sxb.lin.atomikos.dubbo;

public class InitiatorXATransactionLocal {

	private final static ThreadLocal<InitiatorXATransactionLocal> CURRENT_LOCAL = new ThreadLocal<InitiatorXATransactionLocal>();
	
	public static InitiatorXATransactionLocal current() {
        return CURRENT_LOCAL.get();
    }
    
    private InitiatorXATransactionLocal oldXATransactionLocal;
    
    private String tid;
    
    private String tmAddress;
    
    private String timeOut;
    
    private boolean isReadOnly;

	public String getTmAddress() {
		return tmAddress;
	}

	public void setTmAddress(String tmAddress) {
		this.tmAddress = tmAddress;
	}

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}
	
	public String getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(String timeOut) {
		this.timeOut = timeOut;
	}
	
	public void bindToThread(){
		oldXATransactionLocal = CURRENT_LOCAL.get();
		CURRENT_LOCAL.set(this);
    }
	
	public void restoreThreadLocalStatus(){
		CURRENT_LOCAL.set(oldXATransactionLocal);
	}

	public boolean isReadOnly() {
		return isReadOnly;
	}

	public void setReadOnly(boolean isReadOnly) {
		this.isReadOnly = isReadOnly;
	}
	
}
