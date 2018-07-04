package com.sxb.lin.atomikos.dubbo;

public class ParticipantXATransactionLocal {

	private final static ThreadLocal<ParticipantXATransactionLocal> CURRENT_LOCAL = new ThreadLocal<ParticipantXATransactionLocal>();
	
	public static ParticipantXATransactionLocal current() {
        return CURRENT_LOCAL.get();
    }
	
	private ParticipantXATransactionLocal oldXATransactionLocal;
	
	private String tid;
    
    private String tmAddress;

	public String getTid() {
		return tid;
	}

	public void setTid(String tid) {
		this.tid = tid;
	}

	public String getTmAddress() {
		return tmAddress;
	}

	public void setTmAddress(String tmAddress) {
		this.tmAddress = tmAddress;
	}
    
	public void bindToThread(){
		oldXATransactionLocal = CURRENT_LOCAL.get();
		CURRENT_LOCAL.set(this);
    }
	
	public void restoreThreadLocalStatus(){
		CURRENT_LOCAL.set(oldXATransactionLocal);
	}
}
