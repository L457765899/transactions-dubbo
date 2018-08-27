package com.sxb.lin.atomikos.dubbo.spring.jms;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Session;

import org.springframework.jms.connection.JmsResourceHolder;

public class XAJmsResourceHolder extends JmsResourceHolder{

	public XAJmsResourceHolder(ConnectionFactory connectionFactory,
			Connection connection, Session session) {
		super(connectionFactory, connection, session);
	}
}
