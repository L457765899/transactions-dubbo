package com.sxb.lin.atomikos.dubbo.pool;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.Xid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sxb.lin.atomikos.dubbo.service.StartXid;

public class JmsXAResourceHolder implements XAResourceHolder{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(JmsXAResourceHolder.class);
	
	

	public void start() throws XAException, SystemException, RollbackException {
		// TODO Auto-generated method stub
		
	}

	public void end() throws XAException {
		// TODO Auto-generated method stub
		
	}

	public int prepare(Xid xid) throws XAException {
		// TODO Auto-generated method stub
		return 0;
	}

	public void commit(Xid xid, boolean onePhase) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public void rollback(Xid xid) throws XAException {
		// TODO Auto-generated method stub
		
	}

	public String getUuid() {
		// TODO Auto-generated method stub
		return null;
	}

	public StartXid getStartXid() {
		// TODO Auto-generated method stub
		return null;
	}

	public String getTmAddress() {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() {
		// TODO Auto-generated method stub
		
	}

}
