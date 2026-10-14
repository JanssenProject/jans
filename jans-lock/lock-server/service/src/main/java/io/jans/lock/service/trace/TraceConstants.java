/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

import io.jans.lock.service.trace.canon.CanonicalHashes;

/**
 * Shared TRACE wire-format constants: the EAT profile tag, the {@code sha256:<64 hex>} hash
 * format, the MVP event-kind catalog (design §7.2) and the two field names Lock treats specially
 * on every incoming assertion.
 *
 * @author Yuriy Movchan
 */
public final class TraceConstants {

	/** {@code trace.eat_profile} value the MVP accepts (design §7.1). */
	public static final String EAT_PROFILE = "tag:jans.io,2026:trace-v1";

	/**
	 * Receipt-commitment profile tag (GovOps profile §11). It is the {@code receipt_profile} member
	 * of every receipt-hash input; a different receipt-hash layout needs a new tag.
	 */
	public static final String RECEIPT_PROFILE = "tag:jans.io,2026:lock-trace-receipt-v1";

	/** Prefix of every TRACE hash string; kept in sync with {@link CanonicalHashes#SHA256_PREFIX}. */
	public static final String HASH_PREFIX = CanonicalHashes.SHA256_PREFIX;

	/** The genesis predecessor sentinel: {@code "sha256:"} followed by 64 zero digits. */
	public static final String ZERO_HASH = HASH_PREFIX + repeat('0', 64);

	/** Matches the full {@code sha256:<64 lowercase hex>} hash format used throughout TRACE. */
	public static final Pattern HASH_PATTERN = Pattern.compile("^" + Pattern.quote(HASH_PREFIX) + "[0-9a-f]{64}$");

	/**
	 * Opaque stable producer id (design D-17): 1 to 255 ASCII characters, starting with a letter or
	 * digit, then letters, digits and {@code . _ : / -}. Lock does not interpret the value, never
	 * normalizes it and compares it byte-for-byte; software version travels separately in the
	 * signed {@code producer_version} member. Shared by assertion validation and by producer-key and
	 * producer-chain registration, so a registered producer id can always appear in a signed
	 * assertion.
	 */
	public static final Pattern PRODUCER_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:/-]{0,254}$");

	/** Name of the optional top-level signed member that carries the producer software version. */
	public static final String PRODUCER_VERSION_FIELD = "producer_version";

	/** Maximum length of the {@code producer_version} member. */
	public static final int MAX_PRODUCER_VERSION_LENGTH = 255;

	/** {@code trace.event_kind}: an authorization decision by a PDP such as Cedarling. */
	public static final String EVENT_KIND_AUTHORIZATION_DECISION = "AUTHORIZATION_DECISION";

	/** {@code trace.event_kind}: a capability invocation by a PEP or gateway. */
	public static final String EVENT_KIND_CAPABILITY_INVOKED = "CAPABILITY_INVOKED";

	/** {@code trace.event_kind}: an observed runtime effect. */
	public static final String EVENT_KIND_RUNTIME_EFFECT = "RUNTIME_EFFECT";

	/** The MVP event-kind catalog (design §7.2); no other value is accepted. */
	public static final Set<String> EVENT_KINDS = Collections.unmodifiableSet(new LinkedHashSet<>(
			Arrays.asList(EVENT_KIND_AUTHORIZATION_DECISION, EVENT_KIND_CAPABILITY_INVOKED, EVENT_KIND_RUNTIME_EFFECT)));

	/** Name of the top-level signature field, excluded from the JCS signature input. */
	public static final String SIGNATURE_FIELD = "signature";

	/** Name of the field that is forbidden anywhere in an incoming assertion (design D-1). */
	public static final String DOMAIN_FIELD = "evidence_domain_id";

	/**
	 * Members Lock derives, assigns or resolves itself (design §4.1, §10; GovOps profile §1, §12).
	 * A producer must never sign any of them, so an assertion carrying one at the top level, in
	 * {@code trace}, in {@code trace.event} or in a {@code decision}/{@code invocation} fact is
	 * rejected as {@code invalid_assertion} with reason {@code forbidden:<path>}.
	 * {@link #DOMAIN_FIELD} has its own error and is not listed here. The profile names no
	 * members for the verification vector or the settlement, completeness, assurance, lineage,
	 * delegation, instance-binding and external-anchor assessments, so none is listed for them.
	 */
	public static final Set<String> LOCK_DERIVED_MEMBERS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
			"content_digest", "receipt_sequence", "received_at", "prev_receipt_hash",
			"coverage_gap_flag", "chain_link_failure_flag", "equivocation_flag", "late_flag",
			"unattributed_flag", "candidate_flag",
			"key_resolved", "key_temporally_valid", "key_not_revoked", "key_authorized_for_producer",
			"producer_authorized_for_claim", "key_thumbprint", "key_authorization_ref", "verified_at",
			"trust_tier", "capability_resolution", "capability_id", "capability_ids", "cnf")));

	private static String repeat(char c, int count) {
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	private TraceConstants() {
	}

}
