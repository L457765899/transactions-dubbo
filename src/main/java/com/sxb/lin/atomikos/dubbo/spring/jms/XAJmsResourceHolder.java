package com.sxb.lin.atomikos.dubbo.spring.jms;

import java.util.UUID;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.XASession;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;

import org.springframework.jms.connection.JmsResourceHolder;

import com.sxb.lin.atomikos.dubbo.pool.JmsXAResourceHolder;
import com.sxb.lin.atomikos.dubbo.pool.XAResourceHolder;

public class XAJmsResourceHolder extends JmsResourceHolder{
	
	private XAResourceHolder xaResourceHolder;
	
	private boolean isClose;

	public XAJmsResourceHolder(ConnectionFactory connectionFactory,
			Connection connection, XASession session, String dubboUniqueResourceName) throws JMSException {
		super(connectionFactory, connection, session);
		
		String connStr = connection.toString();
		String uuid = UUID.nameUUIDFromBytes(connStr.getBytes()).toString();
		this.xaResourceHolder = new JmsXAResourceHolder(dubboUniqueResourceName, uuid, session.getXAResource(), this);
		this.isClose = false;
		try {
			this.xaResourceHolder.start();
		} catch (XAException e) {
			JMSException jms = new JMSException(e.getMessage());
			jms.setLinkedException(e);
			throw jms;
		} catch (SystemException e) {
			JMSException jms = new JMSException(e.getMessage());
			jms.setLinkedException(e);
			throw jms;
		} catch (RollbackException e) {
			JMSException jms = new JMSException(e.getMessage());
			jms.setLinkedException(e);
			throw jms;
		}
	}

	public XAResourceHolder getXaResourceHolder() {
		return xaResourceHolder;
	}
	
	public void end() throws XAException{
		this.xaResourceHolder.end();
	}

	@Override
	public void reset() {
		super.reset();
		this.xaResourceHolder = null;
	}

	@Override
	public void closeAll() {
		if(this.isClose){
			return;
		}
		super.closeAll();
		this.isClose = true;
	}
	
}
