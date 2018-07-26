package com.sxb.lin.atomikos.dubbo.dispatcher;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.dubbo.remoting.Channel;
import com.alibaba.dubbo.remoting.ChannelHandler;
import com.sxb.lin.atomikos.dubbo.pool.XAResourceHolder;
import com.sxb.lin.atomikos.dubbo.pool.XAResourcePool;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerServiceProxy;

public class ChannelEventRunnable extends com.alibaba.dubbo.remoting.transport.dispatcher.ChannelEventRunnable{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ChannelEventRunnable.class);
	
	private final ChannelState state;
	
	private final Channel channel;

	public ChannelEventRunnable(Channel channel, ChannelHandler handler,ChannelState state) {
		super(channel, handler, state);
		this.state = state;
		this.channel = channel;
	}

	@Override
	public void run() {
		super.run();
		if(state == ChannelState.DISCONNECTED){
			DubboTransactionManagerServiceProxy instance = DubboTransactionManagerServiceProxy.getInstance();
			String tmAddress = channel.getRemoteAddress().toString();
			XAResourcePool xaResourcePool = instance.getXaResourcePool();
			List<XAResourceHolder> list = xaResourcePool.getDisconnectedHolderByTmAddress(tmAddress);
			if(list.size() == 0){
				return;
			}
			if(instance.ping(tmAddress) == 1){
				for(XAResourceHolder xaResourceHolder : list){
					LOGGER.error("disconected from " + tmAddress + ",so close " + xaResourceHolder.getUuid() + ",it from "+tmAddress);
					xaResourcePool.removeXAResourceHolder(xaResourceHolder);
					xaResourceHolder.close();
				}
			}
		}
	}

}
