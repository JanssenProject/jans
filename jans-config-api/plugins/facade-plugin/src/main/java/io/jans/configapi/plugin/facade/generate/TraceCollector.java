/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.configapi.plugin.facade.generate;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.configapi.plugin.facade.lock.LockClient;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Fetches the TRACE records of the executions in {@code executions.json} from Lock into {@code traces/}. */
public final class TraceCollector {

    private static final Logger LOG = LoggerFactory.getLogger(TraceCollector.class);

    private TraceCollector() {
    }

    /** @return the number of records that Lock did not have after the timeout */
    public static int collect(LockClient lock, String readToken, Path outDir, Duration timeoutPerExecution)
            throws IOException, InterruptedException {
        List<TraceFiles.Ref> executions = TraceFiles.readExecutions(outDir);
        LOG.info("Collecting {} executions from Lock", executions.size());
        int missing = 0;
        for (TraceFiles.Ref execution : executions) {
            JsonNode stored = lock.fetchExecution(execution.id(), execution.authority(), readToken,
                    execution.expectedRecords(), timeoutPerExecution);
            int got = stored.path("records").size();
            if (got < execution.expectedRecords()) {
                missing += execution.expectedRecords() - got;
                LOG.warn("Execution {}: {} of {} records", execution.id(), got, execution.expectedRecords());
            }
            TraceFiles.writeTrace(outDir, execution.id(), stored);
        }
        LOG.info("Stored traces in {} (missing records: {})", outDir.resolve(TraceFiles.TRACES_DIR), missing);
        return missing;
    }
}
