/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;

import org.slf4j.Logger;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.service.trace.correlation.CorrelationPlan;
import io.jans.lock.service.trace.correlation.TraceCorrelationService;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ReceiptClaim;
import io.jans.lock.service.trace.receipt.TraceReceiptChain;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.trace.verify.TraceVerificationService;
import io.jans.lock.service.trace.verify.VerifiedAssertion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Orchestrates design §5's full processing order (steps 1-9) for {@code POST /audit/trace}:
 * verification (task 16), the idempotency/conflict pre-check, correlation (task 17), receipt-chain
 * allocation (task 18) and the atomic insert that makes a record visible (design decision D-8, D-9,
 * §12 behavior table).
 *
 * <p>Verification, the pre-check and correlation planning never write anything, so any exception
 * from those steps (crypto unavailable, validation failure, forwarding, chain-registration,
 * genesis) leaves no row behind by construction. Once a receipt is claimed, every exit path either
 * commits it (insert succeeded), voids it (the insert lost a race or failed), or leaves it for
 * {@code TraceReceiptRepairTimer} to resolve (a crash between claim and void/commit).
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceIngestionService {

	static final String REASON_DIGEST_MISMATCH = "digest_mismatch";

	static final String REASON_RACE_RECORD_MISSING = "race_record_missing";

	@Inject
	private Logger log;

	@Inject
	private TraceVerificationService verification;

	@Inject
	private TraceCorrelationService correlation;

	@Inject
	private TraceReceiptChain receiptChain;

	@Inject
	private TraceStore store;

	/**
	 * @throws io.jans.lock.service.trace.error.TraceCryptoException     {@code crypto_unavailable}
	 * @throws io.jans.lock.service.trace.error.TraceValidationException every verification,
	 *                                                                   forwarding, chain or genesis
	 *                                                                   failure
	 * @throws TraceConflictException                                   {@code record_conflict} —
	 *                                                                   same identity, different
	 *                                                                   {@code content_digest}
	 * @throws TraceStorageException                                    {@code storage_failure}
	 */
	public AcceptanceResult ingest(InputStream body, TraceRequestContext ctx) {
		VerifiedAssertion verified = verification.verify(body, ctx);
		RecordIdentity identity = verified.getIdentity();

		Optional<StoredTraceRecord> precheck = store.findRecord(identity);
		if (precheck.isPresent()) {
			return classifyAgainstExisting(precheck.get(), verified.getContentDigest());
		}

		CorrelationPlan plan = correlation.plan(verified, ctx);

		String domainId = ctx.getEvidenceDomainId();
		String recordKey = TraceKeys.recordKey(identity);
		ReceiptClaim claim = receiptChain.claim(domainId, verified.getProducer(), verified.getRecordId(), recordKey,
				verified.getContentDigest(), ctx.getReceivedAtMs(), ctx.getNodeId());

		StoredTraceRecord record = buildRecord(verified, ctx, plan, claim);

		try {
			store.insertRecord(record);
		} catch (DuplicateEntryException ex) {
			receiptChain.voidClaim(domainId, claim.getSeq());
			Optional<StoredTraceRecord> raced = store.findRecord(identity);
			if (!raced.isPresent()) {
				throw new TraceStorageException(REASON_RACE_RECORD_MISSING,
						"Record missing after duplicate-entry race: " + identity);
			}
			return classifyAgainstExisting(raced.get(), verified.getContentDigest());
		} catch (TraceStorageException ex) {
			receiptChain.voidClaim(domainId, claim.getSeq());
			throw ex;
		}

		receiptChain.commit(domainId, claim.getSeq());
		correlation.afterInsert(record, plan);

		logAcceptance(record, false);
		return AcceptanceResult.fromStored(record, false);
	}

	private AcceptanceResult classifyAgainstExisting(StoredTraceRecord existing, String contentDigest) {
		if (digestsEqual(existing.getContentDigest(), contentDigest)) {
			logAcceptance(existing, true);
			return AcceptanceResult.fromStored(existing, true);
		}
		throw new TraceConflictException(TraceErrorResponseType.RECORD_CONFLICT, REASON_DIGEST_MISMATCH);
	}

	private StoredTraceRecord buildRecord(VerifiedAssertion verified, TraceRequestContext ctx, CorrelationPlan plan,
			ReceiptClaim claim) {
		ReceiptEntry receipt = new ReceiptEntry(claim.getSeq(), claim.getReceivedAtMs(), claim.getPrevReceiptHash(),
				claim.getReceiptHash());
		return new StoredTraceRecord(verified.getIdentity(), verified.getParsed().getRawText(),
				verified.getContentDigest(), verified.getVerification(), receipt, plan.getFlags(),
				plan.getExecution(), plan.getPosition(), verified.getInputs().getChainPosition().getPrevRecordHash(),
				verified.getInputs().getCapabilityIds(), verified.getInputs().getTokenRefs(),
				verified.getInputs().getEventKind(), verified.getInputs().getSignedAt(), ctx.getNodeId(),
				ctx.getReceivedAtMs());
	}

	private void logAcceptance(StoredTraceRecord record, boolean replay) {
		log.info(
				"TRACE record accepted: producer={}, record_id={}, receipt_sequence={}, coverage_gap={}, "
						+ "chain_link_failure={}, equivocation={}, late={}, replay={}",
				record.getIdentity().getProducerId(), record.getIdentity().getRecordId(),
				record.getReceipt().getReceiptSequence(), record.getFlags().isCoverageGap(),
				record.getFlags().isChainLinkFailure(), record.getFlags().isEquivocation(),
				record.getFlags().isLate(), replay);
	}

	private static boolean digestsEqual(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}

}
