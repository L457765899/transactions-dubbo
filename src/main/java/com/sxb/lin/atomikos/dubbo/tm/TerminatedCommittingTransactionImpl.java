package com.sxb.lin.atomikos.dubbo.tm;

import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atomikos.icatch.config.Configuration;
import com.atomikos.recovery.LogReadException;
import com.atomikos.recovery.ParticipantLogEntry;
import com.atomikos.recovery.RecoveryLog;
import com.atomikos.recovery.TxState;
import com.mysql.jdbc.StringUtils;

public class TerminatedCommittingTransactionImpl implements TerminatedCommittingTransaction{
	
	private static final Logger LOGGER = LoggerFactory.getLogger(TerminatedCommittingTransactionImpl.class);
	
	private static final int DEFAULT_FORMAT = ('A' << 24) + ('T' << 16) + ('O' << 8) + 'M';

	public void terminated(String tid) {
		try {
			RecoveryLog log = Configuration.getRecoveryLog();
			Collection<ParticipantLogEntry> entries = log.getCommittingParticipants();
			for (ParticipantLogEntry entry : entries) {
				if(tid.equals(entry.coordinatorId) && entry.expires < System.currentTimeMillis()){
					ParticipantLogEntry terminatedEntry = new ParticipantLogEntry(
							entry.coordinatorId,entry.uri,entry.expires,entry.resourceName,TxState.TERMINATED);
					log.terminated(terminatedEntry);
				}
			}
		} catch (LogReadException e) {
			LOGGER.error("terminated committing transaction error", e);
		}
	}

	public String convertToMysqlXid(String globalTransactionId,String branchQualifier) {
		StringBuilder builder = new StringBuilder();
		byte[] gtrid = globalTransactionId.getBytes();
        byte[] btrid = branchQualifier.getBytes();
        if (gtrid != null) {
            StringUtils.appendAsHex(builder, gtrid);
        }

        builder.append(',');
        if (btrid != null) {
            StringUtils.appendAsHex(builder, btrid);
        }

        builder.append(',');
        StringUtils.appendAsHex(builder, DEFAULT_FORMAT);
        
        return builder.toString();
	}

	
}
