/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.client.trace;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Minimal, independent RFC 8785 (JSON Canonicalization Scheme) encoder for TRACE end-to-end
 * client tests. Deliberately not shared with {@code io.jans.lock.service.trace.canon.JcsCanonicalizer}
 * (the Lock Server's own implementation): the test signs assertions with its own canonicalization
 * so it exercises the server's independent verifier, rather than two call sites of the same code
 * agreeing with themselves. Sufficient for the integer/string/ASCII content TRACE assertions use —
 * it does not implement RFC 8785's ECMA-262 number-formatting rules for floating point values.
 */
public final class TraceJcs {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private TraceJcs() {
	}

	/** @return UTF-8 bytes of the canonical form of {@code node} (object keys sorted recursively) */
	public static byte[] canonicalBytes(JsonNode node) {
		JsonNode sorted = sortKeys(node);
		try {
			return MAPPER.writeValueAsBytes(sorted);
		} catch (Exception ex) {
			throw new IllegalStateException("Failed to canonicalize JSON", ex);
		}
	}

	/** @return {@code "sha256:" + lowercase hex} of the SHA-256 digest of {@code canonicalBytes(node)} */
	public static String sha256Digest(JsonNode node) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(canonicalBytes(node));
			StringBuilder hex = new StringBuilder(64);
			for (byte b : hash) {
				hex.append(String.format("%02x", b));
			}
			return "sha256:" + hex;
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private static JsonNode sortKeys(JsonNode node) {
		if (node.isObject()) {
			TreeMap<String, JsonNode> sorted = new TreeMap<>();
			node.fields().forEachRemaining(entry -> sorted.put(entry.getKey(), sortKeys(entry.getValue())));
			ObjectNode out = MAPPER.createObjectNode();
			sorted.forEach(out::set);
			return out;
		}
		if (node.isArray()) {
			ArrayNode out = MAPPER.createArrayNode();
			node.forEach(child -> out.add(sortKeys(child)));
			return out;
		}
		return node;
	}

	static String toJsonString(JsonNode node) {
		return new String(canonicalBytes(node), StandardCharsets.UTF_8);
	}

}
