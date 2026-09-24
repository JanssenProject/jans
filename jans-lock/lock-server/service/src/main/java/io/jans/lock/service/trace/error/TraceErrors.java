/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.error;

import io.jans.lock.model.error.CommonErrorResponseType;
import io.jans.lock.model.error.ErrorResponseFactory;
import io.jans.lock.model.error.TraceErrorResponseType;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

/**
 * Translates the TRACE-pipeline runtime exceptions into the structured JSON error body produced by
 * {@link ErrorResponseFactory} (design decision D-10). This is a plain helper called by the TRACE
 * REST implementations (tasks 15, 20, 21) — it is <strong>not</strong> a JAX-RS
 * {@code ExceptionMapper}; Lock has none, and adding one would change the error behavior of
 * unrelated endpoints.
 *
 * @author Yuriy Movchan
 */
public final class TraceErrors {

	private TraceErrors() {
	}

	/**
	 * @param ex                    a {@link TraceValidationException}, {@link TraceConflictException},
	 *                              {@link TraceStorageException}, {@link TraceCryptoException}, or
	 *                              any other runtime exception (mapped to a generic 500)
	 * @param errorResponseFactory  the configured factory (carries the persisted error messages and
	 *                              {@code errorReasonEnabled})
	 * @return a {@link WebApplicationException} ready to be thrown by the caller
	 */
	public static WebApplicationException toWebApplicationException(RuntimeException ex,
			ErrorResponseFactory errorResponseFactory) {

		if (ex instanceof TraceValidationException) {
			TraceValidationException e = (TraceValidationException) ex;
			return errorResponseFactory.traceException(e.getErrorId(), e.getReason());
		}

		if (ex instanceof TraceConflictException) {
			TraceConflictException e = (TraceConflictException) ex;
			return errorResponseFactory.traceException(e.getErrorId(), e.getReason());
		}

		if (ex instanceof TraceStorageException) {
			TraceStorageException e = (TraceStorageException) ex;
			return errorResponseFactory.traceException(TraceErrorResponseType.STORAGE_FAILURE, e.getReason(), e.getCause());
		}

		if (ex instanceof TraceCryptoException) {
			TraceCryptoException e = (TraceCryptoException) ex;
			TraceErrorResponseType type = TraceCryptoException.REASON_CRYPTO_UNAVAILABLE.equals(e.getReason())
					? TraceErrorResponseType.CRYPTO_UNAVAILABLE
					: TraceErrorResponseType.INVALID_KEY;
			return errorResponseFactory.traceException(type, e.getReason());
		}

		return errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR,
				CommonErrorResponseType.UNKNOWN_ERROR, ex.getClass().getSimpleName());
	}

}
