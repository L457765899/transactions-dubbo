package com.sxb.lin.atomikos.dubbo.filter;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.LocalConfig;

public class ConsumerXATransactionFilter implements Filter {

	public Result invoke(Invoker<?> invoker, Invocation invocation)throws RpcException {
		
		InitiatorXATransactionLocal current = InitiatorXATransactionLocal.current();
		if(current != null){
			RpcContext context = RpcContext.getContext();
			context.setAttachment(LocalConfig.XA_TM_ADDRESS_KEY,current.getTmAddress());
			context.setAttachment(LocalConfig.XA_TID_KEY, current.getTid());
		}
		
		return invoker.invoke(invocation);
	}

}
