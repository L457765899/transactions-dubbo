package com.sxb.lin.atomikos.dubbo.spring.jms;

import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.jms.JmsException;
import org.springframework.jms.core.SessionCallback;
import org.springframework.util.Assert;

import com.sxb.lin.atomikos.dubbo.ParticipantXATransactionLocal;

public class JmsTemplate extends org.springframework.jms.core.JmsTemplate {

	@Override
	public <T> T execute(SessionCallback<T> action, boolean startConnection) throws JmsException {
		if(ParticipantXATransactionLocal.isUseParticipantXATransaction()){
			Assert.notNull(action, "Callback object must not be null");
			try {
				Session sessionToUse = XAConnectionFactoryUtils.doGetTransactionalSession(
						getConnectionFactory(), startConnection, this);
				return action.doInJms(sessionToUse);
			}
			catch (JMSException ex) {
				throw convertJmsAccessException(ex);
			}
		}else{
			return super.execute(action, startConnection);
		}
	}

}
