/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.io.InputStream;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

/**
 * Tests for {@link ParsedAssertion#signatureInput()} and {@link ParsedAssertion#contentDigest()}
 * (design D-13): the signature input must exclude {@code signature} while the content digest must
 * include it, and both must be computed once and cached.
 */
class ParsedAssertionTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static ObjectNode loadFixture() throws IOException {
		try (InputStream is = ParsedAssertionTest.class
				.getResourceAsStream("/trace/assertions/valid-authorization-decision.json")) {
			return (ObjectNode) MAPPER.readTree(is);
		}
	}

	@Test
	void testSignatureInput_unaffectedBySignatureValue_contentDigestChanges() throws IOException {
		ObjectNode rootA = loadFixture();
		ObjectNode rootB = rootA.deepCopy();
		rootB.set("signature", TextNode.valueOf("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));

		ParsedAssertion assertionA = new ParsedAssertion(rootA.toString(), rootA);
		ParsedAssertion assertionB = new ParsedAssertion(rootB.toString(), rootB);

		assertArrayEquals(assertionA.signatureInput(), assertionB.signatureInput());
		assertNotEquals(assertionA.contentDigest(), assertionB.contentDigest());
	}

	@Test
	void testSignatureInput_isComputedOnceAndCached() throws IOException {
		ParsedAssertion assertion = new ParsedAssertion(loadFixture().toString(), loadFixture());

		byte[] first = assertion.signatureInput();
		byte[] second = assertion.signatureInput();

		assertSame(first, second);
	}

	@Test
	void testContentDigest_isComputedOnceAndCached() throws IOException {
		ParsedAssertion assertion = new ParsedAssertion(loadFixture().toString(), loadFixture());

		String first = assertion.contentDigest();
		String second = assertion.contentDigest();

		assertSame(first, second);
	}

}
