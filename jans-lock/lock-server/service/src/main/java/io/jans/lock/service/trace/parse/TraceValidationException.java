/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.parse;

/**
 * Thrown by {@link TraceAssertionParser} and {@link CommonAssertionValidator} when an incoming
 * TRACE request fails bounded parsing or §7.1 common-field validation.
 *
 * <p>Task 10 (structured error catalog) has not landed yet, so {@link #getErrorId()} is a plain
 * string rather than the future {@code TraceErrorResponseType} enum; once task 10 lands, callers
 * should switch to that enum and this class should carry it directly instead of a string id. The
 * {@link #ERROR_*} constants here mirror the ids defined in design decision D-10.
 *
 * <p>{@link #getReason()} never carries request-body content: only field names/paths (e.g.
 * {@code missing:trace.signed_at}, {@code record_id_format}), consistent with the rule that raw
 * bodies are never included in exception messages or logs.
 *
 * @author Yuriy Movchan
 */
public class TraceValidationException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** Malformed JSON, limits exceeded, unreadable body, or a body that decodes malformed UTF-8. */
	public static final String ERROR_INVALID_REQUEST = "invalid_request";

	/** A §7.1/§7.2 common-field violation; {@link #getReason()} names the offending field. */
	public static final String ERROR_INVALID_ASSERTION = "invalid_assertion";

	/** {@code trace.eat_profile} is not the supported TRACE profile. */
	public static final String ERROR_UNSUPPORTED_PROFILE = "unsupported_profile";

	/** The body carries a top-level or {@code trace}-level {@code evidence_domain_id} (design D-1). */
	public static final String ERROR_DOMAIN_FIELD_FORBIDDEN = "domain_field_forbidden";

	/** {@code producer} does not equal {@code producer_chain.producer_id} byte-for-byte. */
	public static final String ERROR_PRODUCER_MISMATCH = "producer_mismatch";

	/** {@code signature} is not a well-formed base64url-encoded 64-byte value. */
	public static final String ERROR_INVALID_SIGNATURE = "invalid_signature";

	private final String errorId;

	private final String reason;

	public TraceValidationException(String errorId, String reason) {
		super(errorId + ": " + reason);
		this.errorId = errorId;
		this.reason = reason;
	}

	public TraceValidationException(String errorId, String reason, Throwable cause) {
		super(errorId + ": " + reason, cause);
		this.errorId = errorId;
		this.reason = reason;
	}

	/**
	 * @return one of the {@code ERROR_*} constants (a {@code TraceErrorResponseType} id once task
	 *         10 lands)
	 */
	public String getErrorId() {
		return errorId;
	}

	/**
	 * @return a machine-readable, field-name-only detail, never request-body content
	 */
	public String getReason() {
		return reason;
	}

}
