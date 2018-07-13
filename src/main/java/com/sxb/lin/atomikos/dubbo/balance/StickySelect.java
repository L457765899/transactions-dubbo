package com.sxb.lin.atomikos.dubbo.balance;

import java.util.Arrays;
import java.util.List;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.sxb.lin.atomikos.dubbo.AtomikosDubboException;
import com.sxb.lin.atomikos.dubbo.InitiatorXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;
import com.sxb.lin.atomikos.dubbo.service.DubboTransactionManagerService;

class StickySelect {
	
	private final static List<String> XA_METHODS = Arrays.asList(new String[] {"prepare","commit","rollback","recover"});

	<T> Invoker<T> select(List<Invoker<T>> invokers, Invoker<T> invoker, Invocation invocation){
		
		InitiatorXATransactionLocal initiatorCurrent = InitiatorXATransactionLocal.current();
		ParticipantXATransactionLocal participantCurrent = ParticipantXATransactionLocal.current();
		if(initiatorCurrent != null && participantCurrent != null){
			throw new AtomikosDubboException("a thread cannot be both an initiator and a participant.");
		}
		
		if(initiatorCurrent != null){
			for(Invoker<T> ivk : invokers){
				if(ivk.getInterface() == DubboTransactionManagerService.class
						&& XA_METHODS.contains(invocation.getMethodName())){
					
					Invoker<T> participantInvoker = null;
					String remoteAddress = (String) invocation.getArguments()[0];
					if(ivk.getUrl().getAddress().equals(remoteAddress)){
						participantInvoker = ivk;
					}
					
					if(participantInvoker != null){
						return participantInvoker;
					}else{
						throw new AtomikosDubboException("initiator current can not select participant invoker.");
					}
				}
			}
		}
		
		if(participantCurrent != null){
			for(Invoker<T> ivk : invokers){
				if(ivk.getInterface() == DubboTransactionManagerService.class
						&& invocation.getMethodName().equals("enlistResource")){
					
					Invoker<T> initiatorInvoker = null;
					String remoteAddress = (String) invocation.getArguments()[0];
					if(ivk.getUrl().getAddress().equals(remoteAddress)){
						initiatorInvoker = ivk;
					}
					
					if(initiatorInvoker != null){
						return initiatorInvoker;
					}else{
						throw new AtomikosDubboException("participant current can not select initiator invoker.");
					}
				}
			}
		}
		
		return invoker;
	}
}
