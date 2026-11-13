/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import io.jans.lock.service.trace.TraceChainIntegrityTest;
import io.jans.lock.service.trace.testkit.TraceTestHarness;

/**
 * The receipt-chain integrity scenarios with every submission going through {@code ingestBatch} as a
 * one-element batch (in-memory atomic store).
 */
public class BatchPathTraceChainIntegrityTest extends TraceChainIntegrityTest {

	@Override
	protected TraceTestHarness newHarness() {
		return new TraceTestHarness().useBatchPath();
	}

}
