/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.parse.TraceValidationException;

/**
 * Field-level checks shared by more than one {@link EventKindValidator}: the {@code {capability_id,
 * outcome}} list shape used by {@code AUTHORIZATION_DECISION} and {@code CAPABILITY_INVOKED}
 * (design §7.2), and the {@code tokens[]} shape common to every kind (design §7.3).
 *
 * @author Yuriy Movchan
 */
final class EventKindValidationSupport {

	static final Set<String> DECISION_OUTCOMES = Collections
			.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("ALLOW", "DENY")));

	private static final int MAX_CAPABILITY_ID_LENGTH = 512;

	private static final int MAX_TOKEN_TYPE_LENGTH = 64;

	private static final int MAX_JTI_LENGTH = 256;

	/** {@code ^[A-Za-z0-9_-]{16,128}$} — the generic opaque-fingerprint form (design §7.3). */
	private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,128}$");

	/** Compact JWS/JWT shape: three dot-separated base64url segments (design §7.3). */
	private static final Pattern COMPACT_JWS_PATTERN = Pattern
			.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

	private static final int MIN_RAW_TOKEN_MATERIAL_LENGTH = 20;

	private EventKindValidationSupport() {
	}

	static void forbidField(JsonNode parent, String field, String path) {
		JsonNode node = parent.get(field);
		if (node != null && !node.isNull()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "forbidden:" + path);
		}
	}

	static JsonNode requireField(JsonNode parent, String field, String path) {
		JsonNode node = parent.get(field);
		if (node == null || node.isNull()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "missing:" + path);
		}
		return node;
	}

	static JsonNode requireObject(JsonNode parent, String field, String path) {
		JsonNode node = requireField(parent, field, path);
		if (!node.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		return node;
	}

	/**
	 * @param maxLength inclusive upper bound on character length, or {@code null} for no extra
	 *        bound beyond "non-empty"
	 */
	static String requireNonEmptyString(JsonNode parent, String field, String path, Integer maxLength) {
		JsonNode node = requireField(parent, field, path);
		if (!node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		String value = node.textValue();
		if (value.isEmpty()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "empty:" + path);
		}
		if (maxLength != null && value.length() > maxLength) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "length:" + path);
		}
		return value;
	}

	/**
	 * Type-only check: the design calls {@code target_id}/{@code result_id} "optional ... strings"
	 * with no length or non-empty rule (design §7.2 keeps {@code RUNTIME_EFFECT} minimal on
	 * purpose), so this only rejects a present-but-non-textual value.
	 */
	static void optionalString(JsonNode parent, String field, String path) {
		JsonNode node = parent.get(field);
		if (node == null || node.isNull()) {
			return;
		}
		if (!node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
	}

	static void optionalHashString(JsonNode parent, String field, String path) {
		JsonNode node = parent.get(field);
		if (node == null || node.isNull()) {
			return;
		}
		if (!node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		if (!TraceConstants.HASH_PATTERN.matcher(node.textValue()).matches()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "invalid:" + path);
		}
	}

	static String requireEnum(JsonNode parent, String field, String path, Set<String> allowed) {
		String value = requireNonEmptyString(parent, field, path, null);
		if (!allowed.contains(value)) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "invalid:" + path);
		}
		return value;
	}

	/**
	 * Validates a non-empty {@code capability_ids[]} array of {@code {capability_id, outcome}}
	 * entries (design §7.2) and returns the {@code capability_id} values, deduplicated with order
	 * preserved.
	 */
	static List<String> requireCapabilityIds(JsonNode event, String basePath) {
		JsonNode node = requireField(event, "capability_ids", basePath);
		if (!node.isArray()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + basePath);
		}
		if (node.size() == 0) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "empty:" + basePath);
		}
		Set<String> dedup = new LinkedHashSet<>();
		for (int i = 0; i < node.size(); i++) {
			JsonNode entry = node.get(i);
			String entryPath = basePath + "[" + i + "]";
			if (!entry.isObject()) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"type:" + entryPath);
			}
			String capabilityId = requireNonEmptyString(entry, "capability_id", entryPath + ".capability_id",
					MAX_CAPABILITY_ID_LENGTH);
			requireEnum(entry, "outcome", entryPath + ".outcome", DECISION_OUTCOMES);
			dedup.add(capabilityId);
		}
		return new ArrayList<>(dedup);
	}

	/**
	 * Validates the optional {@code tokens[]} array common to every event kind (design §7.3) and
	 * returns the extracted {@link TokenRef}s.
	 */
	static List<TokenRef> extractTokens(JsonNode event, String basePath) {
		JsonNode node = event.get("tokens");
		if (node == null || node.isNull()) {
			return Collections.emptyList();
		}
		if (!node.isArray()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + basePath);
		}
		List<TokenRef> result = new ArrayList<>(node.size());
		for (int i = 0; i < node.size(); i++) {
			JsonNode entry = node.get(i);
			String entryPath = basePath + "[" + i + "]";
			if (!entry.isObject()) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"type:" + entryPath);
			}
			rejectRawTokenMaterial(entry);

			String issuer = requireNonEmptyString(entry, "issuer", entryPath + ".issuer", null);
			validateIssuer(issuer, entryPath + ".issuer");
			String tokenType = requireNonEmptyString(entry, "token_type", entryPath + ".token_type",
					MAX_TOKEN_TYPE_LENGTH);

			boolean hasJti = entry.has("jti") && !entry.get("jti").isNull();
			boolean hasFingerprint = entry.has("fingerprint") && !entry.get("fingerprint").isNull();
			if (hasJti == hasFingerprint) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"token_ref_identity");
			}

			String jti = null;
			String fingerprint = null;
			if (hasJti) {
				jti = requireNonEmptyString(entry, "jti", entryPath + ".jti", MAX_JTI_LENGTH);
			} else {
				fingerprint = requireNonEmptyString(entry, "fingerprint", entryPath + ".fingerprint", null);
				if (!TraceConstants.HASH_PATTERN.matcher(fingerprint).matches()
						&& !FINGERPRINT_PATTERN.matcher(fingerprint).matches()) {
					throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
							"invalid:" + entryPath + ".fingerprint");
				}
			}
			result.add(new TokenRef(issuer, tokenType, jti, fingerprint));
		}
		return result;
	}

	/**
	 * Design §7.3: raw bearer tokens MUST NOT be stored. Any string member of a {@code tokens[]}
	 * entry that looks like a compact JWS/JWT is rejected outright, regardless of which field it
	 * appears under.
	 */
	private static void rejectRawTokenMaterial(JsonNode entry) {
		Iterator<Map.Entry<String, JsonNode>> fields = entry.fields();
		while (fields.hasNext()) {
			JsonNode value = fields.next().getValue();
			if (value.isTextual()) {
				String text = value.textValue();
				if (text.length() >= MIN_RAW_TOKEN_MATERIAL_LENGTH && COMPACT_JWS_PATTERN.matcher(text).matches()) {
					throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
							"raw_token_material");
				}
			}
		}
	}

	/**
	 * Design §7.3: an absolute URI with scheme {@code https}, or {@code http} only when the host is
	 * {@code localhost} (documented MVP relaxation for local/dev producers).
	 */
	private static void validateIssuer(String issuer, String path) {
		URI uri;
		try {
			uri = new URI(issuer);
		} catch (URISyntaxException ex) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "invalid:" + path);
		}
		String scheme = uri.getScheme();
		if (!uri.isAbsolute() || scheme == null) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "invalid:" + path);
		}
		if ("https".equalsIgnoreCase(scheme)) {
			return;
		}
		if ("http".equalsIgnoreCase(scheme) && "localhost".equalsIgnoreCase(uri.getHost())) {
			return;
		}
		throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "invalid:" + path);
	}

}
