/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.service.trace.TraceChainIntegrityTest;
import io.jans.lock.service.trace.testkit.TraceTestHarness;

/**
 * The receipt-chain integrity suite over the {@code NATIVE} provider.
 */
public class NativeTraceChainIntegrityTest extends TraceChainIntegrityTest {

	@Override
	protected TraceTestHarness newHarness() {
		return new TraceTestHarness(TestProviders.nativeProvider(new AppConfiguration().getTraceConfiguration()));
	}

}
