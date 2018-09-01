package com.sxb.lin.atomikos.dubbo;

public class ParticipantXATransactionLocal {

	private final static ThreadLocal<ParticipantXATransactionLocal> CURRENT_LOCAL = new ThreadLocal<ParticipantXATransactionLocal>();
	
	public static ParticipantXATransactionLocal current() {
        return CURRENT_LOCAL.get();
    }
	
	public static boolean isUseParticipantXATransaction(){
		ParticipantXATransactionLocal current = current();
		if(current != null && current.isActive()){
			return true;
		}else{
			return false;
		}
	}
	
	private ParticipantXATransactionLocal oldXATransactionLocal;
	
	private Boolean isActive;
	
	private String tid;
    
    private String tmAddress;
    
    private String timeOut;

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
	
	public String getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(String timeOut) {
		this.timeOut = timeOut;
	}
	
	public Boolean getIsActive() {
		return isActive;
	}

	public void active() {
		if(this.isActive == null){
			this.isActive = true;
		}
	}

	public void bindToThread(){
		oldXATransactionLocal = CURRENT_LOCAL.get();
		CURRENT_LOCAL.set(this);
    }
	
	public void restoreThreadLocalStatus(){
		CURRENT_LOCAL.set(oldXATransactionLocal);
	}
	
	public boolean isActive(){
		if(this.isActive == null){
			this.isActive = false;
		}
		return isActive;
	}
}
