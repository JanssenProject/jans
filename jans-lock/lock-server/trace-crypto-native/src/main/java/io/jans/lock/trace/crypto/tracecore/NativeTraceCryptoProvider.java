/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.trace.crypto.tracecore;

import java.nio.charset.CharacterCodingException;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.TraceConfiguration;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoImpl;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceCryptoProviderType;
import io.jans.lock.trace.crypto.TraceSignedRecord;
import io.jans.lock.trace.crypto.TraceVerificationKey;
import io.jans.trace.Digest;
import io.jans.trace.Json;
import io.jans.trace.JsonBody;
import io.jans.trace.Limits;
import io.jans.trace.Profiles;
import io.jans.trace.PublicKey;
import io.jans.trace.ReceiptCommitment;
import io.jans.trace.ReceiptPosition;
import io.jans.trace.ReceivedAt;
import io.jans.trace.SelfTest;
import io.jans.trace.SignedAssertion;
import io.jans.trace.StoredReceipt;
import io.jans.trace.TraceException;
import io.jans.trace.TraceException.CanonicalizeException;
import io.jans.trace.TraceException.JwkException;
import io.jans.trace.TraceException.ParseSignedAssertionException;
import io.jans.trace.TraceException.VerifyException;
import io.jans.trace.VerificationContext;
import io.jans.trace.VerifiedAssertion;
import io.jans.trace.uniffi.ErrorCode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * {@link TraceCryptoProviderType#NATIVE} implementation of {@link TraceCryptoProvider}: JCS,
 * Ed25519, content digest and receipt hash all come from {@code jans-trace-core} through the
 * {@code jans-trace-java} binding.
 *
 * <p>Lock's Jackson parser is the only enforcer of the request limits. The limits handed to the
 * native library are at least twice the Lock values, so a body Lock accepted is never rejected
 * here.
 *
 * <p>A library that does not load surfaces as an {@link Error}; the probe records it and every
 * operation then fails closed with {@code crypto_unavailable}.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
@TraceCryptoImpl(TraceCryptoProviderType.NATIVE)
public class NativeTraceCryptoProvider implements TraceCryptoProvider {

	private static final Logger LOG = LoggerFactory.getLogger(NativeTraceCryptoProvider.class);

	@Inject
	private AppConfiguration appConfiguration;

	private TraceConfiguration traceConfiguration;

	private volatile Limits limits;

	private volatile boolean available;

	private volatile String failure = "not probed";

	private volatile String profile;

	public NativeTraceCryptoProvider() {
	}

	public NativeTraceCryptoProvider(TraceConfiguration traceConfiguration) {
		this.traceConfiguration = traceConfiguration;
	}

	/**
	 * Builds the native limits and runs the library self-test. Never throws: any failure,
	 * including a library load {@link Error}, leaves the provider unavailable.
	 */
	@PostConstruct
	public void probe() {
		try {
			TraceConfiguration config = traceConfiguration != null ? traceConfiguration
					: appConfiguration.getTraceConfiguration();
			Limits built = buildLimits(config);
			SelfTest.run();
			String assertionProfile = Profiles.assertionProfile();
			this.limits = built;
			this.profile = assertionProfile;
			this.failure = null;
			this.available = true;
			LOG.info("Native TRACE crypto probe: jans-trace-core profile={} available=true", assertionProfile);
		} catch (Throwable t) {
			this.available = false;
			this.failure = t.getClass().getSimpleName() + ": " + t.getMessage();
			LOG.error("Native TRACE crypto probe: available=false ({}); TRACE ingestion will fail closed", failure);
			LOG.debug("Native TRACE crypto probe failure detail", t);
		}
	}

	static Limits buildLimits(TraceConfiguration config) {
		return new Limits()
				.withMaxInputBytes(Math.max(262_144L, 2L * config.getMaxRequestBytes()))
				.withMaxDepth(Math.max(32L, 2L * config.getMaxJsonDepth()))
				.withMaxObjectMembers(Math.max(1024L, 2L * config.getMaxObjectMembers()))
				.withMaxArrayElements(Math.max(1024L, 2L * config.getMaxArrayLength()))
				.withMaxStringBytes(Math.max(65_536L, 4L * config.getMaxStringLength()))
				.withMaxCanonicalBytes(Math.max(524_288L, 2L * config.getMaxRequestBytes()));
	}

	@Override
	public TraceCryptoProviderType type() {
		return TraceCryptoProviderType.NATIVE;
	}

	@Override
	public void requireAvailable() {
		if (!available) {
			throw new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
					"Native TRACE crypto is unavailable: " + failure);
		}
	}

	@Override
	public boolean isAvailable() {
		return available;
	}

	@Override
	public String describe() {
		return available ? "native/jans-trace-core " + profile : failure;
	}

	@Override
	public String canonicalize(JsonNode node) {
		requireAvailable();
		try {
			return Json.canonicalize(new JsonBody(node.toString()), limits);
		} catch (CanonicalizeException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_CANONICALIZATION_FAILED,
					codeName(ex), ex);
		} catch (Error err) {
			throw unavailable(err);
		} catch (Exception ex) {
			throw unencodable(ex, TraceCryptoException.REASON_CANONICALIZATION_FAILED);
		}
	}

	@Override
	public TraceVerificationKey parseKey(Map<String, String> jwk) {
		if (jwk == null) {
			throw new TraceCryptoException(TraceCryptoException.REASON_JWK_FORMAT, "JWK is missing");
		}
		requireAvailable();

		ObjectNode json = JsonNodeFactory.instance.objectNode();
		for (String member : new String[] { "kty", "crv", "x", "d" }) {
			String value = jwk.get(member);
			if (value != null) {
				json.put(member, value);
			}
		}

		try {
			PublicKey key = PublicKey.fromJwk(new JsonBody(json.toString()));
			return new NativeVerificationKey(key, key.thumbprint().toString());
		} catch (JwkException ex) {
			String reason = ex.code() == ErrorCode.INVALID_PUBLIC_KEY ? TraceCryptoException.REASON_INVALID_KEY
					: TraceCryptoException.REASON_JWK_FORMAT;
			throw new TraceCryptoException(reason, codeName(ex), ex);
		} catch (Error err) {
			throw unavailable(err);
		} catch (Exception ex) {
			throw unencodable(ex, TraceCryptoException.REASON_JWK_FORMAT);
		}
	}

	@Override
	public String verifyAndDigest(TraceSignedRecord record, byte[] signature, TraceVerificationKey key) {
		if (!(key instanceof NativeVerificationKey)) {
			throw new IllegalArgumentException("key was not created by " + NativeTraceCryptoProvider.class.getSimpleName());
		}
		if (signature == null || signature.length != 64) {
			throw new IllegalArgumentException("Ed25519 signature must be 64 bytes");
		}
		NativeVerificationKey nativeKey = (NativeVerificationKey) key;

		requireAvailable();
		try {
			SignedAssertion signed = SignedAssertion.parse(new JsonBody(record.getRawText()), limits);
			VerifiedAssertion verified = signed
					.verify(new VerificationContext(record.getProducer(), record.getKid(), nativeKey.key));
			return verified.contentDigest().toString();
		} catch (ParseSignedAssertionException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_CANONICALIZATION_FAILED, codeName(ex), ex);
		} catch (VerifyException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_SIGNATURE_MISMATCH, codeName(ex), ex);
		} catch (Error err) {
			throw unavailable(err);
		} catch (Exception ex) {
			throw unencodable(ex, TraceCryptoException.REASON_CANONICALIZATION_FAILED);
		}
	}

	@Override
	public String receiptHash(String domainId, long receiptSequence, long receivedAtMs, String producerId,
			String recordId, String contentDigest, String prevReceiptHash) {
		requireAvailable();
		try {
			StoredReceipt stored = StoredReceipt.builder().evidenceDomainId(domainId)
					.position(ReceiptPosition.of(receiptSequence, Digest.parse(prevReceiptHash)))
					.receivedAt(ReceivedAt.fromUnixMillis(receivedAtMs)).producerId(producerId).recordId(recordId)
					.contentDigest(Digest.parse(contentDigest)).build();
			return ReceiptCommitment.fromStored(stored).receiptHash().toString();
		} catch (TraceException ex) {
			throw new TraceCryptoException(TraceCryptoException.REASON_INVALID_RECEIPT_INPUT, codeName(ex), ex);
		} catch (Error err) {
			throw unavailable(err);
		} catch (Exception ex) {
			throw unencodable(ex, TraceCryptoException.REASON_INVALID_RECEIPT_INPUT);
		}
	}

	private static String codeName(TraceException ex) {
		return String.valueOf(ex.code());
	}

	/**
	 * The binding encodes strings to UTF-8 strictly and rethrows the checked
	 * {@link CharacterCodingException} unwrapped when a string holds an unpaired surrogate, a
	 * failure the Java signatures do not declare. It is bad input, not a library fault; any other
	 * unexpected exception is rethrown as is.
	 */
	private static TraceCryptoException unencodable(Exception ex, String reason) {
		if (ex instanceof CharacterCodingException) {
			return new TraceCryptoException(reason, "input is not encodable as UTF-8", ex);
		}
		if (ex instanceof RuntimeException) {
			throw (RuntimeException) ex;
		}
		throw new IllegalStateException(ex);
	}

	private static TraceCryptoException unavailable(Error err) {
		return new TraceCryptoException(TraceCryptoException.REASON_CRYPTO_UNAVAILABLE,
				"Native TRACE crypto failed: " + err.getClass().getSimpleName(), err);
	}

}
