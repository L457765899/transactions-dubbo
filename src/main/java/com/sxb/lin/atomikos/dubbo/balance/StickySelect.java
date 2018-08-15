package com.sxb.lin.atomikos.dubbo.balance;

import java.util.Arrays;
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
				
				if(invoker.getUrl().getAddress().equals(remoteAddress)){
					return invoker;
				}
				
				Invoker<T> doSelect = this.doSelectByRemoteAddress(invokers, remoteAddress);
				if(doSelect == null){
					throw new RpcException("can not find invoker,use remote address " + remoteAddress);
				}
				return doSelect;
				
			} else {
				if(invocation.getArguments().length > 1){
					
					String uniqueResourceName = (String) invocation.getArguments()[1];
					String uniqueResourceNames = invoker.getUrl().getParameter("uniqueResourceNames");
					if(uniqueResourceNames != null && 
							Arrays.asList(uniqueResourceNames.split(",")).contains(uniqueResourceName)){
						return invoker;
					}
					
					Invoker<T> doSelect = this.doSelectByUniqueResourceName(invokers, uniqueResourceName);
					if(doSelect == null){
						throw new RpcException("can not find invoker,use unique resource name " + uniqueResourceName);
					}
					return doSelect;
					
				}
			}
		}
		
		return invoker;
		
	}
	
	<T> Invoker<T> doSelectByRemoteAddress(List<Invoker<T>> invokers,String remoteAddress){
		for(Invoker<T> invoker : invokers){
			if(invoker.getInterface() == DubboTransactionManagerService.class){
				if(invoker.getUrl().getAddress().equals(remoteAddress)){
					return invoker;
				}
			}
		}
		return null;
	}
	
	<T> Invoker<T> doSelectByUniqueResourceName(List<Invoker<T>> invokers,String uniqueResourceName){
		for(Invoker<T> invoker : invokers){
			if(invoker.getInterface() == DubboTransactionManagerService.class){
				String uniqueResourceNames = invoker.getUrl().getParameter("uniqueResourceNames");
				if(uniqueResourceNames != null && 
						Arrays.asList(uniqueResourceNames.split(",")).contains(uniqueResourceName)){
					return invoker;
				}
			}
		}
		return null;
	}
}
