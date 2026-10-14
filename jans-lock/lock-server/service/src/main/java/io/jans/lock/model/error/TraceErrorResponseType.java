/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.error;

import io.jans.as.model.error.IErrorType;
import jakarta.ws.rs.core.Response;

import static jakarta.ws.rs.core.Response.Status.BAD_REQUEST;
import static jakarta.ws.rs.core.Response.Status.CONFLICT;
import static jakarta.ws.rs.core.Response.Status.FORBIDDEN;
import static jakarta.ws.rs.core.Response.Status.INTERNAL_SERVER_ERROR;
import static jakarta.ws.rs.core.Response.Status.NOT_FOUND;

/**
 * TRACE error family (TRACE MVP design decision D-10). Every id and HTTP status here is normative;
 * do not add, remove or renumber an id without updating design decision D-10 first.
 *
 * @author Yuriy Movchan
 */
public enum TraceErrorResponseType implements IErrorType {

	/** Malformed JSON, limits exceeded, unreadable body, or bad query params. */
	INVALID_REQUEST("invalid_request", BAD_REQUEST),

	/** A design §7.1/§7.2/§7.3 violation; the structured error's {@code reason} names the field. */
	INVALID_ASSERTION("invalid_assertion", BAD_REQUEST),

	/** {@code trace.eat_profile} is not {@code tag:jans.io,2026:trace-v1}. */
	UNSUPPORTED_PROFILE("unsupported_profile", BAD_REQUEST),

	/** The body carries a top-level or {@code trace}-level {@code evidence_domain_id}. */
	DOMAIN_FIELD_FORBIDDEN("domain_field_forbidden", BAD_REQUEST),

	/** {@code producer} does not equal {@code producer_chain.producer_id}. */
	PRODUCER_MISMATCH("producer_mismatch", BAD_REQUEST),

	/** No registered key for {@code (domain, producer, kid)}. */
	UNKNOWN_PRODUCER_KEY("unknown_producer_key", BAD_REQUEST),

	/** The key is outside its validity window or has been revoked. */
	KEY_NOT_VALID("key_not_valid", BAD_REQUEST),

	/** Ed25519 verification failed, or the signature is not well-formed base64url. */
	INVALID_SIGNATURE("invalid_signature", BAD_REQUEST),

	/** The producer's chain is not pre-registered (design decision D-5). */
	CHAIN_NOT_REGISTERED("chain_not_registered", BAD_REQUEST),

	/** The record's genesis position or previous-record hash is inconsistent (design decision D-5). */
	INVALID_GENESIS("invalid_genesis", BAD_REQUEST),

	/** The authenticated client resolves to no evidence domain (design decisions D-1/D-2). */
	CLIENT_NOT_BOUND("client_not_bound", FORBIDDEN),

	/** The authenticated client's binding does not allow the signed producer (design decision D-3). */
	PRODUCER_NOT_ALLOWED("producer_not_allowed", FORBIDDEN),

	/** GET record: no match in the domain. */
	RECORD_NOT_FOUND("record_not_found", NOT_FOUND),

	/** GET execution: no records in the domain. */
	EXECUTION_NOT_FOUND("execution_not_found", NOT_FOUND),

	/** Same identity already exists with a different {@code content_digest}. */
	RECORD_CONFLICT("record_conflict", CONFLICT),

	/** A bare {@code record_id}/{@code trace_execution_id} matches more than one producer/authority. */
	AMBIGUOUS_IDENTIFIER("ambiguous_identifier", CONFLICT),

	/** Admin: duplicate key registration. */
	KEY_ALREADY_EXISTS("key_already_exists", CONFLICT),

	/** Admin: duplicate chain registration. */
	CHAIN_ALREADY_EXISTS("chain_already_exists", CONFLICT),

	/** Admin: the JWK is not {@code {kty:OKP, crv:Ed25519, x:<32 bytes b64url>}} or has bad dates. */
	INVALID_KEY("invalid_key", BAD_REQUEST),

	/** Persistence failure, or receipt-allocation retry exhausted. */
	STORAGE_FAILURE("storage_failure", INTERNAL_SERVER_ERROR),

	/** The security provider lacks Ed25519 support (fail closed). */
	CRYPTO_UNAVAILABLE("crypto_unavailable", INTERNAL_SERVER_ERROR),
	;

	private final String paramName;

	private final Response.Status httpStatus;

	TraceErrorResponseType(String paramName, Response.Status httpStatus) {
		this.paramName = paramName;
		this.httpStatus = httpStatus;
	}

	@Override
	public String getParameter() {
		return paramName;
	}

	/**
	 * @return the HTTP status fixed for this id by design decision D-10
	 */
	public Response.Status httpStatus() {
		return httpStatus;
	}

	/**
	 * @param param a candidate error id
	 * @return the matching {@link TraceErrorResponseType}, or {@code null} if none matches
	 */
	public static TraceErrorResponseType fromString(String param) {
		if (param != null) {
			for (TraceErrorResponseType type : values()) {
				if (param.equals(type.paramName)) {
					return type;
				}
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return paramName;
	}

}
