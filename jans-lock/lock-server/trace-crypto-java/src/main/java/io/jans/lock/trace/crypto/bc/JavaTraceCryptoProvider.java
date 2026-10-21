/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.bc;

import java.security.PublicKey;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoImpl;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceCryptoProviderType;
import io.jans.lock.trace.crypto.TraceSignedRecord;
import io.jans.lock.trace.crypto.TraceVerificationKey;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * {@link TraceCryptoProviderType#JAVA} implementation of {@link TraceCryptoProvider}: in-house
 * RFC 8785 JCS, Ed25519 through the installed BouncyCastle JCA provider, JDK SHA-256.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
@TraceCryptoImpl(TraceCryptoProviderType.JAVA)
public class JavaTraceCryptoProvider implements TraceCryptoProvider {

	private static final String SIGNATURE_MEMBER = "signature";

	@Inject
	private Ed25519Capability capability;

	@Inject
	private Ed25519Verifier verifier;

	public JavaTraceCryptoProvider() {
	}

	public JavaTraceCryptoProvider(Ed25519Capability capability, Ed25519Verifier verifier) {
		this.capability = capability;
		this.verifier = verifier;
	}

	@Override
	public TraceCryptoProviderType type() {
		return TraceCryptoProviderType.JAVA;
	}

	@Override
	public void requireAvailable() {
		capability.requireAvailable();
	}

	@Override
	public boolean isAvailable() {
		return capability.isAvailable();
	}

	@Override
	public String describe() {
		return "java/" + capability.getProviderName();
	}

	@Override
	public String canonicalize(JsonNode node) {
		try {
			return JcsCanonicalizer.canonicalize(node);
		} catch (JcsException ex) {
			throw canonicalizationFailed(ex);
		}
	}

	@Override
	public TraceVerificationKey parseKey(Map<String, String> jwk) {
		PublicKey key = Ed25519PublicKeys.fromJwk(jwk);
		return new JavaVerificationKey(key, Ed25519PublicKeys.thumbprint(jwk));
	}

	@Override
	public String verifyAndDigest(TraceSignedRecord record, byte[] signature, TraceVerificationKey key) {
		if (!(key instanceof JavaVerificationKey)) {
			throw new IllegalArgumentException("key was not created by " + JavaTraceCryptoProvider.class.getSimpleName());
		}
		JavaVerificationKey javaKey = (JavaVerificationKey) key;

		capability.requireAvailable();
		try {
			byte[] input = JcsCanonicalizer
					.canonicalizeToUtf8(JcsCanonicalizer.withoutField(record.getRoot(), SIGNATURE_MEMBER));
			if (!verifier.verify(javaKey.key, input, signature)) {
				throw new TraceCryptoException(TraceCryptoException.REASON_SIGNATURE_MISMATCH,
						"Ed25519 signature does not verify");
			}
			return CanonicalHashes.sha256PrefixedOfJcs(record.getRoot());
		} catch (JcsException ex) {
			throw canonicalizationFailed(ex);
		}
	}

	@Override
	public String receiptHash(String domainId, long receiptSequence, long receivedAtMs, String producerId,
			String recordId, String contentDigest, String prevReceiptHash) {
		return ReceiptHashes.receiptHash(domainId, receiptSequence, receivedAtMs, producerId, recordId,
				contentDigest, prevReceiptHash);
	}

	private static TraceCryptoException canonicalizationFailed(JcsException ex) {
		return new TraceCryptoException(TraceCryptoException.REASON_CANONICALIZATION_FAILED, ex.getMessage(), ex);
	}

	private static final class JavaVerificationKey implements TraceVerificationKey {

		private final PublicKey key;

		private final String thumbprint;

		private JavaVerificationKey(PublicKey key, String thumbprint) {
			this.key = key;
			this.thumbprint = thumbprint;
		}

		@Override
		public String thumbprint() {
			return thumbprint;
		}

	}

}
