/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.bc;

import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.bc.testkit.AbstractTraceCryptoBenchmarkTest;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;

/**
 * Throughput benchmark of {@link JavaTraceCryptoProvider}. Opt-in: {@code -Dtrace.bench=true}.
 */
class JavaTraceCryptoBenchmarkTest extends AbstractTraceCryptoBenchmarkTest {

	@Override
	protected TraceCryptoProvider createProvider() {
		TraceTestKeys.installProvider();
		Ed25519Capability capability = new Ed25519Capability();
		capability.probe();
		return new JavaTraceCryptoProvider(capability, new Ed25519Verifier());
	}

	@Override
	protected String label() {
		return "JAVA";
	}

}
