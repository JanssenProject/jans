/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.testkit;

import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.bc.Ed25519Capability;
import io.jans.lock.trace.crypto.bc.Ed25519Verifier;
import io.jans.lock.trace.crypto.bc.JavaTraceCryptoProvider;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;

/**
 * Builds the {@code JAVA} {@link TraceCryptoProvider} and {@link TraceKeys} the unit tests wire by
 * hand, since no test boots a CDI container.
 */
public final class TraceTestCrypto {

	private TraceTestCrypto() {
	}

	/** @return a probed {@link JavaTraceCryptoProvider} over the installed test security provider */
	public static TraceCryptoProvider javaProvider() {
		TraceTestKeys.installProvider();
		Ed25519Capability capability = new Ed25519Capability();
		capability.probe();
		return new JavaTraceCryptoProvider(capability, new Ed25519Verifier());
	}

	public static TraceKeys traceKeys() {
		return new TraceKeys(javaProvider());
	}

	public static TraceKeys traceKeys(TraceCryptoProvider provider) {
		return new TraceKeys(provider);
	}

}
