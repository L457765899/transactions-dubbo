package com.sxb.lin.atomikos.dubbo.balance;

import java.util.List;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.RpcException;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;

class StickySelect {
	
	<T> Invoker<T> select(List<Invoker<T>> invokers, Invoker<T> invoker, Invocation invocation){

		for(int i = 0,len = invokers.size();i < len;i++){
			Invoker<T> ivk = invokers.get(i);
			if(ivk.getInterface() == DubboTransactionManagerService.class){
				
				String remoteAddress = (String) invocation.getArguments()[0];
				Invoker<T> selectInvoker = null;
				
				if(ivk.getUrl().getAddress().equals(remoteAddress)){
					selectInvoker = ivk;
				}
				
				if(selectInvoker != null){
					return selectInvoker;
				}else{
					if(i == len - 1){
						throw new RpcException("can not find invoker,use remote address" + remoteAddress);
					}
				}
			}
		}
		
		return invoker;
	}
}
