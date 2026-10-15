/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.TraceIssuers;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.model.TokenRef;

/**
 * Field-level checks shared by more than one {@link EventKindValidator}: the {@code decision} /
 * {@code invocation} fact shapes used by {@code AUTHORIZATION_DECISION} and
 * {@code CAPABILITY_INVOKED} (design §7.2), the {@code trace.subject} rules (design §7.1), and the
 * {@code tokens[]} shape common to every kind (design §7.3).
 *
 * @author Yuriy Movchan
 */
final class EventKindValidationSupport {

	static final Set<String> DECISION_OUTCOMES = Collections
			.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("ALLOW", "DENY")));

	static final Set<String> INVOCATION_OUTCOMES = Collections
			.unmodifiableSet(new LinkedHashSet<>(Arrays.asList("SUCCESS", "FAILURE")));

	private static final int MAX_FACT_TEXT_LENGTH = 512;

	private static final int MAX_FACT_ID_LENGTH = 256;

	private static final int MAX_SUBJECT_ID_LENGTH = 255;

	private static final int MAX_TOKEN_TYPE_LENGTH = 64;

	private static final int MAX_JTI_LENGTH = 256;

	/** {@code ^[A-Za-z0-9_-]{16,128}$} — the generic opaque-fingerprint form (design §7.3). */
	private static final Pattern FINGERPRINT_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{16,128}$");

	/**
	 * Compact JWS (3 segments) or compact JWE (5 segments; the encrypted-key segment may be empty).
	 */
	private static final Pattern COMPACT_JWS_PATTERN = Pattern
			.compile("^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$"
					+ "|^[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]*\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+$");

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

	/**
	 * Validates the optional operation-binding digests of an event (GovOps profile §8): each
	 * present member must be a {@code sha256:<64 lowercase hex>} string, the same rule as
	 * {@code RUNTIME_EFFECT.result_digest}.
	 */
	static void optionalDigests(JsonNode event, String... fields) {
		for (String field : fields) {
			optionalHashString(event, field, "trace.event." + field);
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
	 * Validates the signed capability <em>facts</em> of a decision or invocation event (design §7.2): exactly one of a singular object {@code singular} or a non-empty array
	 * {@code plural} of entries. Every fact carries {@code action} and {@code resource_type}
	 * (non-empty, at most {@value #MAX_FACT_TEXT_LENGTH} characters) and an {@code outcome}; plural
	 * entries also carry a unique {@code idField}. A {@code resource_id} member is forbidden (the
	 * facts name a resource <em>type</em>, never an instance). The record-level
	 * {@code trace.event.outcome} and the removed {@code capability_ids} are forbidden, because the
	 * outcome lives in the facts and Lock does not resolve capability ids.
	 *
	 * <p>Nothing is returned or stored: the facts remain in the signed assertion only.
	 *
	 * @param outcomes the closed outcome vocabulary of the kind ({@link #DECISION_OUTCOMES} or
	 *        {@link #INVOCATION_OUTCOMES})
	 */
	static void requireCapabilityFacts(JsonNode event, String singular, String plural, String idField,
			Set<String> outcomes) {
		forbidField(event, "capability_ids", "trace.event.capability_ids");
		forbidField(event, "outcome", "trace.event.outcome");

		boolean hasSingular = present(event, singular);
		boolean hasPlural = present(event, plural);
		if (hasSingular && hasPlural) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"forbidden:trace.event." + plural);
		}
		if (!hasSingular && !hasPlural) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"missing:trace.event." + singular);
		}

		if (hasSingular) {
			String path = "trace.event." + singular;
			JsonNode fact = event.get(singular);
			if (!fact.isObject()) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
			}
			validateFact(fact, path, outcomes);
			return;
		}

		String path = "trace.event." + plural;
		JsonNode entries = event.get(plural);
		if (!entries.isArray()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		if (entries.size() == 0) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "empty:" + path);
		}
		Set<String> seenIds = new HashSet<>();
		for (int i = 0; i < entries.size(); i++) {
			JsonNode entry = entries.get(i);
			String entryPath = path + "[" + i + "]";
			if (!entry.isObject()) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"type:" + entryPath);
			}
			String id = requireNonEmptyString(entry, idField, entryPath + "." + idField, MAX_FACT_ID_LENGTH);
			validateFact(entry, entryPath, outcomes);
			if (!seenIds.add(id)) {
				throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
						"duplicate:" + entryPath + "." + idField);
			}
		}
	}

	static boolean present(JsonNode parent, String field) {
		JsonNode node = parent.get(field);
		return node != null && !node.isNull();
	}

	private static void validateFact(JsonNode fact, String path, Set<String> outcomes) {
		forbidField(fact, "resource_id", path + ".resource_id");
		requireNonEmptyString(fact, "action", path + ".action", MAX_FACT_TEXT_LENGTH);
		requireNonEmptyString(fact, "resource_type", path + ".resource_type", MAX_FACT_TEXT_LENGTH);
		requireEnum(fact, "outcome", path + ".outcome", outcomes);
	}

	/**
	 * {@code trace.subject} for event kinds that identify the acting workload (design §7.1):
	 * {@code workload_id} is required, {@code workload_instance_id} optional. An absent subject is
	 * reported as the missing {@code workload_id}.
	 */
	static void requireWorkloadSubject(JsonNode trace) {
		JsonNode subject = trace.get("subject");
		if (subject == null || subject.isNull()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION,
					"missing:trace.subject.workload_id");
		}
		if (!subject.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:trace.subject");
		}
		requireNonEmptyString(subject, "workload_id", "trace.subject.workload_id", MAX_SUBJECT_ID_LENGTH);
		optionalBoundedString(subject, "workload_instance_id", "trace.subject.workload_instance_id",
				MAX_SUBJECT_ID_LENGTH);
	}

	/**
	 * {@code trace.subject} for event kinds where the subject is optional (design §7.1): validated
	 * only when present.
	 */
	static void validateOptionalSubject(JsonNode trace) {
		JsonNode subject = trace.get("subject");
		if (subject == null || subject.isNull()) {
			return;
		}
		if (!subject.isObject()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:trace.subject");
		}
		JsonNode workloadId = subject.get("workload_id");
		if (workloadId != null && !workloadId.isNull()) {
			requireNonEmptyString(subject, "workload_id", "trace.subject.workload_id", MAX_SUBJECT_ID_LENGTH);
		}
		optionalBoundedString(subject, "workload_instance_id", "trace.subject.workload_instance_id",
				MAX_SUBJECT_ID_LENGTH);
	}

	private static void optionalBoundedString(JsonNode parent, String field, String path, int maxLength) {
		JsonNode node = parent.get(field);
		if (node == null || node.isNull()) {
			return;
		}
		if (!node.isTextual()) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "type:" + path);
		}
		if (node.textValue().length() > maxLength) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "length:" + path);
		}
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

	/** Design §7.3: see {@link TraceIssuers#isValid(String)}. */
	private static void validateIssuer(String issuer, String path) {
		if (!TraceIssuers.isValid(issuer)) {
			throw new TraceValidationException(TraceValidationException.ERROR_INVALID_ASSERTION, "invalid:" + path);
		}
	}

}
