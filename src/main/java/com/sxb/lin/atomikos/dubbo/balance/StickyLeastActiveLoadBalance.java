package com.sxb.lin.atomikos.dubbo.balance;

import java.util.List;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.loadbalance.LeastActiveLoadBalance;

public class StickyLeastActiveLoadBalance extends LeastActiveLoadBalance{

	private StickySelect stickySelect = new StickySelect();

	@Override
	public <T> Invoker<T> select(List<Invoker<T>> invokers, URL url, Invocation invocation) {
		return stickySelect.select(invokers, super.select(invokers, url, invocation), invocation);
	}
}
