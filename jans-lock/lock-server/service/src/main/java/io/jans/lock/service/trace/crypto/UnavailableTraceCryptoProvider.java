/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.crypto;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceCryptoProviderType;
import io.jans.lock.trace.crypto.TraceSignedRecord;
import io.jans.lock.trace.crypto.TraceVerificationKey;

/**
 * Fail-closed {@link TraceCryptoProvider} used when no implementation of the selected type is on
 * the classpath. Every operation throws {@code crypto_unavailable}, so TRACE ingestion answers
 * {@code crypto_unavailable} instead of failing at startup.
 *
 * <p>The class is public, non-final and has a public no-argument constructor so that Weld can
 * proxy it as the {@code @ApplicationScoped} producer result.
 *
 * @author Yuriy Movchan
 */
public class UnavailableTraceCryptoProvider implements TraceCryptoProvider {

	private final TraceCryptoProviderType type;

	private final String reason;

	public UnavailableTraceCryptoProvider() {
		this(TraceCryptoProviderType.JAVA, "provider not initialized");
	}

	public UnavailableTraceCryptoProvider(TraceCryptoProviderType type, String reason) {
		this.type = type;
		this.reason = reason;
	}

	@Override
	public TraceCryptoProviderType type() {
		return type;
	}

	@Override
	public void requireAvailable() {
		throw new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
				"TRACE crypto provider " + type + " is unavailable: " + reason);
	}

	@Override
	public boolean isAvailable() {
		return false;
	}

	@Override
	public String describe() {
		return "unavailable/" + type + " (" + reason + ")";
	}

	@Override
	public String canonicalize(JsonNode node) {
		requireAvailable();
		return null;
	}

	@Override
	public TraceVerificationKey parseKey(Map<String, String> jwk) {
		requireAvailable();
		return null;
	}

	@Override
	public String verifyAndDigest(TraceSignedRecord record, byte[] signature, TraceVerificationKey key) {
		requireAvailable();
		return null;
	}

	@Override
	public String receiptHash(String domainId, long receiptSequence, long receivedAtMs, String producerId,
			String recordId, String contentDigest, String prevReceiptHash) {
		requireAvailable();
		return null;
	}

}
