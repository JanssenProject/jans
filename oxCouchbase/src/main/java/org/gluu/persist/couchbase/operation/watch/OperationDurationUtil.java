package org.gluu.persist.couchbase.operation.watch;

import org.gluu.persist.watch.DurationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple Couchbase operation duration calculator helper
 *
 * @author Yuriy Movchan Date: 04/08/2019
 */
public class OperationDurationUtil extends DurationUtil {

    private static final Logger log = LoggerFactory.getLogger(OperationDurationUtil.class);
    
    private static OperationDurationUtil instance = new OperationDurationUtil();

    public static DurationUtil instance() {
    	return instance;
    }

	@Override
	public void logDebug(String format, Object... arguments) {
		super.logDebug(format, arguments);
	}

	@Override
	public Logger getLog() {
		return log;
	}

}
