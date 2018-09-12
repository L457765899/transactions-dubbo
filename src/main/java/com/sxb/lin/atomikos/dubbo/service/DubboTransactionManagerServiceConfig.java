package com.sxb.lin.atomikos.dubbo.service;

import java.util.Map;
import java.util.Set;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.sxb.lin.atomikos.dubbo.pool.recover.UniqueResource;

public class DubboTransactionManagerServiceConfig {

	private ApplicationConfig applicationConfig;
	
	private RegistryConfig registryConfig;
	
	private ProtocolConfig protocolConfig;
	
	private ProviderConfig providerConfig;
	
	private ConsumerConfig consumerConfig;
	
	private Map<String,UniqueResource> uniqueResourceMapping;
	
	private Set<String> excludeResourceNames;
	
	private String serviceDispatcher;
	
	private String serviceLoadbalance;

	public ApplicationConfig getApplicationConfig() {
		return applicationConfig;
	}

	public void setApplicationConfig(ApplicationConfig applicationConfig) {
		this.applicationConfig = applicationConfig;
	}

	public RegistryConfig getRegistryConfig() {
		return registryConfig;
	}

	public void setRegistryConfig(RegistryConfig registryConfig) {
		this.registryConfig = registryConfig;
	}

	public ProtocolConfig getProtocolConfig() {
		return protocolConfig;
	}

	public void setProtocolConfig(ProtocolConfig protocolConfig) {
		this.protocolConfig = protocolConfig;
	}

	public ProviderConfig getProviderConfig() {
		return providerConfig;
	}

	public void setProviderConfig(ProviderConfig providerConfig) {
		this.providerConfig = providerConfig;
	}

	public ConsumerConfig getConsumerConfig() {
		return consumerConfig;
	}

	public void setConsumerConfig(ConsumerConfig consumerConfig) {
		this.consumerConfig = consumerConfig;
	}

	public Map<String, UniqueResource> getUniqueResourceMapping() {
		return uniqueResourceMapping;
	}

	public void setUniqueResourceMapping(
			Map<String, UniqueResource> uniqueResourceMapping) {
		this.uniqueResourceMapping = uniqueResourceMapping;
	}

	public Set<String> getExcludeResourceNames() {
		return excludeResourceNames;
	}

	public void setExcludeResourceNames(Set<String> excludeResourceNames) {
		this.excludeResourceNames = excludeResourceNames;
	}

	public String getServiceDispatcher() {
		return serviceDispatcher;
	}

	public void setServiceDispatcher(String serviceDispatcher) {
		this.serviceDispatcher = serviceDispatcher;
	}

	public String getServiceLoadbalance() {
		return serviceLoadbalance;
	}

	public void setServiceLoadbalance(String serviceLoadbalance) {
		this.serviceLoadbalance = serviceLoadbalance;
	}
	
}
