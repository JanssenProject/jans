/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.error;

import io.jans.lock.service.trace.crypto.Ed25519Verifier;

/**
 * Thrown by the TRACE crypto layer when a key cannot be built from a JWK or when the security
 * provider cannot perform Ed25519 operations. Never thrown for a signature that merely fails to
 * verify (that is a {@code false} from {@link Ed25519Verifier#verify}).
 *
 * <p>The {@link #getReason() reason} is a short stable token intended for the structured error
 * body ({@code reason} member); the message is for logs.
 *
 * @author Yuriy Movchan
 */
public class TraceCryptoException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	/** The provider lacks Ed25519 support or is not installed (fail closed). */
	public static final String REASON_CRYPTO_UNAVAILABLE = "crypto_unavailable";

	/** JWK {@code kty} is missing or not {@code OKP}. */
	public static final String REASON_JWK_KTY = "jwk_kty";

	/** JWK {@code crv} is missing or not {@code Ed25519}. */
	public static final String REASON_JWK_CRV = "jwk_crv";

	/** JWK {@code x} does not decode to exactly 32 bytes. */
	public static final String REASON_JWK_X_LENGTH = "jwk_x_length";

	/** JWK {@code x} is missing, not a string, or not strict unpadded base64url. */
	public static final String REASON_JWK_X_ENCODING = "jwk_x_encoding";

	/** JWK carries a {@code d} member (private key material must never be registered). */
	public static final String REASON_JWK_PRIVATE_MATERIAL = "jwk_private_material";

	/** JWK is null or not a JSON object. */
	public static final String REASON_JWK_FORMAT = "jwk_format";

	/** The provider rejected the key (not an Ed25519 key usable for verification). */
	public static final String REASON_INVALID_KEY = "invalid_key";

	private final String reason;

	public TraceCryptoException(String reason, String message) {
		super(message);
		this.reason = reason;
	}

	public TraceCryptoException(String reason, String message, Throwable cause) {
		super(message, cause);
		this.reason = reason;
	}

	/**
	 * @return short stable reason token, e.g. {@code jwk_x_length} or {@code crypto_unavailable}
	 */
	public String getReason() {
		return reason;
	}

}
