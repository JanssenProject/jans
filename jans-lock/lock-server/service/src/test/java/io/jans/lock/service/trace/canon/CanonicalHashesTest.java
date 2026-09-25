/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.canon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

class CanonicalHashesTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void testSha256Prefixed_knownVector() {
		String hash = CanonicalHashes.sha256Prefixed(new byte[0]);

		assertEquals("sha256:e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", hash);
		assertTrue(hash.startsWith("sha256:e3b0c442"));
		assertTrue(hash.endsWith("b855"));
		assertTrue(hash.matches("^sha256:[0-9a-f]{64}$"));
	}

	@Test
	void testSha256Prefixed_abc_matchesFipsTestVector() {
		// FIPS 180-4 example: SHA-256("abc")
		assertEquals("sha256:ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
				CanonicalHashes.sha256Prefixed("abc".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	void testSha256Prefixed_null_throws() {
		assertThrows(IllegalArgumentException.class, () -> CanonicalHashes.sha256Prefixed(null));
	}

	@Test
	void testSha256PrefixedOfJcs_emptyObject_matchesDigestOfBraces() {
		// sha256sum of the two bytes "{}"
		assertEquals("sha256:44136fa355b3678a1146ad16f7e8649e94fb4fc21fe77e8310c060f61caaff8a",
				CanonicalHashes.sha256PrefixedOfJcs(JsonNodeFactory.instance.objectNode()));
	}

	@Test
	void testSha256PrefixedOfJcs_equalsDigestOfCanonicalBytes_independentOfInputFormatting() throws IOException {
		JsonNode compact = MAPPER.readTree("{\"b\":[1,2.0],\"a\":\"\\u20ac\"}");
		JsonNode spaced = MAPPER.readTree("{ \"a\" : \"\u20ac\", \"b\" : [ 1 , 2 ] }");

		String expected = CanonicalHashes.sha256Prefixed(JcsCanonicalizer.canonicalizeToUtf8(compact));

		assertEquals(expected, CanonicalHashes.sha256PrefixedOfJcs(compact));
		assertEquals(expected, CanonicalHashes.sha256PrefixedOfJcs(spaced));
	}

	@Test
	void testSha256PrefixedOfJcs_nonCanonicalizable_throwsJcsException() {
		assertThrows(JcsException.class,
				() -> CanonicalHashes.sha256PrefixedOfJcs(JsonNodeFactory.instance.numberNode(Double.NaN)));
	}

}
