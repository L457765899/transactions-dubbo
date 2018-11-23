package com.sxb.lin.atomikos.dubbo.spring;

public class XAAnnotationInfo {

	private boolean useXA = false;
	
	private boolean noXA = false;
	
	private int propagationBehavior = -1;
	
	private String className;
	
	private String methodName;

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

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
