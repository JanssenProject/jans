/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.bc.testkit.AbstractTraceCryptoBenchmarkTest;

/**
 * Throughput benchmark of {@link NativeTraceCryptoProvider}. Opt-in: {@code -Dtrace.bench=true}.
 */
class NativeTraceCryptoBenchmarkTest extends AbstractTraceCryptoBenchmarkTest {

	@Override
	protected TraceCryptoProvider createProvider() {
		return TestProviders.nativeProvider();
	}

	@Override
	protected String label() {
		return "NATIVE";
	}

}
