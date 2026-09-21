/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Verifies Ed25519 (RFC 8032, PureEdDSA, no pre-hash, no context) signatures with the JCA
 * {@link Signature} of the installed BouncyCastle provider. Constant-time behaviour is the
 * provider's responsibility; this class never compares signature bytes itself beyond the length
 * check.
 *
 * <p>Stateless and thread-safe: a fresh {@link Signature} instance is created per call.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class Ed25519Verifier {

	/** Length of an Ed25519 signature (RFC 8032 §5.1.6). */
	public static final int SIGNATURE_LENGTH = 64;

	/**
	 * @param key the producer's public key, from {@link Ed25519PublicKeys}
	 * @param message the exact bytes that were signed (for TRACE: UTF-8 of the JCS form of the
	 *        assertion without {@code signature})
	 * @param signature the raw 64-byte signature
	 * @return {@code true} only if the provider accepts the signature; {@code false} for a null or
	 *         wrongly sized signature and for every signature the provider rejects
	 * @throws IllegalArgumentException if {@code key} or {@code message} is null
	 * @throws TraceCryptoException with reason {@code crypto_unavailable} if the provider has no
	 *         Ed25519 {@link Signature}, or {@code invalid_key} if it rejects the key
	 */
	public boolean verify(PublicKey key, byte[] message, byte[] signature) {
		if (key == null) {
			throw new IllegalArgumentException("key must not be null");
		}
		if (message == null) {
			throw new IllegalArgumentException("message must not be null");
		}
		if ((signature == null) || (signature.length != SIGNATURE_LENGTH)) {
			return false;
		}

		Provider provider = Ed25519PublicKeys.requireProvider();
		Signature verifier;
		try {
			verifier = Signature.getInstance(Ed25519PublicKeys.ALGORITHM, provider);
		} catch (NoSuchAlgorithmException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
					"Provider '" + provider.getName() + "' has no " + Ed25519PublicKeys.ALGORITHM + " Signature", ex);
		}

		try {
			verifier.initVerify(key);
		} catch (InvalidKeyException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_INVALID_KEY,
					"Provider '" + provider.getName() + "' rejected the verification key", ex);
		}

		try {
			verifier.update(message);
			return verifier.verify(signature);
		} catch (SignatureException ex) {
			// Malformed or invalid signature: a verification failure, not an error.
			return false;
		}
	}

}
