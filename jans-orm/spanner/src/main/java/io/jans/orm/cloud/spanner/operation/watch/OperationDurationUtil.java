/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.orm.cloud.spanner.operation.watch;

import io.jans.orm.watch.DurationUtil;

/**
 * Simple Couchbase operation duration calculator helper
 *
 * @author Yuriy Movchan Date: 12/16/2020
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
