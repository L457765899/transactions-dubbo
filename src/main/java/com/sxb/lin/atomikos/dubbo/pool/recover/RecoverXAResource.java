package com.sxb.lin.atomikos.dubbo.pool.recover;

import javax.transaction.xa.XAResource;

public interface RecoverXAResource {

	void close();

	XAResource getXAResource();

}
