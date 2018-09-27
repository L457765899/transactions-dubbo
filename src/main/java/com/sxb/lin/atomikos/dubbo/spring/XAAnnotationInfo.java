package com.sxb.lin.atomikos.dubbo.spring;

public class XAAnnotationInfo {

	private boolean useXA = false;
	
	private boolean noXA = false;

	public boolean isUseXA() {
		return useXA;
	}

	public void setUseXA(boolean useXA) {
		this.useXA = useXA;
	}

	public boolean isNoXA() {
		return noXA;
	}

	public void setNoXA(boolean noXA) {
		this.noXA = noXA;
	}
}
