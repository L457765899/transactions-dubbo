package com.sxb.lin.atomikos.dubbo.service;

import java.util.HashMap;
import java.util.Map;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.alibaba.dubbo.config.ProtocolConfig;
import com.alibaba.dubbo.config.ReferenceConfig;
import com.alibaba.dubbo.config.ServiceConfig;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.recovery.CoordinatorLogEntry;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.ParticipantLogEntry;
import com.atomikos.recovery.Repository;
import com.atomikos.recovery.TxState;
import com.sxb.lin.atomikos.dubbo.AtomikosDubboException;
import com.sxb.lin.atomikos.dubbo.DubboXATransactionalResource;
import com.sxb.lin.atomikos.dubbo.pool.XAResourcePool;
import com.sxb.lin.atomikos.dubbo.pool.recover.UniqueResource;
import com.sxb.lin.atomikos.dubbo.rocketmq.MQProducerFor2PC;

public class DubboTransactionManagerServiceProxy implements DubboTransactionManagerService{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(DubboTransactionManagerServiceProxy.class);
	
	private final static DubboTransactionManagerServiceProxy INSTANCE = new DubboTransactionManagerServiceProxy();
	
	public static DubboTransactionManagerServiceProxy getInstance(){
		return INSTANCE;
	}
	
	private ReferenceConfig<DubboTransactionManagerService> referenceConfig;
	
	private ServiceConfig<DubboTransactionManagerService> serviceConfig;

	private DubboTransactionManagerService remoteDubboTransactionManagerService;
	
	private DubboTransactionManagerService localDubboTransactionManagerService;
	
	private XAResourcePool xaResourcePool;
	
	private DubboXATransactionalResource dubboXATransactionalResource;
	
	private boolean inited = false;
	
	private String localAddress;
	
	private String uniqueResourceNames;
	
	private DubboTransactionManagerServiceProxy() {
		
	}
	
	public void init(DubboTransactionManagerServiceConfig config){
		if(inited){
			return;
		}
		dubboXATransactionalResource = new DubboXATransactionalResource(config.getExcludeResourceNames());
		this.export(config);
		this.reference(config);
		inited = true;
		Configuration.addResource(dubboXATransactionalResource);
	}
	
	private void export(DubboTransactionManagerServiceConfig config){
		Map<String, UniqueResource> uniqueResourceMapping = config.getUniqueResourceMapping();
		this.uniqueResourceNames = StringUtils.collectionToCommaDelimitedString(uniqueResourceMapping.keySet());
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put("uniqueResourceNames", uniqueResourceNames);
		
		xaResourcePool = new XAResourcePool(uniqueResourceMapping);
		DubboTransactionManagerServiceImpl dubboTransactionManagerService = 
				new DubboTransactionManagerServiceImpl(xaResourcePool,dubboXATransactionalResource);
		
		ProtocolConfig protocolConfig = config.getProtocolConfig();
		ProtocolConfig xaProtocolConfig = null;
		if(StringUtils.hasLength(config.getServiceDispatcher())){
			xaProtocolConfig = new ProtocolConfig();
			xaProtocolConfig.setPort(protocolConfig.getPort());
			xaProtocolConfig.setName(protocolConfig.getName());
			xaProtocolConfig.setDispatcher(config.getServiceDispatcher());
		}else{
			if(protocolConfig.getDispatcher() != null && protocolConfig.getDispatcher().equals("xa_all")){
				xaProtocolConfig = protocolConfig;
			}else{
				xaProtocolConfig = new ProtocolConfig();
				xaProtocolConfig.setPort(protocolConfig.getPort());
				xaProtocolConfig.setName(protocolConfig.getName());
				xaProtocolConfig.setDispatcher("xa_all");
			}
		}
		
		ServiceConfig<DubboTransactionManagerService> serviceConfig = new ServiceConfig<DubboTransactionManagerService>();
		serviceConfig.setApplication(config.getApplicationConfig());
        serviceConfig.setRegistry(config.getRegistryConfig());
        serviceConfig.setProtocol(xaProtocolConfig);
        serviceConfig.setProvider(config.getProviderConfig());
        serviceConfig.setInterface(DubboTransactionManagerService.class);
        serviceConfig.setRef(dubboTransactionManagerService);
        serviceConfig.setParameters(parameters);
        if(StringUtils.hasLength(config.getServiceLoadbalance())){
        	serviceConfig.setLoadbalance(config.getServiceLoadbalance());
        }else{
        	String loadbalance = config.getProviderConfig().getLoadbalance();
        	if(loadbalance == null || !loadbalance.equals("sticky_roundrobin")){
        		serviceConfig.setLoadbalance("sticky_roundrobin");
        	}
        }
        serviceConfig.export();
        this.localAddress = serviceConfig.toUrl().getAddress();
        dubboTransactionManagerService.setLocalAddress(this.localAddress);
        this.localDubboTransactionManagerService = dubboTransactionManagerService;
        this.serviceConfig = serviceConfig;
	}
	
	private void reference(DubboTransactionManagerServiceConfig config){
		ReferenceConfig<DubboTransactionManagerService> referenceConfig = new ReferenceConfig<DubboTransactionManagerService>();
		referenceConfig.setApplication(config.getApplicationConfig());
		referenceConfig.setRegistry(config.getRegistryConfig());
		referenceConfig.setConsumer(config.getConsumerConfig());
		referenceConfig.setInterface(DubboTransactionManagerService.class);
		if(StringUtils.hasLength(config.getServiceLoadbalance())){
			referenceConfig.setLoadbalance(config.getServiceLoadbalance());
		}else{
			String loadbalance = config.getProviderConfig().getLoadbalance();
        	if(loadbalance == null || !loadbalance.equals("sticky_roundrobin")){
        		referenceConfig.setLoadbalance("sticky_roundrobin");
        	}
		}
		referenceConfig.setScope("remote");
		this.remoteDubboTransactionManagerService = referenceConfig.get();
		this.referenceConfig = referenceConfig;
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
	
	public void destory() {
		if(this.inited){
			this.serviceConfig.unexport();
			this.referenceConfig.destroy();
			this.xaResourcePool.destory();
			
			this.serviceConfig = null;
			this.referenceConfig = null;
			this.remoteDubboTransactionManagerService = null;
			this.localDubboTransactionManagerService = null;
			this.xaResourcePool = null;
			this.dubboXATransactionalResource = null;
			this.inited = false;
			this.localAddress = null;
			this.uniqueResourceNames = null;
		}
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
	
	public DubboXATransactionalResource getDubboXATransactionalResource() {
		return dubboXATransactionalResource;
	}
	
	public String getUniqueResourceNames() {
		return uniqueResourceNames;
	}
	
	public String getFirstUniqueResourceName(){
		String[] array = uniqueResourceNames.split(",");
		return array[0];
	}

	public StartXid enlistResource(String remoteAddress, String uniqueResourceName, String tid,
			String localAddress) throws SystemException, RollbackException {
		if(this.isLocal(remoteAddress)){
			return this.getLocalDubboTransactionManagerService()
					.enlistResource(remoteAddress, uniqueResourceName, tid, localAddress);
		}else{
			return this.getRemoteDubboTransactionManagerService()
					.enlistResource(remoteAddress, uniqueResourceName, tid, localAddress);
		}
	}

	public int prepare(String remoteAddress, String uniqueResourceName, Xid xid) throws XAException {
		Xid converXid = converXid(xid);
		if(this.isLocal(remoteAddress)){
			return this.getLocalDubboTransactionManagerService().prepare(remoteAddress, uniqueResourceName, converXid);
		}else{
			return this.getRemoteDubboTransactionManagerService().prepare(remoteAddress, uniqueResourceName, converXid);
		}
	}

	public void commit(String remoteAddress, String uniqueResourceName, Xid xid, boolean onePhase)
			throws XAException {
		Xid converXid = converXid(xid);
		if(this.isLocal(remoteAddress)){
			this.getLocalDubboTransactionManagerService().commit(remoteAddress, uniqueResourceName, converXid, onePhase);
		}else{
			this.getRemoteDubboTransactionManagerService().commit(remoteAddress, uniqueResourceName, converXid, onePhase);
		}
	}

	public void rollback(String remoteAddress, String uniqueResourceName, Xid xid) throws XAException {
		Xid converXid = converXid(xid);
		if(this.isLocal(remoteAddress)){
			this.getLocalDubboTransactionManagerService().rollback(remoteAddress, uniqueResourceName, converXid);
		}else{
			this.getRemoteDubboTransactionManagerService().rollback(remoteAddress, uniqueResourceName, converXid);
		}
	}

	public Xid[] recover(String remoteAddress, String uniqueResourceName, int flag) throws XAException {
		if(this.isLocal(remoteAddress)){
			return this.getLocalDubboTransactionManagerService().recover(remoteAddress, uniqueResourceName, flag);
		}else{
			return this.getRemoteDubboTransactionManagerService().recover(remoteAddress, uniqueResourceName, flag);
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
			LOGGER.error(e.getMessage(), e);
			return -1;
		}
	}

	public Boolean wasCommitted(String remoteAddress, String tid, String uri) {
		if(this.isLocal(remoteAddress)){
			return this.getLocalDubboTransactionManagerService().wasCommitted(remoteAddress, tid, uri);
		}else{
			return this.getRemoteDubboTransactionManagerService().wasCommitted(remoteAddress, tid, uri);
		}
	}

	public boolean wasTerminated(String uniqueResourceName, Xid xid){
		if(!uniqueResourceName.startsWith(MQProducerFor2PC.MQ_UNIQUE_TOPIC_PREFIX)){
			return false;
		}
		DubboTransactionManagerServiceImpl impl = (DubboTransactionManagerServiceImpl) this.localDubboTransactionManagerService;
		Repository repository = impl.getRepository();
		if(repository == null){
			return false;
		}
		try {
			String tid = new String(xid.getGlobalTransactionId());
			String uri = new String(xid.getBranchQualifier());
			CoordinatorLogEntry coordinatorLogEntry = repository.get(tid);
			if(coordinatorLogEntry == null){
				return false;
			}
			for(ParticipantLogEntry entry : coordinatorLogEntry.participants){
				if(entry.coordinatorId.equals(tid) && entry.uri.equals(uri) && entry.state == TxState.TERMINATED){
					return true;
				}
				if(entry.coordinatorId.equals(tid) && entry.resourceName.startsWith(MQProducerFor2PC.MQ_UNIQUE_TOPIC_PREFIX)
						&& entry.state == TxState.TERMINATED){
					String commiting = uri + "," + uniqueResourceName;
					String terminated = entry.uri + "," + entry.resourceName;
					LOGGER.warn(commiting + "is not find terminated,but " + terminated + " is terminated.so terminated " + commiting);
					return true;
				}
			}
		} catch (LogReadException e) {
			LOGGER.error(e.getMessage(), e);
		}
		return false;
	}
	
	public boolean isInit(){
		return inited;
	}
}
