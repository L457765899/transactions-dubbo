package com.sxb.lin.atomikos.dubbo;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.transaction.xa.XAResource;

import com.atomikos.datasource.RecoverableResource;
import com.atomikos.datasource.ResourceException;
import com.atomikos.datasource.TransactionalResource;
import com.atomikos.datasource.xa.XATransactionalResource;
import com.atomikos.icatch.config.Configuration;
import com.atomikos.icatch.provider.TransactionServiceProvider;
import com.atomikos.logging.Logger;
import com.atomikos.logging.LoggerFactory;
import com.atomikos.recovery.LogException;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.ParticipantLogEntry;
import com.atomikos.recovery.RecoveryLog;
import com.atomikos.recovery.xa.XaResourceRecoveryManager;

public class DubboXATransactionalResource extends XATransactionalResource{
	
	private static final Logger LOGGER = LoggerFactory.createLogger(DubboXATransactionalResource.class);

	private Map<String,Long> uniqueResourceNameMap;
	
	private Map<String,Long> recoverMap;
	
	public DubboXATransactionalResource() {
		super("dubboXATransactionalResource");
		uniqueResourceNameMap = new ConcurrentHashMap<String, Long>();
		recoverMap = new ConcurrentHashMap<String, Long>();
	}

	@Override
	protected XAResource refreshXAConnection() throws ResourceException {
		return null;
	}
	
	protected XAResource createDubboXAResource(String resourceName) {
		LOGGER.logInfo ( this.getName() + ": created " + resourceName + " XAResource" );	
		return new DubboXAResourceImpl(resourceName);
	}

	@Override
	public void recover() {
		
		try {
			Set<String> resourceNames = this.getExpiredCommittingResourceNames();
			for(String resourceName : recoverMap.keySet()){
				Long timeout = recoverMap.get(resourceName);
				if(timeout != null && timeout.longValue() <= System.currentTimeMillis()){
					recoverMap.remove(resourceName);
				}
				resourceNames.add(resourceName);
			}
			for(String resourceName : uniqueResourceNameMap.keySet()){
				long timeout = uniqueResourceNameMap.remove(resourceName);
				if(timeout > System.currentTimeMillis()){
					Long oldTimeout = recoverMap.get(resourceName);
					if(oldTimeout == null || timeout > oldTimeout.longValue()){
						recoverMap.put(resourceName, timeout);
					}
				}
				resourceNames.add(resourceName);
			}
			XaResourceRecoveryManager xaResourceRecoveryManager = XaResourceRecoveryManager.getInstance();
			if (xaResourceRecoveryManager != null) {
				for(String resourceName : resourceNames){
					try {
						xaResourceRecoveryManager.recover(this.createDubboXAResource(resourceName));
					} catch (Exception e) {
						LOGGER.logError(e.getMessage(), e);
					}
				}
			}
		} catch (LogException couldNotRetrieveCommittingXids) {
			LOGGER.logWarning("Transient error while recovering - will retry later...", couldNotRetrieveCommittingXids);
		}
		
	}
	
	private Set<String> getExpiredCommittingResourceNames() throws LogReadException {
		Set<String> ret = new HashSet<String>();
		RecoveryLog log = Configuration.getRecoveryLog();
		Collection<ParticipantLogEntry> entries = log.getCommittingParticipants();
		for (ParticipantLogEntry entry : entries) {
			if (expired(entry) && !http(entry)) {
				LOGGER.logWarning("committing interrupted " + entry.toString());
				ret.add(entry.resourceName);
			}
		}
		return ret;
	}

	private boolean http(ParticipantLogEntry entry) {
		return entry.uri.startsWith("http");
	}

	private boolean expired(ParticipantLogEntry entry) {
		long now = System.currentTimeMillis();
		return now > entry.expires;
	}
	
	private TransactionalResource createTransactionalResource(String uniqueResourceName,long timeout){
		recoverMap.remove(uniqueResourceName);
		uniqueResourceNameMap.put(uniqueResourceName, timeout);
		TransactionalResource transactionalResource = new TemporaryXATransactionalResource(uniqueResourceName);
		TransactionServiceProvider transactionService = (TransactionServiceProvider) Configuration.getTransactionService();
		transactionalResource.setRecoveryService(transactionService.getRecoveryService());
		return transactionalResource;
	}
	
	public TransactionalResource findOrCreateTransactionalResource(String uniqueResourceName,long timeout) {
		RecoverableResource resource = Configuration.getResource(uniqueResourceName);
		TransactionalResource ret = null;
		if(resource == null){
			ret = this.createTransactionalResource(uniqueResourceName,timeout);
		} else {
			ret = (TransactionalResource) resource;
		}
		return ret;
	}
}
