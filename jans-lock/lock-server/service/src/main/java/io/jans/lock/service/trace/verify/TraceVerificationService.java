/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.verify;

import java.io.InputStream;

import org.slf4j.Logger;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.canon.JcsException;
import io.jans.lock.service.trace.crypto.Ed25519Capability;
import io.jans.lock.service.trace.crypto.Ed25519PublicKeys;
import io.jans.lock.service.trace.crypto.Ed25519Verifier;
import io.jans.lock.service.trace.crypto.StrictBase64Url;
import io.jans.lock.service.trace.error.TraceCryptoException;
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
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Design §5 processing steps 3–6 as one stateless service: turns a request body into a
 * {@link VerifiedAssertion} or throws a structured error, without touching the record store. The
 * only lookup performed is the producer-key resolution through {@link ProducerKeyRegistry}.
 *
 * <p>The order is normative (cheap checks before crypto, design §5):
 * <ol>
 * <li>{@link Ed25519Capability#requireAvailable()} — fail closed before reading the body;</li>
 * <li>bounded parse ({@link TraceAssertionParser});</li>
 * <li>§7.1 common validation ({@link CommonAssertionValidator});</li>
 * <li>forwarding check ({@link ForwardingPolicy}, decision D-3) — the signed {@code producer} is
 * known only now;</li>
 * <li>§7.2/§7.3 event-kind validation ({@link EventKindValidatorRegistry});</li>
 * <li>key resolution with validity evaluation at {@code ctx.receivedAtMs} (decision D-4);</li>
 * <li>Ed25519 verification over {@code JCS(assertion without "signature")} (decision D-13);</li>
 * <li>content digest over {@code JCS(full assertion)}.</li>
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
	private Ed25519Capability capability;

	@Inject
	private TraceAssertionParser parser;

	@Inject
	private CommonAssertionValidator commonValidator;

	@Inject
	private EventKindValidatorRegistry eventKindRegistry;

	@Inject
	private ProducerKeyRegistry keyRegistry;

	@Inject
	private Ed25519Verifier verifier;

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
		capability.requireAvailable();

		// 2. Bounded parse under the request limits (D-12).
		ParsedAssertion parsed = parser.parse(body);

		// 3. §7.1 common fields.
		commonValidator.validate(parsed);

		// 4. Forwarding (D-3): the signed producer is known now.
		ForwardingPolicy.check(ctx, parsed.getProducer());

		// 5. §7.2/§7.3 per event kind.
		CorrelationInputs inputs = eventKindRegistry.validate(parsed);

		// 6. Key resolution and validity at the receipt instant (D-4).
		ProducerKeyRegistry.ResolvedKey key = keyRegistry.resolveForVerification(ctx.getEvidenceDomainId(),
				parsed.getProducer(), parsed.getKid(), ctx.getReceivedAtMs());

		// 7. Signature over JCS(assertion minus "signature") (D-13).
		byte[] signature;
		try {
			signature = StrictBase64Url.decode(parsed.getSignature());
		} catch (IllegalArgumentException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_SIGNATURE, REASON_SIGNATURE_FORMAT, ex);
		}

		byte[] signatureInput;
		try {
			signatureInput = parsed.signatureInput();
		} catch (JcsException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_ASSERTION, REASON_CANONICALIZATION_FAILED,
					ex);
		}

		if (!verifier.verify(key.getPublicKey(), signatureInput, signature)) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_SIGNATURE, REASON_SIGNATURE_MISMATCH);
		}

		// 8. Digest over JCS(full assertion) (D-13).
		String contentDigest;
		try {
			contentDigest = parsed.contentDigest();
		} catch (JcsException ex) {
			throw new TraceValidationException(TraceErrorResponseType.INVALID_ASSERTION, REASON_CANONICALIZATION_FAILED,
					ex);
		}

		// 9. Assemble. verified_at is the same instant the key validity was evaluated at (D-4).
		VerificationResult verification = new VerificationResult(true, key.getKey().getKid(), ctx.getReceivedAtMs(),
				Ed25519PublicKeys.ALGORITHM);
		RecordIdentity identity = new RecordIdentity(ctx.getEvidenceDomainId(), parsed.getProducer(),
				parsed.getRecordId());

		if (log.isDebugEnabled()) {
			log.debug("TRACE assertion verified: producer={}, kid={}, record_id={}, event_kind={}",
					parsed.getProducer(), parsed.getKid(), parsed.getRecordId(), parsed.getEventKind());
		}

		return new VerifiedAssertion(parsed, inputs, contentDigest, verification, identity);
	}

}
