/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import java.util.Objects;

/**
 * The result of one element of {@link TraceIngestionService#ingestBatch}: either the
 * {@link AcceptanceResult} or the exception {@link TraceIngestionService#ingest} would have thrown
 * for that element. Exactly one of the two is present.
 *
 * @author Yuriy Movchan
 */
public final class BatchOutcome {

	private final AcceptanceResult result;

	private final RuntimeException error;

	private BatchOutcome(AcceptanceResult result, RuntimeException error) {
		this.result = result;
		this.error = error;
	}

	public static BatchOutcome accepted(AcceptanceResult result) {
		return new BatchOutcome(Objects.requireNonNull(result, "result"), null);
	}

	public static BatchOutcome failed(RuntimeException error) {
		return new BatchOutcome(null, Objects.requireNonNull(error, "error"));
	}

	public boolean isAccepted() {
		return result != null;
	}

	/** @return the result, or {@code null} when the element failed */
	public AcceptanceResult getResult() {
		return result;
	}

	/** @return the failure, or {@code null} when the element was accepted */
	public RuntimeException getError() {
		return error;
	}

	@Override
	public String toString() {
		return result != null ? "BatchOutcome[" + result + "]" : "BatchOutcome[error=" + error + "]";
	}

}
