/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.registry;

import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.crypto.Ed25519PublicKeys;
import io.jans.lock.service.trace.crypto.TraceCryptoException;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.model.ProducerKey;
import io.jans.lock.service.trace.parse.TraceValidationException;
import io.jans.lock.service.trace.store.DuplicateEntryException;
import io.jans.lock.service.trace.store.TraceStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Business-level service over {@link TraceStore} for producer-key lookup with validity evaluation
 * (design decision D-4) and key registration/revocation (design §6, T-4).
 *
 * <p>The MVP does no caching: every {@link #resolveForVerification} call hits {@link TraceStore}
 * directly, so a revocation is visible on the very next request. The safe upgrade path, if the
 * per-request lookup cost ever matters, is a short-TTL cache keyed by
 * {@code (domainId, producerId, kid)} that is explicitly invalidated by {@link #revoke} — never a
 * cache that only expires on its own, since that would let a revoked key keep verifying for up to
 * the TTL.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class ProducerKeyRegistry {

	static final String REASON_KEY_NOT_REGISTERED = "key_not_registered";

	static final String REASON_STORED_KEY_UNUSABLE = "stored_key_unusable";

	static final String REASON_NOT_YET_VALID = "not_yet_valid";

	static final String REASON_EXPIRED = "expired";

	static final String REASON_REVOKED = "revoked";

	static final String REASON_VALIDITY_WINDOW = "validity_window";

	static final String REASON_PRODUCER_ID_FORMAT = "producer_id_format";

	static final String REASON_KID_EMPTY = "kid_empty";

	static final String REASON_KID_LENGTH = "kid_length";

	static final String REASON_DUPLICATE_KEY = "duplicate_key";

	private static final int MAX_KID_LENGTH = 255;

	/**
	 * {@code name/semver}, identical to task 08's {@code CommonAssertionValidator.PRODUCER_PATTERN}:
	 * producer id up to 128 chars, then a semver.
	 */
	private static final Pattern PRODUCER_PATTERN = Pattern
			.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}/[0-9]+\\.[0-9]+\\.[0-9]+([-+][0-9A-Za-z.-]+)?$");

	@Inject
	private Logger log;

	@Inject
	private TraceStore traceStore;

	/**
	 * Resolves the key that must verify a record's signature and evaluates its validity window at
	 * {@code nowMs} (design decision D-4).
	 *
	 * @throws TraceValidationException {@code unknown_producer_key} when no key is registered for
	 *                                  this {@code (domainId, producerId, kid)}, or when the stored
	 *                                  JWK cannot be parsed into a usable key (a data error, not a
	 *                                  crypto-provider failure); {@code key_not_valid} with reason
	 *                                  {@code not_yet_valid}/{@code expired}/{@code revoked} when the
	 *                                  key is outside its validity window
	 */
	public ResolvedKey resolveForVerification(String domainId, String producerId, String kid, long nowMs) {
		ProducerKey key = traceStore.findProducerKey(domainId, producerId, kid)
				.orElseThrow(() -> new TraceValidationException(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY,
						REASON_KEY_NOT_REGISTERED));

		if (nowMs < key.getValidFromMs()) {
			throw new TraceValidationException(TraceErrorResponseType.KEY_NOT_VALID, REASON_NOT_YET_VALID);
		}
		if (key.getValidUntilMs() != null && nowMs >= key.getValidUntilMs()) {
			throw new TraceValidationException(TraceErrorResponseType.KEY_NOT_VALID, REASON_EXPIRED);
		}
		if (key.getRevokedAtMs() != null && nowMs >= key.getRevokedAtMs()) {
			throw new TraceValidationException(TraceErrorResponseType.KEY_NOT_VALID, REASON_REVOKED);
		}

		PublicKey publicKey;
		try {
			publicKey = Ed25519PublicKeys.fromJwk(key.getPublicKeyJwk());
		} catch (TraceCryptoException ex) {
			log.error("Stored producer key is unusable: domainId={}, producerId={}, kid={}, reason={}", domainId,
					producerId, kid, ex.getReason());
			throw new TraceValidationException(TraceErrorResponseType.UNKNOWN_PRODUCER_KEY, REASON_STORED_KEY_UNUSABLE,
					ex);
		}

		return new ResolvedKey(key, publicKey);
	}

	/**
	 * Registers a new producer key. Keys are create-only (design decision D-4): rotation is
	 * registering a new {@code kid}, retirement is {@link #revoke}; there is no update.
	 *
	 * @throws TraceCryptoException     if {@code jwk} is not a well-formed RFC 8037 Ed25519 OKP JWK
	 *                                  (mapped by {@code TraceErrors} to {@code invalid_key} or
	 *                                  {@code crypto_unavailable})
	 * @throws TraceValidationException {@code invalid_key} when {@code validUntilMs} is not strictly
	 *                                  after {@code validFromMs}, {@code producerId} does not match
	 *                                  the {@code name/semver} format, or {@code kid} is empty or
	 *                                  longer than 255 characters
	 * @throws TraceConflictException   {@code key_already_exists} when this
	 *                                  {@code (domainId, producerId, kid)} is already registered
	 */
	public ProducerKey register(String domainId, String producerId, String kid, Map<String, String> jwk,
			long validFromMs, Long validUntilMs, String registeredBy) {
		Ed25519PublicKeys.fromJwk(jwk);

		if (validUntilMs != null && validUntilMs <= validFromMs) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_KEY, REASON_VALIDITY_WINDOW);
		}
		if (!PRODUCER_PATTERN.matcher(Objects.toString(producerId, "")).matches()) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_KEY, REASON_PRODUCER_ID_FORMAT);
		}
		if (StringUtils.isEmpty(kid)) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_KEY, REASON_KID_EMPTY);
		}
		if (kid.length() > MAX_KID_LENGTH) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_KEY, REASON_KID_LENGTH);
		}

		ProducerKey key = new ProducerKey(domainId, producerId, kid, jwk, validFromMs, validUntilMs, null,
				registeredBy);
		try {
			traceStore.insertProducerKey(key);
		} catch (DuplicateEntryException ex) {
			throw new TraceConflictException(TraceErrorResponseType.KEY_ALREADY_EXISTS, REASON_DUPLICATE_KEY);
		}
		return key;
	}

	/**
	 * Revokes a producer key. Idempotent: revoking an already-revoked key succeeds and returns it
	 * unchanged.
	 *
	 * @return the key after revocation, or {@link Optional#empty()} if no such key is registered
	 */
	public Optional<ProducerKey> revoke(String domainId, String producerId, String kid, long nowMs) {
		boolean found = traceStore.revokeProducerKey(domainId, producerId, kid, nowMs);
		if (!found) {
			return Optional.empty();
		}
		return traceStore.findProducerKey(domainId, producerId, kid);
	}

	/**
	 * @param producerIdOrNull restricts the result to one producer, or {@code null} for all
	 *                          producers in the domain
	 */
	public List<ProducerKey> list(String domainId, String producerIdOrNull) {
		return traceStore.findProducerKeys(domainId, producerIdOrNull);
	}

	/**
	 * Test seam: bypasses CDI injection of {@link TraceStore}.
	 */
	void setTraceStore(TraceStore traceStore) {
		this.traceStore = traceStore;
	}

	/**
	 * Test seam: bypasses CDI injection of {@link Logger}.
	 */
	void setLog(Logger log) {
		this.log = log;
	}

	/**
	 * A producer key together with the {@link PublicKey} built from its stored JWK, returned by
	 * {@link #resolveForVerification} so callers never re-parse the JWK.
	 */
	public static final class ResolvedKey {

		private final ProducerKey key;

		private final PublicKey publicKey;

		ResolvedKey(ProducerKey key, PublicKey publicKey) {
			this.key = Objects.requireNonNull(key, "key");
			this.publicKey = Objects.requireNonNull(publicKey, "publicKey");
		}

		public ProducerKey getKey() {
			return key;
		}

		public PublicKey getPublicKey() {
			return publicKey;
		}

	}

}
