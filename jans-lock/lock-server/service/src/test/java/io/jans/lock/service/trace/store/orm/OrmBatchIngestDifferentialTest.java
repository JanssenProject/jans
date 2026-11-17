/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.store.orm;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import io.jans.lock.service.trace.ingest.TraceBatchIngestDifferentialTest;
import io.jans.lock.service.trace.testkit.TraceTestCrypto;
import io.jans.lock.service.trace.testkit.TraceTestHarness;

/**
 * The batch-versus-one-by-one scenarios over {@link OrmTraceStore}: its atomic batch insert, the
 * multi-key reads and the entity mapping, with {@link FakeEntryManagerBehavior} standing in for the
 * database. A run against a live SQL database needs the opt-in profile used by
 * {@code TraceIngestBenchmark} and is not part of this class.
 */
class OrmBatchIngestDifferentialTest extends TraceBatchIngestDifferentialTest {

	@Test
	void ormStoreReportsAtomicBatchSupport() {
		assertTrue(newHarness().ingestionService().isBatchIngestAvailable());
	}

	@Override
	protected TraceTestHarness newHarness() {
		return new TraceTestHarness(TraceTestCrypto.javaProvider(),
				keys -> OrmTraceStores.create(FakeEntryManagerBehavior.create(), keys));
	}

}
