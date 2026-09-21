/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.error;

/**
 * Thrown by the TRACE store/ingestion layer when a persistence operation fails or a
 * multi-node allocation protocol (design decision D-8) gives up after its retry limit. Always maps
 * to {@code storage_failure} / HTTP 500 (design decision D-10); see
 * {@link TraceErrors#toWebApplicationException}.
 *
 * <p>{@link #getReason()} is a short stable token for the structured error body's {@code reason}
 * member; it never carries request-body content, tokens or stack traces.
 *
 * @author Yuriy Movchan
 */
public class TraceStorageException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final String reason;

	public TraceStorageException(String reason, String message) {
		super(message);
		this.reason = reason;
	}

	public TraceStorageException(String reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	/**
	 * @return short stable reason token, e.g. {@code receipt_allocation_retry_exhausted}
	 */
	public String getReason() {
		return reason;
	}

}
