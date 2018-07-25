package com.sxb.lin.atomikos.dubbo.filter;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;

public class ConsumerXATransactionFilter implements Filter {
	
	public final static String XA_TM_ADDRESS_KEY = "xaTmAddress";
	
	public final static String XA_TID_KEY = "xaTid";

	public Result invoke(Invoker<?> invoker, Invocation invocation)throws RpcException {
		
		if(invoker.getInterface() != DubboTransactionManagerService.class){
			InitiatorXATransactionLocal icurrent = InitiatorXATransactionLocal.current();
			if(icurrent != null){
				RpcContext context = RpcContext.getContext();
				context.setAttachment(XA_TM_ADDRESS_KEY,icurrent.getTmAddress());
				context.setAttachment(XA_TID_KEY, icurrent.getTid());
				return invoker.invoke(invocation);
			}
			
			ParticipantXATransactionLocal pcurrent = ParticipantXATransactionLocal.current();
			if(pcurrent != null){
				RpcContext context = RpcContext.getContext();
				context.setAttachment(XA_TM_ADDRESS_KEY,pcurrent.getTmAddress());
				context.setAttachment(XA_TID_KEY, pcurrent.getTid());
				return invoker.invoke(invocation);
			}
		}
		
		return invoker.invoke(invocation);
	}

}
