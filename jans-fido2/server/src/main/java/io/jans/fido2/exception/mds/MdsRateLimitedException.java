/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.exception.mds;

import io.jans.fido2.exception.Fido2RuntimeException;

/**
 * Raised when the FIDO Metadata Service answers a TOC download with HTTP 429 (Too Many Requests).
 * <p>
 * This is deliberately distinct from a generic download failure: a 429 is the endpoint explicitly
 * asking us to stop, so retrying it immediately makes the situation worse rather than better. Callers
 * that retry failed downloads should treat this exception as "do not retry now" and honour
 * {@link #getRetryAfterSeconds()} when it is present.
 */
public class MdsRateLimitedException extends Fido2RuntimeException {

	private static final long serialVersionUID = 8258821405431245933L;

	private final transient Integer retryAfterSeconds;

	public MdsRateLimitedException(String errorMessage, Integer retryAfterSeconds) {
		super(errorMessage);
		this.retryAfterSeconds = retryAfterSeconds;
	}

	/**
	 * @return the delay advertised by the server's {@code Retry-After} header, or {@code null} when the
	 *         header was absent or unparseable.
	 */
	public Integer getRetryAfterSeconds() {
		return retryAfterSeconds;
	}
}
