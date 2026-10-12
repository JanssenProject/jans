/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.receipt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

import io.jans.lock.service.trace.TraceConstants;

/**
 * Known-vector test for {@link ReceiptHashes#receiptHash}: the expected hash below is computed
 * independently here, by hand-building the canonical JSON string (object members in ASCII key
 * order, exactly as RFC 8785/JCS would sort them) and hashing it with a raw {@link MessageDigest},
 * never through {@code JcsCanonicalizer}/{@code CanonicalHashes} (design §9, TRACE MVP design
 * decisions D-8, D-13).
 */
class ReceiptHashesTest {

	@Test
	void testReceiptHash_knownVector() {
		String domainId = "domain-1";
		long receiptSequence = 1L;
		long receivedAtMs = 1_800_000_000_000L;
		String producerId = "producer-1/1.0.0";
		String recordId = "record-1";
		String contentDigest = "sha256:" + repeat('a', 64);
		String prevReceiptHash = TraceConstants.ZERO_HASH;

		String canonical = "{\"content_digest\":\"" + contentDigest + "\",\"evidence_domain_id\":\"" + domainId
				+ "\",\"prev_receipt_hash\":\"" + prevReceiptHash + "\",\"producer_id\":\"" + producerId
				+ "\",\"receipt_sequence\":" + receiptSequence + ",\"received_at\":" + receivedAtMs
				+ ",\"record_id\":\"" + recordId + "\"}";
		String expected = "sha256:" + sha256Hex(canonical);

		String actual = ReceiptHashes.receiptHash(domainId, receiptSequence, receivedAtMs, producerId, recordId,
				contentDigest, prevReceiptHash);

		assertEquals(expected, actual);
		assertTrue(actual.matches("^sha256:[0-9a-f]{64}$"));
	}

	@Test
	void testReceiptHash_differentPrevReceiptHash_differentResult() {
		String domainId = "domain-1";
		long receiptSequence = 2L;
		long receivedAtMs = 1_800_000_000_000L;
		String producerId = "producer-1/1.0.0";
		String recordId = "record-2";
		String contentDigest = "sha256:" + repeat('b', 64);

		String hashA = ReceiptHashes.receiptHash(domainId, receiptSequence, receivedAtMs, producerId, recordId,
				contentDigest, "sha256:" + repeat('1', 64));
		String hashB = ReceiptHashes.receiptHash(domainId, receiptSequence, receivedAtMs, producerId, recordId,
				contentDigest, "sha256:" + repeat('2', 64));

		assertNotEquals(hashA, hashB);
	}

	@Test
	void testReceiptHash_nullDomainId_throwsNullPointerException() {
		assertThrows(NullPointerException.class,
				() -> ReceiptHashes.receiptHash(null, 1L, 1_000L, "p/1.0.0", "r", "sha256:" + repeat('0', 64),
						TraceConstants.ZERO_HASH));
	}

	private static String sha256Hex(String s) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(s.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(Character.forDigit((b >> 4) & 0xF, 16));
				sb.append(Character.forDigit(b & 0xF, 16));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static String repeat(char c, int count) {
		char[] chars = new char[count];
		java.util.Arrays.fill(chars, c);
		return new String(chars);
	}

}
