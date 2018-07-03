package com.sxb.lin.atomikos.dubbo.balance;

import java.util.List;

import com.alibaba.dubbo.rpc.Invoker;
import com.sxb.lin.atomikos.dubbo.XATransactionLocal;

class StickySelect {

	<T> Invoker<T> select(List<Invoker<T>> invokers, Invoker<T> invoker){
		
		XATransactionLocal current = XATransactionLocal.current();
		if(current != null){
			if(current.getInvokeAddress() != null){
				for(Invoker<T> ivk : invokers){
					if(ivk.getUrl().getAddress().equals(current.getInvokeAddress())){
						return ivk;
					}
				}
			}
			current.setInvokeAddress(invoker.getUrl().getAddress());
		}
		
		return invoker;
	}
}
