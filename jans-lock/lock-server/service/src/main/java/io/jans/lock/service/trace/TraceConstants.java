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

	/** Prefix of every TRACE hash string; kept in sync with {@link CanonicalHashes#SHA256_PREFIX}. */
	public static final String HASH_PREFIX = CanonicalHashes.SHA256_PREFIX;

	/** The genesis predecessor sentinel: {@code "sha256:"} followed by 64 zero digits. */
	public static final String ZERO_HASH = HASH_PREFIX + repeat('0', 64);

	/** Matches the full {@code sha256:<64 lowercase hex>} hash format used throughout TRACE. */
	public static final Pattern HASH_PATTERN = Pattern.compile("^" + Pattern.quote(HASH_PREFIX) + "[0-9a-f]{64}$");

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

	private static String repeat(char c, int count) {
		char[] chars = new char[count];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	private TraceConstants() {
	}

}
