package com.sxb.lin.atomikos.dubbo.dispatcher;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.alibaba.dubbo.remoting.Dispatcher;

public class AllDispatcher implements Dispatcher{

	public ChannelHandler dispatch(ChannelHandler handler, URL url) {
		return new AllChannelHandler(handler, url);
	}

}
