/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.verify;

import java.io.InputStream;

import org.slf4j.Logger;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.error.TraceValidationException;
import io.jans.lock.service.trace.identity.ForwardingPolicy;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.parse.CommonAssertionValidator;
import io.jans.lock.service.trace.parse.ParsedAssertion;
import io.jans.lock.service.trace.parse.TraceAssertionParser;
import io.jans.lock.service.trace.registry.ProducerKeyRegistry;
import io.jans.lock.service.trace.validate.CorrelationInputs;
import io.jans.lock.service.trace.validate.EventKindValidatorRegistry;
import io.jans.lock.trace.crypto.StrictBase64Url;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
import io.jans.lock.trace.crypto.TraceSignedRecord;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Design §5 processing steps 3–6 as one stateless service: turns a request body into a
 * {@link VerifiedAssertion} or throws a structured error, without touching the record store. The
 * only lookup performed is the producer-key resolution through {@link ProducerKeyRegistry}.
 *
 * <p>The order is normative (cheap checks before crypto, design §5):
 * <ol>
 * <li>{@link TraceCryptoProvider#requireAvailable()} — fail closed before reading the body;</li>
 * <li>bounded parse ({@link TraceAssertionParser});</li>
 * <li>§7.1 common validation ({@link CommonAssertionValidator});</li>
 * <li>forwarding check ({@link ForwardingPolicy}) — the signed {@code producer} is
 * known only now;</li>
 * <li>§7.2/§7.3 event-kind validation ({@link EventKindValidatorRegistry});</li>
 * <li>key resolution with validity evaluation at {@code ctx.receivedAtMs};</li>
 * <li>event-kind scope of the key, before any signature work;</li>
 * <li>Ed25519 verification over {@code JCS(assertion without "signature")} and content digest
 * over {@code JCS(full assertion)}, both through {@link TraceCryptoProvider#verifyAndDigest}.</li>
 * </ol>
 *
 * <p>Logging: one DEBUG line per verified record (producer, kid, record_id, event_kind); every
 * rejection at INFO with the error id and reason only. Bodies, signatures and keys are never
 * logged.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceVerificationService {

	static final String REASON_SIGNATURE_MISMATCH = "signature_mismatch";

	static final String REASON_SIGNATURE_FORMAT = "signature_format";

	static final String REASON_CANONICALIZATION_FAILED = "canonicalization_failed";

	@Inject
	private Logger log;

	@Inject
	private TraceCryptoProvider cryptoProvider;

	@Inject
	private TraceAssertionParser parser;

	@Inject
	private CommonAssertionValidator commonValidator;

	@Inject
	private EventKindValidatorRegistry eventKindRegistry;

	@Inject
	private ProducerKeyRegistry keyRegistry;

	/**
	 * @param body the raw request body; read at most once, bounded by {@code maxRequestBytes}
	 * @param ctx  the resolved request context (domain, allowed producers, receipt instant)
	 * @return the verified assertion, ready for correlation and storage
	 * @throws TraceCryptoException     {@code crypto_unavailable} when the provider lacks Ed25519
	 *                                  (thrown before the body is read)
	 * @throws TraceValidationException every parsing, validation, forwarding, key and signature
	 *                                  failure, carrying the {@link TraceErrorResponseType} and a
	 *                                  field-name-only reason
	 */
	public VerifiedAssertion verify(InputStream body, TraceRequestContext ctx) {
		try {
			return doVerify(body, ctx);
		} catch (TraceValidationException ex) {
			log.info("TRACE assertion rejected: error={}, reason={}", ex.getErrorId().getParameter(), ex.getReason());
			throw ex;
		} catch (TraceCryptoException ex) {
			// Same error-id mapping TraceErrors applies when building the HTTP response.
			TraceErrorResponseType errorId = TraceCryptoException.REASON_CRYPTO_UNAVAILABLE.equals(ex.getReason())
					? TraceErrorResponseType.CRYPTO_UNAVAILABLE
					: TraceErrorResponseType.INVALID_KEY;
			log.info("TRACE assertion rejected: error={}, reason={}", errorId.getParameter(), ex.getReason());
			throw ex;
		}
	}

	private VerifiedAssertion doVerify(InputStream body, TraceRequestContext ctx) {
		// 1. Fail closed before touching the body.
		cryptoProvider.requireAvailable();

		// 2. Bounded parse under the request limits.
		ParsedAssertion parsed = parser.parse(body);

		// 3. §7.1 common fields.
		commonValidator.validate(parsed);

		// 4. Forwarding: the signed producer is known now.
		ForwardingPolicy.check(ctx, parsed.getProducer());

		// 5. §7.2/§7.3 per event kind.
		CorrelationInputs inputs = eventKindRegistry.validate(parsed);

		// 6. Key resolution and validity at the receipt instant.
		ProducerKeyRegistry.ResolvedKey key = keyRegistry.resolveForVerification(ctx.getEvidenceDomainId(),
				parsed.getProducer(), parsed.getKid(), ctx.getReceivedAtMs());

		// 6b. The key must be authorized for this event kind; no crypto has run yet.
		keyRegistry.requireEventKindAuthorized(key, parsed.getEventKind());

		// 7. Signature over JCS(assertion minus "signature").
		byte[] signature;
		try {
			signature = StrictBase64Url.decode(parsed.getSignature());
		} catch (IllegalArgumentException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_SIGNATURE, REASON_SIGNATURE_FORMAT, ex);
		}

		// 8. Digest over JCS(full assertion), computed by the same call.
		String contentDigest;
		try {
			contentDigest = cryptoProvider.verifyAndDigest(new TraceSignedRecord(parsed.getRawText(),
					parsed.getRoot(), parsed.getProducer(), parsed.getKid()), signature, key.getVerificationKey());
		} catch (TraceCryptoException ex) {
			if (TraceCryptoException.REASON_SIGNATURE_MISMATCH.equals(ex.getReason())) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_SIGNATURE,
						REASON_SIGNATURE_MISMATCH);
			}
			if (TraceCryptoException.REASON_CANONICALIZATION_FAILED.equals(ex.getReason())) {
				throw new TraceValidationException(TraceErrorResponseType.INVALID_ASSERTION,
						REASON_CANONICALIZATION_FAILED, ex);
			}
			throw ex;
		}

		// 9. Assemble. verified_at is the same instant the key validity was evaluated at.
		// Every admission check passed above (a failing record is rejected, never stored), so
		// key_authorized is true; both new members are computed here once and never again on read.
		String keyThumbprint = key.getVerificationKey().thumbprint();
		VerificationResult verification = new VerificationResult(true, key.getKey().getKid(), ctx.getReceivedAtMs(),
				TraceConstants.SIGNATURE_ALGORITHM, keyThumbprint, Boolean.TRUE);
		RecordIdentity identity = new RecordIdentity(ctx.getEvidenceDomainId(), parsed.getProducer(),
				parsed.getRecordId());

		if (log.isDebugEnabled()) {
			log.debug("TRACE assertion verified: producer={}, kid={}, record_id={}, event_kind={}",
					parsed.getProducer(), parsed.getKid(), parsed.getRecordId(), parsed.getEventKind());
		}

		return new VerifiedAssertion(parsed, inputs, contentDigest, verification, identity);
	}

}
