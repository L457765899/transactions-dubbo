package com.sxb.lin.atomikos.dubbo.balance;

import java.util.List;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.sxb.lin.atomikos.dubbo.AtomikosDubboException;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;

class StickySelect {
	
	<T> Invoker<T> select(List<Invoker<T>> invokers, Invoker<T> invoker, Invocation invocation){

		for(Invoker<T> ivk : invokers){
			if(ivk.getInterface() == DubboTransactionManagerService.class){
				
				String remoteAddress = (String) invocation.getArguments()[0];
				Invoker<T> selectInvoker = null;
				
				if(ivk.getUrl().getAddress().equals(remoteAddress)){
					selectInvoker = ivk;
				}
				
				if(selectInvoker != null){
					return selectInvoker;
				}else{
					throw new AtomikosDubboException("can not find invoker,use remote address" + remoteAddress);
				}
			}
		}
		
		return invoker;
	}
}
