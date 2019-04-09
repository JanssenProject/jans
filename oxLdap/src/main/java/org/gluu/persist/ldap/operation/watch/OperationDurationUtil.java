package org.gluu.persist.ldap.operation.watch;

import org.gluu.persist.watch.DurationUtil;

/**
 * Simple LDAP operation duration calculator helper
 *
 * @author Yuriy Movchan Date: 04/08/2019
 */
public class OperationDurationUtil extends DurationUtil {

    private static OperationDurationUtil instance = new OperationDurationUtil();

    public static DurationUtil instance() {
    	return instance;
    }

    public void logDebug(String format, Object... arguments) {
        if (log.isDebugEnabled()) {
            log.debug(format, arguments);
        }
    }

}
