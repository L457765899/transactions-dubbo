package com.sxb.lin.atomikos.dubbo.filter;

import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;

public class XATransactionFilter implements Filter {

	public Result invoke(Invoker<?> invoker, Invocation invocation)throws RpcException {
		// TODO Auto-generated method stub
		return null;
	}

}
