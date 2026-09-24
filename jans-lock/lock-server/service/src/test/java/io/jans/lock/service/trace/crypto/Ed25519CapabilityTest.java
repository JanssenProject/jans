/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.jans.lock.service.trace.error.TraceCryptoException;
import io.jans.util.security.SecurityProviderUtility;

class Ed25519CapabilityTest {

	@BeforeAll
	static void installProvider() {
		// Guarded: a previous test class in the same JVM may already have installed the provider.
		Ed25519TestKeys.installProvider();
	}

	@Test
	void testProbe_withInstalledProvider_available() {
		Ed25519Capability capability = new Ed25519Capability();

		capability.probe();

		assertTrue(capability.isAvailable(), "failure: " + capability.getFailure());
		assertNull(capability.getFailure());
		assertEquals(SecurityProviderUtility.getBCProvider().getName(), capability.getProviderName());
		assertNotNull(capability.getProviderName());
		assertDoesNotThrow(capability::requireAvailable);
	}

	@Test
	void testProbe_calledTwice_stillAvailable() {
		Ed25519Capability capability = new Ed25519Capability();

		capability.probe();
		capability.probe();

		assertTrue(capability.isAvailable());
	}

	@Test
	void testRequireAvailable_beforeProbe_throwsCryptoUnavailable() {
		Ed25519Capability capability = new Ed25519Capability();

		assertFalse(capability.isAvailable());
		TraceCryptoException ex = assertThrows(TraceCryptoException.class, capability::requireAvailable);
		assertEquals(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE, ex.getReason());
	}

}
