package com.sxb.lin.atomikos.dubbo.filter;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.sxb.lin.atomikos.dubbo.XATransactionLocal;

public class XATransactionFilter implements Filter {
	
	public final static String XA_TM_ADDRESS_KEY = "xaTmAddress";
	
	public final static String XA_TID_KEY = "xaTid";

	public Result invoke(Invoker<?> invoker, Invocation invocation)throws RpcException {
		
		XATransactionLocal current = XATransactionLocal.current();
		if(current != null){
			RpcContext context = RpcContext.getContext();
			context.setAttachment(XA_TM_ADDRESS_KEY,current.getTmAddress());
			context.setAttachment(XA_TID_KEY, current.getTid());
		}
		
		return invoker.invoke(invocation);
	}

}
