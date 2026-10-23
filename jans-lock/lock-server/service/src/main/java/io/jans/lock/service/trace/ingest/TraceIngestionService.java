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
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.correlation.CorrelationPlan;
import io.jans.lock.service.trace.correlation.TraceCorrelationService;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ReceiptClaim;
import io.jans.lock.service.trace.receipt.TraceReceiptChain;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.trace.verify.TraceVerificationService;
import io.jans.lock.service.trace.verify.VerifiedAssertion;
import io.jans.lock.trace.crypto.TraceCryptoException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Orchestrates design §5's full processing order (steps 1-9) for {@code POST /audit/trace}:
 * verification, the idempotency/conflict pre-check, correlation, receipt-chain
 * allocation and the atomic insert that makes a record visible (design §12 behavior table).
 *
 * <p>Verification, the pre-check and correlation planning never write anything, so any exception
 * from those steps (crypto unavailable, validation failure, forwarding, chain-registration,
 * genesis) leaves no row behind by construction. Once a receipt is claimed, every exit path either
 * commits it (insert succeeded), voids it (the insert lost a race or failed), or leaves it for
 * {@code TraceReceiptRepairTimer} to resolve (a crash between claim and void/commit, or a failed
 * insert whose outcome could not be read back). A receipt leaves {@code PENDING} by
 * compare-and-swap only; see {@link TraceReceiptChain}.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceIngestionService {

	static final String REASON_DIGEST_MISMATCH = "digest_mismatch";

	static final String REASON_RACE_RECORD_MISSING = "race_record_missing";

	/** The worker passed its ingest fence before it inserted the record; the claim was voided. */
	static final String REASON_CLAIM_EXPIRED = "receipt_claim_expired";

	/** The record is stored but its receipt is {@code VOID}: an orphan that blocks its identity. */
	static final String REASON_VOID_AFTER_INSERT = "receipt_void_after_insert";

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

	@Inject
	private TraceKeys traceKeys;

	/**
	 * @throws io.jans.lock.service.trace.error.TraceCryptoException     {@code crypto_unavailable}
	 * @throws io.jans.lock.service.trace.error.TraceValidationException every verification,
	 *                                                                   forwarding, chain or genesis
	 *                                                                   failure
	 * @throws TraceConflictException                                   {@code record_conflict} —
	 *                                                                   same identity, different
	 *                                                                   {@code content_digest}
	 * @throws TraceStorageException                                    {@code storage_failure},
	 *                                                                   including reasons
	 *                                                                   {@code receipt_claim_expired}
	 *                                                                   and
	 *                                                                   {@code receipt_void_after_insert}
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
		String recordKey = traceKeys.recordKey(identity);
		ReceiptClaim claim = receiptChain.claim(domainId, verified.getProducer(), verified.getRecordId(), recordKey,
				verified.getContentDigest(), ctx.getReceivedAtMs(), ctx.getNodeId());

		StoredTraceRecord stored = buildRecord(verified, ctx, plan, claim);

		if (receiptChain.isFenceExpired(claim)) {
			receiptChain.voidClaim(domainId, claim.getSeq());
			throw new TraceStorageException(REASON_CLAIM_EXPIRED,
					"Receipt claim expired before the record was inserted: domain=" + domainId + ", seq="
							+ claim.getSeq());
		}

		try {
			store.insertRecord(stored);
		} catch (DuplicateEntryException ex) {
			ReceiptSettlement voided = receiptChain.voidClaim(domainId, claim.getSeq());
			if (voided == ReceiptSettlement.CONFLICT) {
				log.warn("Duplicate-identity race loser claim is already COMMITTED: domain={}, seq={}", domainId,
						claim.getSeq());
			}
			Optional<StoredTraceRecord> raced = store.findRecord(identity);
			if (!raced.isPresent()) {
				throw new TraceStorageException(REASON_RACE_RECORD_MISSING,
						"Record missing after duplicate-entry race: " + identity);
			}
			return classifyAgainstExisting(raced.get(), verified.getContentDigest());
		} catch (TraceStorageException ex) {
			if (!insertLanded(identity, claim, verified.getContentDigest(), ex)) {
				throw ex;
			}
			log.warn("Record insert reported a failure but the record is stored: domain={}, seq={}, reason={}",
					domainId, claim.getSeq(), ex.getReason());
		}

		return settleAfterInsert(stored, plan, domainId, claim.getSeq());
	}

	/**
	 * Decides whether an {@code insertRecord} that threw really failed. Re-reads the record: if it
	 * exists and carries this claim, the write succeeded (a timeout after commit) and the caller
	 * continues the success path; if it does not, the claim is voided. If the re-read itself fails
	 * the claim stays {@code PENDING} for the repair timer.
	 *
	 * @return {@code true} if the record is stored under this claim, {@code false} if the caller
	 *         must rethrow {@code failure}
	 */
	private boolean insertLanded(RecordIdentity identity, ReceiptClaim claim, String contentDigest,
			TraceStorageException failure) {
		Optional<StoredTraceRecord> reread;
		try {
			reread = store.findRecord(identity);
		} catch (RuntimeException readFailure) {
			log.warn("Could not re-read record after failed insert, leaving claim PENDING: domain={}, seq={}",
					identity.getDomainId(), claim.getSeq(), readFailure);
			return false;
		}
		if (reread.isPresent() && TraceReceiptChain.carriesClaim(reread.get(), claim.getSeq(), contentDigest)) {
			return true;
		}
		receiptChain.voidClaim(identity.getDomainId(), claim.getSeq());
		return false;
	}

	/**
	 * Commits the claim of a stored record. {@code CONFLICT} means the repair timer voided the claim
	 * while the insert was in flight (a fence breach): the record is an orphan carrying a {@code VOID}
	 * receipt. Records are immutable, so it is neither deleted nor correlated, and the request fails
	 * with {@code receipt_void_after_insert}.
	 */
	private AcceptanceResult settleAfterInsert(StoredTraceRecord stored, CorrelationPlan plan, String domainId,
			long seq) {
		ReceiptSettlement outcome = receiptChain.commit(domainId, seq);
		if (outcome == ReceiptSettlement.CONFLICT) {
			log.error("TRACE record stored under a VOID receipt: domain={}, producer={}, record_id={}, receipt_sequence={}",
					domainId, stored.getIdentity().getProducerId(), stored.getIdentity().getRecordId(), seq);
			throw voidAfterInsert(domainId, seq);
		}
		correlation.afterInsert(stored, plan);

		logAcceptance(stored, false);
		return AcceptanceResult.fromStored(stored, false);
	}

	private static TraceStorageException voidAfterInsert(String domainId, long seq) {
		return new TraceStorageException(REASON_VOID_AFTER_INSERT,
				"Record stored under a VOID receipt: domain=" + domainId + ", seq=" + seq);
	}

	/**
	 * Acknowledges {@code existing} as a replay when its {@code content_digest} matches, except when
	 * its receipt is {@code VOID}: that record is an orphan from {@link #settleAfterInsert} and must
	 * not be acknowledged. The identity stays blocked, and every retry fails with
	 * {@code receipt_void_after_insert}, until an operator intervenes. The extra receipt read happens
	 * on this replay path only.
	 */
	private AcceptanceResult classifyAgainstExisting(StoredTraceRecord existing, String contentDigest) {
		if (digestsEqual(existing.getContentDigest(), contentDigest)) {
			String domainId = existing.getIdentity().getDomainId();
			long seq = existing.getReceipt().getReceiptSequence();
			boolean orphan = store.findReceipt(domainId, seq)
					.filter(receipt -> receipt.getReceiptStateEnum() == TraceReceiptState.VOID).isPresent();
			if (orphan) {
				throw voidAfterInsert(domainId, seq);
			}
			logAcceptance(existing, true);
			return AcceptanceResult.fromStored(existing, true);
		}
		throw new TraceConflictException(TraceErrorResponseType.RECORD_CONFLICT, REASON_DIGEST_MISMATCH);
	}

	private StoredTraceRecord buildRecord(VerifiedAssertion verified, TraceRequestContext ctx, CorrelationPlan plan,
			ReceiptClaim claim) {
		ReceiptEntry receipt = new ReceiptEntry(claim.getSeq(), claim.getReceivedAtMs(), claim.getPrevReceiptHash(),
				claim.getReceiptHash());
		// version=0: not yet persisted; the store assigns the real initial version on insert,
		// and afterInsert() always re-reads the row before any CAS write.
		return new StoredTraceRecord(verified.getIdentity(), verified.getParsed().getRawText(),
				verified.getContentDigest(), verified.getVerification(), receipt, plan.getFlags(),
				plan.getExecution(), plan.getPosition(), verified.getInputs().getChainPosition().getPrevRecordHash(),
				verified.getInputs().getTokenRefs(),
				verified.getInputs().getEventKind(), verified.getInputs().getSignedAt(), ctx.getNodeId(),
				ctx.getReceivedAtMs(), 0L, verified.getInputs().getPolicy());
	}

	private void logAcceptance(StoredTraceRecord stored, boolean replay) {
		log.info(
				"TRACE record accepted: producer={}, record_id={}, receipt_sequence={}, coverage_gap={}, "
						+ "chain_link_failure={}, equivocation={}, late={}, policy_mismatch={}, reserved_member={}, "
						+ "replay={}",
				stored.getIdentity().getProducerId(), stored.getIdentity().getRecordId(),
				stored.getReceipt().getReceiptSequence(), stored.getFlags().isCoverageGap(),
				stored.getFlags().isChainLinkFailure(), stored.getFlags().isEquivocation(),
				stored.getFlags().isLate(), stored.getFlags().isPolicyMismatch(),
				stored.getFlags().isReservedMember(), replay);
	}

	private static boolean digestsEqual(String a, String b) {
		return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
	}

}
