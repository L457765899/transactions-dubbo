package com.sxb.lin.atomikos.dubbo.balance;

import java.util.List;

import org.springframework.util.StringUtils;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;

class StickySelect {
	
	<T> Invoker<T> select(List<Invoker<T>> invokers, Invoker<T> invoker, Invocation invocation){

		if(invoker.getInterface() == DubboTransactionManagerService.class){
			String remoteAddress = (String) invocation.getArguments()[0];
			if(StringUtils.hasLength(remoteAddress)){
				Invoker<T> doSelect = this.doSelect(invokers, remoteAddress);
				if(doSelect == null){
					throw new RpcException("can not find invoker,use remote address" + remoteAddress);
				}
				return doSelect;
			}
		}
		
		return invoker;
		
	}
	
	<T> Invoker<T> doSelect(List<Invoker<T>> invokers,String remoteAddress){
		for(Invoker<T> invoker : invokers){
			if(invoker.getInterface() == DubboTransactionManagerService.class){
				if(invoker.getUrl().getAddress().equals(remoteAddress)){
					return invoker;
				}
			}
		}
		return null;
	}
}
