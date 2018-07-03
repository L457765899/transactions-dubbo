package com.sxb.lin.atomikos.dubbo;

public class LocalConfig {

	private static Integer protocolPort = null;

	public static Integer getProtocolPort() {
		return protocolPort;
	}

	public static void setProtocolPort(Integer protocolPort) {
		LocalConfig.protocolPort = protocolPort;
	}
	
}
