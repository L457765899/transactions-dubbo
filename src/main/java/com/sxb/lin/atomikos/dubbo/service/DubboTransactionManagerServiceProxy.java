package com.sxb.lin.atomikos.dubbo.service;

import java.util.Map;

import javax.sql.DataSource;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import com.alibaba.dubbo.config.ApplicationConfig;
import com.alibaba.dubbo.config.ConsumerConfig;
import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ProviderConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.RegistryConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.atomikos.logging.Logger;
import com.atomikos.logging.LoggerFactory;
import com.sxb.lin.atomikos.dubbo.AtomikosDubboException;
import com.sxb.lin.atomikos.dubbo.pool.XAResourcePool;

public class DubboTransactionManagerServiceProxy implements DubboTransactionManagerService{
	
	private static final Logger LOGGER = LoggerFactory.createLogger(DubboTransactionManagerServiceProxy.class);
	
	private final static DubboTransactionManagerServiceProxy INSTANCE = new DubboTransactionManagerServiceProxy();
	
	public static DubboTransactionManagerServiceProxy getInstance(){
		return INSTANCE;
	}

	private DubboTransactionManagerService remoteDubboTransactionManagerService;
	
	private DubboTransactionManagerService localDubboTransactionManagerService;
	
	private XAResourcePool xaResourcePool;
	
	private boolean inited = false;
	
	private String localAddress;
	
	private DubboTransactionManagerServiceProxy() {
		
	}
	
	public void init(ApplicationConfig applicationConfig,RegistryConfig registryConfig,
			ProtocolConfig protocolConfig,ProviderConfig providerConfig,ConsumerConfig consumerConfig,
			Map<String,DataSource> dataSourceMapping){
		if(inited){
			return;
		}
		this.export(applicationConfig, registryConfig, protocolConfig, providerConfig,dataSourceMapping);
		this.reference(applicationConfig, registryConfig, consumerConfig);
		inited = true;
	}
	
	private void export(ApplicationConfig applicationConfig,RegistryConfig registryConfig,
			ProtocolConfig protocolConfig,ProviderConfig providerConfig,Map<String,DataSource> dataSourceMapping){
		xaResourcePool = new XAResourcePool(dataSourceMapping);
		localDubboTransactionManagerService = new DubboTransactionManagerServiceImpl(xaResourcePool);
		ServiceConfig<DubboTransactionManagerService> serviceConfig = new ServiceConfig<DubboTransactionManagerService>();
		serviceConfig.setApplication(applicationConfig);
        serviceConfig.setRegistry(registryConfig);
        serviceConfig.setProtocol(protocolConfig);
        serviceConfig.setProvider(providerConfig);
        serviceConfig.setInterface(DubboTransactionManagerService.class);
        serviceConfig.setRef(localDubboTransactionManagerService);
        serviceConfig.export();
        localAddress = serviceConfig.toUrl().getAddress();
	}
	
	private void reference(ApplicationConfig applicationConfig,RegistryConfig registryConfig,ConsumerConfig consumerConfig){
		ReferenceConfig<DubboTransactionManagerService> referenceConfig = new ReferenceConfig<DubboTransactionManagerService>();
		referenceConfig.setApplication(applicationConfig);
		referenceConfig.setRegistry(registryConfig);
		referenceConfig.setConsumer(consumerConfig);
		referenceConfig.setInterface(DubboTransactionManagerService.class);
		referenceConfig.setScope("remote");
		remoteDubboTransactionManagerService = referenceConfig.get();
	}
	
	private void check(Object service){
		if(service == null){
			throw new AtomikosDubboException("DubboTransactionManagerServiceFactory must be init.");
		}
	}
	
	private boolean isLocal(String remoteAddress){
		return this.getLocalAddress().equals(remoteAddress);
	}
	
	private Xid converXid(Xid xid){
		DubboXid dubboXid = new DubboXid();
		dubboXid.setFormatId(xid.getFormatId());
		dubboXid.setBranchQualifier(xid.getBranchQualifier());
		dubboXid.setGlobalTransactionId(xid.getGlobalTransactionId());
		dubboXid.setBranchQualifierStr(new String(xid.getBranchQualifier()));
		dubboXid.setGlobalTransactionIdStr(new String(xid.getGlobalTransactionId()));
		return dubboXid;
	}

	public DubboTransactionManagerService getRemoteDubboTransactionManagerService() {
		this.check(remoteDubboTransactionManagerService);
		return remoteDubboTransactionManagerService;
	}

	public DubboTransactionManagerService getLocalDubboTransactionManagerService() {
		this.check(localDubboTransactionManagerService);
		return localDubboTransactionManagerService;
	}

	public String getLocalAddress() {
		this.check(localAddress);
		return localAddress;
	}
	
	public XAResourcePool getXaResourcePool() {
		return xaResourcePool;
	}

	public StartXid enlistResource(String remoteAddress, String tid,
			String localAddress, String uniqueResourceName)
			throws SystemException, RollbackException {
		if(this.isLocal(remoteAddress)){
			return this.getLocalDubboTransactionManagerService()
					.enlistResource(remoteAddress, tid, localAddress, uniqueResourceName);
		}else{
			return this.getRemoteDubboTransactionManagerService()
					.enlistResource(remoteAddress, tid, localAddress, uniqueResourceName);
		}
	}

	public int prepare(String remoteAddress, Xid xid, String uniqueResourceName) throws XAException {
		Xid converXid = converXid(xid);
		if(this.isLocal(remoteAddress)){
			return this.getLocalDubboTransactionManagerService().prepare(remoteAddress, converXid, uniqueResourceName);
		}else{
			return this.getRemoteDubboTransactionManagerService().prepare(remoteAddress, converXid, uniqueResourceName);
		}
	}

	public void commit(String remoteAddress, Xid xid, boolean onePhase, String uniqueResourceName)
			throws XAException {
		Xid converXid = converXid(xid);
		if(this.isLocal(remoteAddress)){
			this.getLocalDubboTransactionManagerService().commit(remoteAddress, converXid, onePhase, uniqueResourceName);
		}else{
			this.getRemoteDubboTransactionManagerService().commit(remoteAddress, converXid, onePhase, uniqueResourceName);
		}
	}

	public void rollback(String remoteAddress, Xid xid, String uniqueResourceName) throws XAException {
		Xid converXid = converXid(xid);
		if(this.isLocal(remoteAddress)){
			this.getLocalDubboTransactionManagerService().rollback(remoteAddress, converXid, uniqueResourceName);
		}else{
			this.getRemoteDubboTransactionManagerService().rollback(remoteAddress, converXid, uniqueResourceName);
		}
	}

	public Xid[] recover(String remoteAddress, int flag, String uniqueResourceName) throws XAException {
		if(this.isLocal(remoteAddress)){
			return this.getLocalDubboTransactionManagerService().recover(remoteAddress, flag, uniqueResourceName);
		}else{
			return this.getRemoteDubboTransactionManagerService().recover(remoteAddress, flag, uniqueResourceName);
		}
	}

	public long ping(String remoteAddress) {
		try {
			if(this.isLocal(remoteAddress)){
				return this.getLocalDubboTransactionManagerService().ping(remoteAddress);
			}else{
				return this.getRemoteDubboTransactionManagerService().ping(remoteAddress);
			}
		} catch (Exception e) {
			LOGGER.logError(e.getMessage(), e);
			return -1;
		}
	}
	
}
