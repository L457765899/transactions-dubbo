package com.sxb.lin.atomikos.dubbo.filter;

import org.springframework.util.StringUtils;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;

public class ProviderXATransactionFilter implements Filter {

	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		
		RpcContext context = RpcContext.getContext();
		String tmAddress = context.getAttachment(ConsumerXATransactionFilter.XA_TM_ADDRESS_KEY);
		String tid = context.getAttachment(ConsumerXATransactionFilter.XA_TID_KEY);
		ParticipantXATransactionLocal local = null;
		if(StringUtils.hasLength(tmAddress) && StringUtils.hasLength(tid)){
			local = new ParticipantXATransactionLocal();
			local.setTmAddress(tmAddress);
			local.setTid(tid);
			local.bindToThread();
		}
		
		Result result = invoker.invoke(invocation);
		if(local != null){
			local.restoreThreadLocalStatus();
		}
		
		return result;
	}

}
