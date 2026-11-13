/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import io.jans.lock.service.trace.testkit.TraceTestHarness;

/**
 * The batch-versus-one-by-one scenarios with parallel verification and post-commit correlation on
 * its own pool. The comparison runs after the pools are drained.
 */
public class ParallelBatchIngestDifferentialTest extends TraceBatchIngestDifferentialTest {

	@Override
	protected TraceTestHarness newHarness() {
		TraceTestHarness harness = new TraceTestHarness();
		harness.traceConfiguration().setBatchVerifyThreads(4);
		harness.traceConfiguration().setPostCommitCorrelationThreads(2);
		return harness;
	}

}
