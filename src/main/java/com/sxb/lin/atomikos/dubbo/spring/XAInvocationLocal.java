package com.sxb.lin.atomikos.dubbo.spring;

import java.lang.reflect.Method;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.lin.atomikos.dubbo.annotation.NOXA;
import com.sxb.lin.atomikos.dubbo.annotation.XA;

public class XAInvocationLocal {
	
	private final static Logger LOGGER = LoggerFactory.getLogger(XAInvocationLocal.class);
	
	private final static ThreadLocal<XAInvocationLocal> CURRENT_LOCAL = new ThreadLocal<XAInvocationLocal>();
	
	public static XAInvocationLocal current() {
        return CURRENT_LOCAL.get();
    }
	
	public static XAAnnotationInfo info(){
		XAInvocationLocal current = XAInvocationLocal.current();
		XAAnnotationInfo info = new XAAnnotationInfo();
		if(current != null){
			info.setPropagationBehavior(current.getPropagationBehavior());
			Method method = current.getMethod();
			try {
				Method methodImpl = current.getTargetClass().getMethod(method.getName(), method.getParameterTypes());
				XA xaAnnotation = methodImpl.getAnnotation(XA.class);
				NOXA noxaAnnotation = methodImpl.getAnnotation(NOXA.class);
				current.clear();
				if(xaAnnotation != null){
					info.setUseXA(true);
				}
				if(noxaAnnotation != null){
					info.setNoXA(true);
				}
			} catch (NoSuchMethodException e) {
				LOGGER.error(e.getMessage(),e);
			} catch (SecurityException e) {
				LOGGER.error(e.getMessage(),e);
			}
		}
		return info;
	}

	private Method method;
	
	private Class<?> targetClass;
	
	private int propagationBehavior = -1;

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

	public int getPropagationBehavior() {
		return propagationBehavior;
	}

	public void setPropagationBehavior(int propagationBehavior) {
		this.propagationBehavior = propagationBehavior;
	}
}
