package com.sxb.lin.atomikos.dubbo.spring;

public class XAAnnotationInfo {

	private boolean useXA = false;
	
	private boolean noXA = false;
	
	private int propagationBehavior = -1;

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

	public int getPropagationBehavior() {
		return propagationBehavior;
	}

	public void setPropagationBehavior(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}
}
