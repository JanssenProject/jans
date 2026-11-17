/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import io.jans.lock.service.trace.TraceMvpAcceptanceTest;
import io.jans.lock.service.trace.testkit.TraceTestHarness;

/**
 * The MVP acceptance scenarios with every submission going through {@code ingestBatch} as a
 * one-element batch (in-memory atomic store).
 */
public class BatchPathTraceMvpAcceptanceTest extends TraceMvpAcceptanceTest {

	@Override
	protected TraceTestHarness newHarness() {
		return new TraceTestHarness().useBatchPath();
	}

}
