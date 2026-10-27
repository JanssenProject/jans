/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.error;

import io.jans.lock.service.trace.store.TraceStore;

/**
 * Thrown by a {@link TraceStore} insert operation when the composite key already exists (a receipt-sequence race or a record identity race). A new
 * checked exception owned by this package: it is deliberately <strong>not</strong>
 * {@code io.jans.orm.exception.operation.DuplicateEntryException}, which the ORM store
 * cannot rely on for SQL backends (duplicate detection there is a
 * {@code contains(dn)} pre-check plus re-probe on failure, never this exception type in the ORM's
 * own cause chain).
 *
 * @author Yuriy Movchan
 */
public class DuplicateEntryException extends Exception {

	private static final long serialVersionUID = 1L;

	private final String key;

	public DuplicateEntryException(String key) {
		super("Entry already exists: " + key);
		this.key = key;
	}

	public DuplicateEntryException(String key, String message) {
		super(message);
		this.key = key;
	}

	public DuplicateEntryException(String key, String message, Throwable cause) {
		super(message, cause);
		this.key = key;
	}

	/**
	 * @return the composite key ({@code jansId} value) that already existed
	 */
	public String getKey() {
		return key;
	}

}
