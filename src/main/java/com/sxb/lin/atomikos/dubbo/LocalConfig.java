package com.sxb.lin.atomikos.dubbo;

public class LocalConfig {
	
	public final static String XA_TM_ADDRESS_KEY = "xaTmAddress";
	
	public final static String XA_TID_KEY = "xaTid";

	private static Integer protocolPort = null;

	public static Integer getProtocolPort() {
		return protocolPort;
	}

	public static void setProtocolPort(Integer protocolPort) {
		LocalConfig.protocolPort = protocolPort;
	}
	
}
