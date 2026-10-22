/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The crypto operations of TRACE ingestion: canonicalization, key parsing, signature
 * verification with content digest, and receipt hashing. Select an implementation with
 * {@link TraceCryptoImpl}.
 *
 * <p><b>Scopes.</b> The Ed25519 signature covers the JCS (RFC 8785) form of the record
 * <em>without</em> its top-level {@code signature} member. The {@code content_digest} covers the
 * JCS form of the <em>complete</em> record, {@code signature} included.
 *
 * <p><b>Receipt hash.</b> {@code "sha256:" + hex(SHA-256(JCS(m)))} where {@code m} has exactly
 * these members: {@code receipt_profile} ({@link TraceHashes#RECEIPT_PROFILE}),
 * {@code evidence_domain_id}, {@code receipt_sequence} (JSON integer), {@code received_at}
 * (RFC 3339 UTC string with three fraction digits, see {@link TraceTime}), {@code producer_id},
 * {@code record_id}, {@code content_digest} and {@code prev_receipt_hash}.
 *
 * <p><b>Thread safety.</b> Implementations are {@code @ApplicationScoped} singletons. Every
 * method is safe for concurrent use.
 *
 * <p>Failures are reported as {@link TraceCryptoException}; messages carry no key material,
 * signature bytes or record content.
 *
 * @author Yuriy Movchan
 */
public interface TraceCryptoProvider {

	/**
	 * @return the type this provider implements
	 */
	TraceCryptoProviderType type();

	/**
	 * @throws TraceCryptoException with reason {@code crypto_unavailable} when the provider cannot
	 *         perform Ed25519 verification
	 */
	void requireAvailable();

	/**
	 * @return {@code true} when {@link #requireAvailable()} would not throw
	 */
	boolean isAvailable();

	/**
	 * @return one line for the startup log; never contains secrets
	 */
	String describe();

	/**
	 * @return the RFC 8785 canonical text of {@code node}
	 * @throws TraceCryptoException with reason {@code canonicalization_failed}
	 */
	String canonicalize(JsonNode node);

	/**
	 * Builds a verification key from a public Ed25519 JWK ({@code kty}, {@code crv}, {@code x}).
	 *
	 * @throws TraceCryptoException with a {@code jwk_*} reason, {@code invalid_key} or
	 *         {@code crypto_unavailable}
	 */
	TraceVerificationKey parseKey(Map<String, String> jwk);

	/**
	 * Verifies {@code signature} (Ed25519) over the JCS form of the record without its top-level
	 * {@code signature} member and computes the content digest.
	 *
	 * @return {@code content_digest}: {@code "sha256:" + hex(SHA-256(JCS(complete record)))}
	 * @throws TraceCryptoException with reason {@code signature_mismatch},
	 *         {@code canonicalization_failed} or {@code crypto_unavailable}
	 */
	String verifyAndDigest(TraceSignedRecord record, byte[] signature, TraceVerificationKey key);

	/**
	 * @return the receipt hash for one receipt-chain position (see the class description for the
	 *         member set)
	 * @throws TraceCryptoException with reason {@code invalid_receipt_input} when an input cannot
	 *         be a receipt position
	 */
	String receiptHash(String domainId, long receiptSequence, long receivedAtMs, String producerId,
			String recordId, String contentDigest, String prevReceiptHash);

}
