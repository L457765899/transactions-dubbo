package com.sxb.lin.atomikos.dubbo;




public class InitiatorXATransactionLocal {

	private final static ThreadLocal<InitiatorXATransactionLocal> CURRENT_LOCAL = new ThreadLocal<InitiatorXATransactionLocal>();
	
	public static InitiatorXATransactionLocal current() {
        return CURRENT_LOCAL.get();
    }
	
	public void suspend(){
		
	}
    
    private InitiatorXATransactionLocal oldXATransactionLocal;
    
    private String tid;
    
    private String invokeAddress;
    
    private String tmAddress;

	public String getInvokeAddress() {
		return invokeAddress;
	}

	public void setInvokeAddress(String invokeAddress) {
		this.invokeAddress = invokeAddress;
	}

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
	
	public void bindToThread(){
		oldXATransactionLocal = CURRENT_LOCAL.get();
		CURRENT_LOCAL.set(this);
    }
	
	public void restoreThreadLocalStatus(){
		CURRENT_LOCAL.set(oldXATransactionLocal);
	}
	
}
