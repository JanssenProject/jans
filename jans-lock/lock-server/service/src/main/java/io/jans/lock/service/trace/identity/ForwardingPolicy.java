/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.identity;

import java.util.List;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.error.TraceValidationException;

/**
 * Enforces design decision D-3: a client may submit a record only if its binding's
 * {@code allowedProducerIds} contains the signed {@code producer} string byte-for-byte, or
 * contains {@code "*"}.
 *
 * <p>A pure, stateless check; no CDI bean is needed.
 *
 * @author Yuriy Movchan
 */
public final class ForwardingPolicy {

	static final String REASON_PRODUCER_NOT_ALLOWED = "producer_not_allowed";

	private static final String WILDCARD = "*";

	private ForwardingPolicy() {
	}

	/**
	 * @param ctx            the resolved request context (carries {@code allowedProducerIds})
	 * @param signedProducer the assertion's {@code producer} field, compared byte-for-byte
	 * @throws TraceValidationException {@code producer_not_allowed} (403) when the producer is not
	 *                                  in the allowlist and the allowlist does not contain {@code "*"}
	 */
	public static void check(TraceRequestContext ctx, String signedProducer) {
		List<String> allowed = ctx == null ? null : ctx.getAllowedProducerIds();
		if (allowed != null && (allowed.contains(WILDCARD) || allowed.contains(signedProducer))) {
			return;
		}

		throw new TraceValidationException(TraceErrorResponseType.PRODUCER_NOT_ALLOWED, REASON_PRODUCER_NOT_ALLOWED);
	}

}
