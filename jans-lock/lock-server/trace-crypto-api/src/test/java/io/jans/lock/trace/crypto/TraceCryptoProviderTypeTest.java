/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class TraceCryptoProviderTypeTest {

	@Test
	void fromStringIsCaseInsensitive() {
		assertEquals(TraceCryptoProviderType.JAVA, TraceCryptoProviderType.fromString("java"));
		assertEquals(TraceCryptoProviderType.NATIVE, TraceCryptoProviderType.fromString("Native"));
	}

	@Test
	void fromStringTrims() {
		assertEquals(TraceCryptoProviderType.JAVA, TraceCryptoProviderType.fromString("  JAVA\t"));
	}

	@Test
	void fromStringUnknownIsNull() {
		assertNull(TraceCryptoProviderType.fromString("rust"));
		assertNull(TraceCryptoProviderType.fromString(""));
		assertNull(TraceCryptoProviderType.fromString(null));
	}

}
