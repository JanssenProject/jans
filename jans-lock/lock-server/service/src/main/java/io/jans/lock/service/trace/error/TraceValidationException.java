/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.error;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.TraceAssertionParser;

/**
 * Thrown by {@link TraceAssertionParser} and {@link CommonAssertionValidator} when an incoming
 * TRACE request fails bounded parsing or §7.1 common-field validation.
 *
 * <p>{@link #getErrorId()} carries the {@link TraceErrorResponseType} the structured error catalog
 * (task 10) maps to a status and JSON body; see {@code TraceErrors.toWebApplicationException}. The
 * {@link #ERROR_*} constants here are the subset of {@link TraceErrorResponseType} values this
 * class's callers throw.
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
	public static final TraceErrorResponseType ERROR_INVALID_REQUEST = TraceErrorResponseType.INVALID_REQUEST;

	/** A §7.1/§7.2 common-field violation; {@link #getReason()} names the offending field. */
	public static final TraceErrorResponseType ERROR_INVALID_ASSERTION = TraceErrorResponseType.INVALID_ASSERTION;

	/** {@code trace.eat_profile} is not the supported TRACE profile. */
	public static final TraceErrorResponseType ERROR_UNSUPPORTED_PROFILE = TraceErrorResponseType.UNSUPPORTED_PROFILE;

	/** The body carries a top-level or {@code trace}-level {@code evidence_domain_id} (design D-1). */
	public static final TraceErrorResponseType ERROR_DOMAIN_FIELD_FORBIDDEN = TraceErrorResponseType.DOMAIN_FIELD_FORBIDDEN;

	/** {@code producer} does not equal {@code producer_chain.producer_id} byte-for-byte. */
	public static final TraceErrorResponseType ERROR_PRODUCER_MISMATCH = TraceErrorResponseType.PRODUCER_MISMATCH;

	/** {@code signature} is not a well-formed base64url-encoded 64-byte value. */
	public static final TraceErrorResponseType ERROR_INVALID_SIGNATURE = TraceErrorResponseType.INVALID_SIGNATURE;

	private final TraceErrorResponseType errorId;

	private final String reason;

	public TraceValidationException(TraceErrorResponseType errorId, String reason) {
		super(errorId + ": " + reason);
		this.errorId = errorId;
		this.reason = reason;
	}

	public TraceValidationException(TraceErrorResponseType errorId, String reason, Throwable cause) {
		super(errorId + ": " + reason, cause);
		this.errorId = errorId;
		this.reason = reason;
	}

	/**
	 * @return the {@link TraceErrorResponseType} this violation maps to
	 */
	public TraceErrorResponseType getErrorId() {
		return errorId;
	}

	/**
	 * @return a machine-readable, field-name-only detail, never request-body content
	 */
	public String getReason() {
		return reason;
	}

}
