package com.sxb.lin.atomikos.dubbo.spring;

import java.lang.reflect.Method;

public class XAInvocationLocal {
	
	private final static ThreadLocal<XAInvocationLocal> CURRENT_LOCAL = new ThreadLocal<XAInvocationLocal>();
	
	public static XAInvocationLocal current() {
        return CURRENT_LOCAL.get();
    }

	private Method method;
	
	private Class<?> targetClass;

	public Method getMethod() {
		return method;
	}

	public void setMethod(Method method) {
		this.method = method;
	}

	public Class<?> getTargetClass() {
		return targetClass;
	}

	public void setTargetClass(Class<?> targetClass) {
		this.targetClass = targetClass;
	}
	
	public void bindToThread(){
		CURRENT_LOCAL.set(this);
	}
	
	public void clear(){
		CURRENT_LOCAL.remove();
	}
}
