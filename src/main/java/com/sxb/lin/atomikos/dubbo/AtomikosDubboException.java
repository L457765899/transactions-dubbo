package com.sxb.lin.atomikos.dubbo;

public class AtomikosDubboException extends Exception{

	
	private static final long serialVersionUID = 1L;

	public AtomikosDubboException() {
		super();
	}

	public AtomikosDubboException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public AtomikosDubboException(String message, Throwable cause) {
		super(message, cause);
	}

	public AtomikosDubboException(String message) {
		super(message);
	}

	public AtomikosDubboException(Throwable cause) {
		super(cause);
	}

}
