/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StrictBase64UrlTest {

	@ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
	@CsvSource({ "'', ''", "Zg, f", "Zm8, fo", "Zm9v, foo", "Zm9vYg, foob", "Zm9vYmE, fooba", "Zm9vYmFy, foobar" })
	void testDecode_rfc4648Vectors_decode(String encoded, String expected) {
		assertArrayEquals(expected.getBytes(StandardCharsets.US_ASCII), StrictBase64Url.decode(encoded));
	}

	@ParameterizedTest(name = "\"{1}\" -> \"{0}\"")
	@CsvSource({ "'', ''", "Zg, f", "Zm8, fo", "Zm9v, foo", "Zm9vYg, foob", "Zm9vYmE, fooba", "Zm9vYmFy, foobar" })
	void testEncode_rfc4648Vectors_noPadding(String expected, String plain) {
		assertEquals(expected, StrictBase64Url.encode(plain.getBytes(StandardCharsets.US_ASCII)));
	}

	@Test
	void testDecode_YQ_decodesToA() {
		assertArrayEquals(new byte[] { 'a' }, StrictBase64Url.decode("YQ"));
	}

	@ParameterizedTest(name = "\"{0}\" rejected")
	@ValueSource(strings = { "YQ==", "YQ=", "Zm8=", "YR", "Zm9", "Y Q", " YQ", "YQ ", "YQ\n", "A", "YQ+", "Y/Q",
			"YQ.", "ÿQ" })
	void testDecode_paddedNonCanonicalOrForeignCharacters_rejected(String input) {
		assertThrows(IllegalArgumentException.class, () -> StrictBase64Url.decode(input));
	}

	@Test
	void testDecode_lengthOneModFour_rejected() {
		assertThrows(IllegalArgumentException.class, () -> StrictBase64Url.decode("Zm9vY"));
	}

	@Test
	void testDecode_null_rejected() {
		assertThrows(IllegalArgumentException.class, () -> StrictBase64Url.decode(null));
	}

	@Test
	void testEncode_null_rejected() {
		assertThrows(IllegalArgumentException.class, () -> StrictBase64Url.encode(null));
	}

	@Test
	void testEncode_urlSafeAlphabet_usesMinusAndUnderscore() {
		// 0xfb 0xff -> "-_8" in the URL-safe alphabet ("+/8" in the standard one)
		assertEquals("-_8", StrictBase64Url.encode(new byte[] { (byte) 0xfb, (byte) 0xff }));
		assertArrayEquals(new byte[] { (byte) 0xfb, (byte) 0xff }, StrictBase64Url.decode("-_8"));
	}

	@Test
	void testRoundTrip_randomLengths_identity() {
		SecureRandom random = new SecureRandom();
		for (int length = 0; length < 70; length++) {
			byte[] bytes = new byte[length];
			random.nextBytes(bytes);
			assertArrayEquals(bytes, StrictBase64Url.decode(StrictBase64Url.encode(bytes)), "length " + length);
		}
	}

}
