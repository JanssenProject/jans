/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.error;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceRecordEntry;

/**
 * Thrown when an identity that must be unique already exists (design decision D-8 step 5) or a
 * bare identifier resolves to more than one candidate in a domain (design §14). Maps to one of two
 * {@link TraceErrorResponseType}s:
 * <ul>
 * <li>{@link TraceErrorResponseType#RECORD_CONFLICT} — same identity, different
 * {@code content_digest}; {@link #getExistingRecord()} carries the row that already won.</li>
 * <li>{@link TraceErrorResponseType#AMBIGUOUS_IDENTIFIER} — a bare {@code record_id}/
 * {@code trace_execution_id} matches more than one producer/authority; {@link #getExistingRecord()}
 * is {@code null} since there is no single existing row to return.</li>
 * </ul>
 *
 * <p>{@link #getReason()} never carries request-body content, tokens or stack traces.
 *
 * @author Yuriy Movchan
 */
public class TraceConflictException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final TraceErrorResponseType errorId;

	private final String reason;

	private final TraceRecordEntry existingRecord;

	public TraceConflictException(TraceErrorResponseType errorId, String reason) {
		this(errorId, reason, null);
	}

	public TraceConflictException(TraceErrorResponseType errorId, String reason, TraceRecordEntry existingRecord) {
		super(errorId + ": " + reason);
		this.errorId = errorId;
		this.reason = reason;
		this.existingRecord = existingRecord;
	}

	/**
	 * @return {@link TraceErrorResponseType#RECORD_CONFLICT} or
	 *         {@link TraceErrorResponseType#AMBIGUOUS_IDENTIFIER}
	 */
	public TraceErrorResponseType getErrorId() {
		return errorId;
	}

	/**
	 * @return short stable reason token, never request-body content
	 */
	public String getReason() {
		return reason;
	}

	/**
	 * @return the record row that already holds this identity, or {@code null} when none applies
	 *         (e.g. an ambiguous-identifier match)
	 */
	public TraceRecordEntry getExistingRecord() {
		return existingRecord;
	}

}
