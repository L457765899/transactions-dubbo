package com.sxb.lin.atomikos.dubbo.filter;

import org.springframework.util.StringUtils;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;

public class ProviderXATransactionFilter implements Filter {

	public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
		
		Result result = null;
		if(invoker.getInterface() != DubboTransactionManagerService.class){
			RpcContext context = RpcContext.getContext();
			String tmAddress = context.getAttachment(ConsumerXATransactionFilter.XA_TM_ADDRESS_KEY);
			String tid = context.getAttachment(ConsumerXATransactionFilter.XA_TID_KEY);
			String timeOut = context.getAttachment(ConsumerXATransactionFilter.XA_TIME_OUT);
			ParticipantXATransactionLocal local = null;
			if(StringUtils.hasLength(tmAddress) && StringUtils.hasLength(tid) && StringUtils.hasLength(timeOut)){
				local = new ParticipantXATransactionLocal();
				local.setTmAddress(tmAddress);
				local.setTid(tid);
				local.setTimeOut(timeOut);
				local.bindToThread();
			}
			
			result = invoker.invoke(invocation);
			
			if(local != null){
				local.restoreThreadLocalStatus();
			}
		}else{
			result = invoker.invoke(invocation);
		}
		
		return result;
	}

}
