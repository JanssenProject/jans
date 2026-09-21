/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.util.security.SecurityProviderUtility;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Startup probe for Ed25519 support in the installed BouncyCastle provider. The standard WAR
 * ships {@code bcprov}, the FIPS WAR ships {@code bc-fips}; whether the latter exposes Ed25519
 * is decided at runtime here, and TRACE ingestion fails closed (500 {@code crypto_unavailable})
 * when it does not. Observed with bc-fips 1.0.2.4: Ed25519 is available in the default mode but
 * absent when the JVM runs with {@code -Dorg.bouncycastle.fips.approved_only=true}.
 *
 * <p>The probe generates a throwaway key pair, signs a fixed message, rebuilds the public key
 * from its raw bytes through {@link Ed25519PublicKeys} (exercising the {@code KeyFactory} path),
 * verifies the signature with {@link Ed25519Verifier}, and checks that a corrupted signature is
 * rejected. Only the outcome and the provider name are logged.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class Ed25519Capability {

	private static final Logger LOG = LoggerFactory.getLogger(Ed25519Capability.class);

	private static final byte[] PROBE_MESSAGE = "jans-lock trace ed25519 capability probe"
			.getBytes(StandardCharsets.UTF_8);

	private static final int SPKI_LENGTH = 44;

	private volatile boolean available;

	private volatile String providerName;

	private volatile String failure;

	/**
	 * Runs the probe. Safe to call again; the last result wins. Never throws.
	 */
	@PostConstruct
	public void probe() {
		Provider provider = SecurityProviderUtility.getBCProvider();
		if (provider == null) {
			record(false, null, "BouncyCastle provider is not installed");
			return;
		}

		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance(Ed25519PublicKeys.ALGORITHM, provider);
			KeyPair pair = generator.generateKeyPair();

			Signature signer = Signature.getInstance(Ed25519PublicKeys.ALGORITHM, provider);
			signer.initSign(pair.getPrivate());
			signer.update(PROBE_MESSAGE);
			byte[] signature = signer.sign();
			if (signature.length != Ed25519Verifier.SIGNATURE_LENGTH) {
				record(false, provider.getName(), "unexpected signature length " + signature.length);
				return;
			}

			PublicKey rebuilt = Ed25519PublicKeys.fromRaw(rawKeyOf(pair.getPublic()));
			Ed25519Verifier verifier = new Ed25519Verifier();
			if (!verifier.verify(rebuilt, PROBE_MESSAGE, signature)) {
				record(false, provider.getName(), "self-signed probe message did not verify");
				return;
			}

			byte[] corrupted = signature.clone();
			corrupted[0] ^= 0x01;
			if (verifier.verify(rebuilt, PROBE_MESSAGE, corrupted)) {
				record(false, provider.getName(), "corrupted signature verified");
				return;
			}

			record(true, provider.getName(), null);
		} catch (GeneralSecurityException | RuntimeException ex) {
			record(false, provider.getName(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
			LOG.debug("Ed25519 capability probe failure detail", ex);
		}
	}

	/**
	 * @throws TraceCryptoException with reason {@code crypto_unavailable} unless the last probe
	 *         succeeded
	 */
	public void requireAvailable() {
		if (!available) {
			throw new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
					"Ed25519 is not available in provider '" + providerName + "': " + failure);
		}
	}

	public boolean isAvailable() {
		return available;
	}

	/**
	 * @return the provider name the probe ran against, or {@code null} if none was installed
	 */
	public String getProviderName() {
		return providerName;
	}

	/**
	 * @return a short description of why the probe failed, or {@code null} if it passed
	 */
	public String getFailure() {
		return failure;
	}

	private void record(boolean ok, String provider, String reason) {
		this.available = ok;
		this.providerName = provider;
		this.failure = reason;
		if (ok) {
			LOG.info("Ed25519 capability probe: provider={} available=true", provider);
		} else {
			LOG.error("Ed25519 capability probe: provider={} available=false ({}); TRACE ingestion will fail closed",
					provider, reason);
		}
	}

	/**
	 * Extracts the raw 32-byte key from the SubjectPublicKeyInfo encoding a JCA provider returns
	 * for an Ed25519 public key, without touching provider-specific classes.
	 */
	private static byte[] rawKeyOf(PublicKey key) {
		byte[] encoded = key.getEncoded();
		if ((encoded == null) || (encoded.length != SPKI_LENGTH)) {
			throw new IllegalStateException(
					"unexpected Ed25519 SubjectPublicKeyInfo length " + ((encoded == null) ? 0 : encoded.length));
		}
		byte[] raw = new byte[Ed25519PublicKeys.RAW_KEY_LENGTH];
		System.arraycopy(encoded, SPKI_LENGTH - Ed25519PublicKeys.RAW_KEY_LENGTH, raw, 0, raw.length);
		return raw;
	}

}
