/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.verify;

import java.util.Objects;

import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.VerificationResult;
import io.jans.lock.service.trace.parse.ParsedAssertion;
import io.jans.lock.service.trace.validate.CorrelationInputs;

/**
 * The outcome of {@link TraceVerificationService#verify}: an assertion that passed bounded
 * parsing, common and event-kind validation, the forwarding check and Ed25519 signature
 * verification (design §5 steps 3–6), together with the values the ingestion pipeline needs next.
 *
 * <p>Immutable. {@link #getParsed()} is the same {@link ParsedAssertion} instance the parser
 * produced, so {@link ParsedAssertion#getRawText()} is still the request body exactly as received
 * — that text, not a re-serialization, is what gets stored as the record's {@code assertion}.
 *
 * @author Yuriy Movchan
 */
public final class VerifiedAssertion {

	private final ParsedAssertion parsed;

	private final CorrelationInputs inputs;

	private final String contentDigest;

	private final VerificationResult verification;

	private final RecordIdentity identity;

	public VerifiedAssertion(ParsedAssertion parsed, CorrelationInputs inputs, String contentDigest,
			VerificationResult verification, RecordIdentity identity) {
		this.parsed = Objects.requireNonNull(parsed, "parsed");
		this.inputs = Objects.requireNonNull(inputs, "inputs");
		this.contentDigest = Objects.requireNonNull(contentDigest, "contentDigest");
		this.verification = Objects.requireNonNull(verification, "verification");
		this.identity = Objects.requireNonNull(identity, "identity");
	}

	/**
	 * @return the parsed assertion; its {@code rawText} is the evidence to store verbatim
	 */
	public ParsedAssertion getParsed() {
		return parsed;
	}

	/**
	 * @return the correlation inputs extracted by the event-kind validator (task 09)
	 */
	public CorrelationInputs getInputs() {
		return inputs;
	}

	/**
	 * @return {@code "sha256:" + hex(SHA-256(JCS(full assertion including signature)))} (design D-13)
	 */
	public String getContentDigest() {
		return contentDigest;
	}

	/**
	 * @return the verification outcome to store with the record: {@code signatureValid=true}, the
	 *         exact {@code kid} used, the verification instant and the algorithm label
	 */
	public VerificationResult getVerification() {
		return verification;
	}

	/**
	 * @return {@code (evidence_domain_id, producer, record_id)} — the domain comes from the request
	 *         context, never from the body
	 */
	public RecordIdentity getIdentity() {
		return identity;
	}

	/**
	 * @return the signed {@code producer} string; shorthand for {@code getParsed().getProducer()}
	 */
	public String getProducer() {
		return parsed.getProducer();
	}

	/**
	 * @return the signed {@code record_id}; shorthand for {@code getParsed().getRecordId()}
	 */
	public String getRecordId() {
		return parsed.getRecordId();
	}

	@Override
	public String toString() {
		return "VerifiedAssertion [identity=" + identity + ", eventKind=" + parsed.getEventKind() + ", kid="
				+ parsed.getKid() + ", contentDigest=" + contentDigest + "]";
	}

}
