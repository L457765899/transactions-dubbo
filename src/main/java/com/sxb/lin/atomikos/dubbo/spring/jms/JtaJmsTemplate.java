package com.sxb.lin.atomikos.dubbo.spring.jms;

import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.jms.JmsException;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.SessionCallback;
import org.springframework.util.Assert;

import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;

public class JtaJmsTemplate extends JmsTemplate {
	
	private String dubboUniqueResourceName;
	
	private ConnectionFactory dubboConnectionFactory;

	@Override
	public <T> T execute(SessionCallback<T> action, boolean startConnection) throws JmsException {
		if(ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			Assert.notNull(action, "Callback object must not be null");
			try {
				Session sessionToUse = XAConnectionFactoryUtils.doGetTransactionalSession(
						getDubboConnectionFactory(), startConnection, this);
				return action.doInJms(sessionToUse);
			}
			catch (JMSException ex) {
				throw convertJmsAccessException(ex);
			}
		}else{
			return super.execute(action, startConnection);
		}
	}

	public String getDubboUniqueResourceName() {
		return dubboUniqueResourceName;
	}

	public void setDubboUniqueResourceName(String dubboUniqueResourceName) {
		this.dubboUniqueResourceName = dubboUniqueResourceName;
	}

	@Override
	public void afterPropertiesSet() {
		super.afterPropertiesSet();
		if (this.getDubboConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'dubboConnectionFactory' is required");
		}
		if(this.getDubboUniqueResourceName() == null){
			throw new IllegalArgumentException("Property 'dubboUniqueResourceName' is required");
		}
	}

	public ConnectionFactory getDubboConnectionFactory() {
		return dubboConnectionFactory;
	}

	public void setDubboConnectionFactory(ConnectionFactory dubboConnectionFactory) {
		this.dubboConnectionFactory = dubboConnectionFactory;
	}
}
