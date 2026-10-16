/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

class TraceHashesTest {

	private static final String EMPTY_SHA256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

	@Test
	void sha256OfEmptyInput() {
		assertEquals(EMPTY_SHA256, TraceHashes.sha256Hex(new byte[0]));
		assertEquals("sha256:" + EMPTY_SHA256, TraceHashes.sha256Prefixed(new byte[0]));
	}

	@Test
	void sha256OfKnownInput() {
		assertEquals("sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				TraceHashes.sha256Prefixed("abc".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void zeroHashShape() {
		assertEquals(71, TraceHashes.ZERO_HASH.length());
		assertTrue(TraceHashes.ZERO_HASH.startsWith(TraceHashes.SHA256_PREFIX));
		assertEquals("0".repeat(64), TraceHashes.ZERO_HASH.substring(TraceHashes.SHA256_PREFIX.length()));
	}

	@Test
	void receiptProfileTag() {
		assertEquals("tag:jans.io,2026:lock-trace-receipt-v1", TraceHashes.RECEIPT_PROFILE);
	}

	@Test
	void nullInputRejected() {
		assertThrows(IllegalArgumentException.class, () -> TraceHashes.sha256Prefixed(null));
		assertThrows(IllegalArgumentException.class, () -> TraceHashes.sha256Hex(null));
	}

}
