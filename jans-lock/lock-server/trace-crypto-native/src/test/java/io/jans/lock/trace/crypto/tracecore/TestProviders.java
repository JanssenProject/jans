/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.bc.Ed25519Capability;
import io.jans.lock.trace.crypto.bc.Ed25519Verifier;
import io.jans.lock.trace.crypto.bc.JavaTraceCryptoProvider;
import io.jans.lock.trace.crypto.bc.testkit.TraceTestKeys;

/**
 * Builds the two providers.
 */
final class TestProviders {

	private TestProviders() {
	}

	static NativeTraceCryptoProvider nativeProvider() {
		return nativeProvider(new TraceConfiguration());
	}

	static NativeTraceCryptoProvider nativeProvider(TraceConfiguration traceConfiguration) {
		NativeTraceCryptoProvider provider = new NativeTraceCryptoProvider(traceConfiguration);
		provider.probe();
		return provider;
	}

	static TraceCryptoProvider javaProvider() {
		TraceTestKeys.installProvider();
		Ed25519Capability capability = new Ed25519Capability();
		capability.probe();
		return new JavaTraceCryptoProvider(capability, new Ed25519Verifier());
	}

}
