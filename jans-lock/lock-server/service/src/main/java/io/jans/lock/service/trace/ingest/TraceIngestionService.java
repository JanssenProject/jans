/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.ingest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import org.slf4j.Logger;

import io.jans.lock.model.error.TraceErrorResponseType;
import io.jans.lock.model.trace.entity.TraceReceiptEntry;
import io.jans.lock.model.trace.entity.TraceReceiptState;
import io.jans.lock.service.trace.TraceConstants;
import io.jans.lock.service.trace.correlation.BatchOverlay;
import io.jans.lock.service.trace.correlation.CorrelationPlan;
import io.jans.lock.service.trace.correlation.TraceCorrelationService;
import io.jans.lock.service.trace.error.DuplicateEntryException;
import io.jans.lock.service.trace.error.TraceConflictException;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.ReceiptEntry;
import io.jans.lock.service.trace.model.ReceiptSettlement;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.receipt.ReceiptBatchItem;
import io.jans.lock.service.trace.receipt.ReceiptClaim;
import io.jans.lock.service.trace.receipt.ReceiptRun;
import io.jans.lock.service.trace.receipt.TraceReceiptChain;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.trace.verify.TraceVerificationService;
import io.jans.lock.service.trace.verify.VerifiedAssertion;
import io.jans.lock.trace.crypto.TraceCryptoException;
import io.jans.lock.trace.crypto.TraceCryptoProvider;
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

	@Inject
	private TraceCryptoProvider cryptoProvider;

	@Inject
	private TraceIngestExecutors executors;

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
		return ingestVerified(verification.verify(body, ctx), ctx);
	}

	/**
	 * Everything {@link #ingest} does after verification: the idempotency pre-check, correlation,
	 * receipt claim, insert and settlement. Used by {@code ingest}, by the batch fallback and by tests.
	 *
	 * @throws io.jans.lock.service.trace.error.TraceValidationException chain or genesis failure
	 * @throws TraceConflictException                                   {@code record_conflict}
	 * @throws TraceStorageException                                    {@code storage_failure}
	 */
	public AcceptanceResult ingestVerified(VerifiedAssertion verified, TraceRequestContext ctx) {
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
			Optional<StoredTraceRecord> raced = store.findRecord(identity);
			if (!raced.isPresent()) {
				receiptChain.voidClaim(domainId, claim.getSeq());
				throw new TraceStorageException(REASON_RACE_RECORD_MISSING,
						"Record missing after duplicate-entry race: " + identity);
			}
			if (!TraceReceiptChain.carriesClaim(raced.get(), claim.getSeq(), verified.getContentDigest())) {
				ReceiptSettlement voided = receiptChain.voidClaim(domainId, claim.getSeq());
				if (voided == ReceiptSettlement.CONFLICT) {
					log.warn("Duplicate-identity race loser claim is already COMMITTED: domain={}, seq={}",
							domainId, claim.getSeq());
				}
				return classifyAgainstExisting(raced.get(), verified.getContentDigest());
			}
			log.warn("Duplicate reported for a record that carries this claim: domain={}, seq={}", domainId,
					claim.getSeq());
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
		boolean orphan = false;
		if (digestsEqual(existing.getContentDigest(), contentDigest)) {
			orphan = store.findReceipt(existing.getIdentity().getDomainId(), existing.getReceipt().getReceiptSequence())
					.filter(receipt -> receipt.getReceiptStateEnum() == TraceReceiptState.VOID).isPresent();
		}
		return classifyExisting(existing, contentDigest, orphan);
	}

	/**
	 * @param orphan whether the receipt of {@code existing} is {@code VOID}; only read when the digests match
	 */
	private AcceptanceResult classifyExisting(StoredTraceRecord existing, String contentDigest, boolean orphan) {
		if (digestsEqual(existing.getContentDigest(), contentDigest)) {
			if (orphan) {
				throw voidAfterInsert(existing.getIdentity().getDomainId(),
						existing.getReceipt().getReceiptSequence());
			}
			logAcceptance(existing, true);
			return AcceptanceResult.fromStored(existing, true);
		}
		throw new TraceConflictException(TraceErrorResponseType.RECORD_CONFLICT, REASON_DIGEST_MISMATCH);
	}

	private StoredTraceRecord buildRecord(VerifiedAssertion verified, TraceRequestContext ctx, CorrelationPlan plan,
			ReceiptClaim claim) {
		return buildRecord(verified, ctx, plan, claim.getSeq(), claim.getPrevReceiptHash(), claim.getReceiptHash(),
				claim.getReceivedAtMs());
	}

	/**
	 * Builds the record to store from the receipt triple of a {@link ReceiptRun} item (or of a claim).
	 */
	StoredTraceRecord buildRecord(VerifiedAssertion verified, TraceRequestContext ctx, CorrelationPlan plan, long seq,
			String prevReceiptHash, String receiptHash, long receivedAtMs) {
		ReceiptEntry receipt = new ReceiptEntry(seq, receivedAtMs, prevReceiptHash, receiptHash);
		// version=0: not yet persisted; the store assigns the real initial version on insert,
		// and afterInsert() always re-reads the row before any CAS write.
		return new StoredTraceRecord(verified.getIdentity(), verified.getParsed().getRawText(),
				verified.getContentDigest(), verified.getVerification(), receipt, plan.getFlags(),
				plan.getExecution(), plan.getPosition(), verified.getInputs().getChainPosition().getPrevRecordHash(),
				verified.getInputs().getTokenRefs(),
				verified.getInputs().getEventKind(), verified.getInputs().getSignedAt(), ctx.getNodeId(),
				ctx.getReceivedAtMs(), 0L, verified.getInputs().getPolicy());
	}

	// -- batch ingestion ------------------------------------------------------------------------

	/**
	 * @return {@code true} when {@link #ingestBatch} can commit a batch atomically: the store
	 *         supports atomic batches and the crypto provider is available
	 */
	public boolean isBatchIngestAvailable() {
		return store.isAtomicIngestSupported() && cryptoProvider.isAvailable();
	}

	/**
	 * Ingests one submission through the batch pipeline as a batch of one: one database transaction
	 * for the receipt and the record instead of the three of {@link #ingest}. Same result, same
	 * exceptions and same stored state as {@link #ingest}; callers use it only when
	 * {@link #isBatchIngestAvailable()}.
	 *
	 * @throws io.jans.lock.service.trace.error.TraceCryptoException     as {@link #ingest}
	 * @throws io.jans.lock.service.trace.error.TraceValidationException as {@link #ingest}
	 * @throws TraceConflictException                                   as {@link #ingest}
	 * @throws TraceStorageException                                    as {@link #ingest}
	 */
	public AcceptanceResult ingestAsBatch(InputStream body, TraceRequestContext ctx) {
		VerifiedAssertion verified = verification.verify(body, ctx);
		BatchOutcome[] outcomes = new BatchOutcome[1];
		commitVerified(Collections.singletonList(new BatchItem(0, verified)), ctx, outcomes);
		BatchOutcome outcome = outcomes[0];
		if (!outcome.isAccepted()) {
			throw outcome.getError();
		}
		return outcome.getResult();
	}

	/**
	 * Ingests the elements of one bulk request with one database transaction instead of one per
	 * element, giving the same per-element outcomes and the same stored state as calling
	 * {@link #ingest} on each element in order.
	 *
	 * <ol>
	 * <li>Verify every element (no lock held), in parallel when {@code batchVerifyThreads} is set.
	 * A failure is that element's outcome.</li>
	 * <li>Read once the records that exist for the identities and once the records at the
	 * predecessor, same and successor positions of all elements. They seed a {@link BatchOverlay}.</li>
	 * <li>In input order, classify an element whose identity exists (stored, or planned earlier in
	 * this batch) as a replay or a conflict; plan the others against the overlay and add each
	 * success to it, so an element sees the earlier elements of the batch as already stored.</li>
	 * <li>Reserve one contiguous receipt range and insert all receipts and records in one transaction
	 * ({@link TraceReceiptChain#runWithRange}). On a position or identity collision the range is
	 * recomputed; identities that now exist are classified and dropped. Plans are not recomputed: the
	 * one-by-one path has the same window between planning and insert, which
	 * {@code afterInsert} corrects.</li>
	 * <li>After the commit run {@code afterInsert} for each stored record with one shared read of the
	 * chain positions; on the post-commit pool when {@code postCommitCorrelationThreads} is set, so
	 * the outcomes are returned without waiting for it.</li>
	 * <li>If the commit fails for any other reason nothing was stored by it: the elements without an
	 * outcome go one by one through {@link #ingestVerified}, which also reports an element whose
	 * transaction committed without the caller learning it as a replay.</li>
	 * </ol>
	 * An element is only reported accepted after its rows are committed.
	 *
	 * @return one outcome per element, in input order; never {@code null}
	 */
	public List<BatchOutcome> ingestBatch(List<BatchElement> elements, TraceRequestContext ctx) {
		BatchOutcome[] outcomes = new BatchOutcome[elements.size()];
		commitVerified(verifyAll(elements, ctx, outcomes), ctx, outcomes);
		return Arrays.asList(outcomes);
	}

	/** Steps 2-6 of {@link #ingestBatch} for the elements that passed verification. */
	private void commitVerified(List<BatchItem> items, TraceRequestContext ctx, BatchOutcome[] outcomes) {
		BatchStats stats = new BatchStats();
		if (!items.isEmpty()) {
			try {
				runBatch(items, ctx, outcomes, stats);
			} catch (RuntimeException ex) {
				fallbackToSinglePath(items, ctx, outcomes, stats, ex);
			}
		}
		logBatch(outcomes, stats);
	}

	/**
	 * Verifies every element, on the verification pool when one is configured. Verification only
	 * reads, so the order in which elements are verified does not change any outcome.
	 *
	 * @return the elements that passed, in input order; the others get their failure as outcome
	 */
	private List<BatchItem> verifyAll(List<BatchElement> elements, TraceRequestContext ctx, BatchOutcome[] outcomes) {
		List<BatchItem> items = new ArrayList<>(elements.size());
		Executor pool = elements.size() > 1 ? executors.verificationExecutor() : null;
		if (pool == null) {
			for (int index = 0; index < elements.size(); index++) {
				try {
					items.add(new BatchItem(index, verification.verify(elements.get(index).openStream(), ctx)));
				} catch (RuntimeException ex) {
					outcomes[index] = BatchOutcome.failed(ex);
				}
			}
			return items;
		}

		List<CompletableFuture<VerifiedAssertion>> futures = new ArrayList<>(elements.size());
		for (BatchElement element : elements) {
			futures.add(CompletableFuture.supplyAsync(() -> verification.verify(element.openStream(), ctx), pool));
		}
		for (int index = 0; index < elements.size(); index++) {
			try {
				items.add(new BatchItem(index, futures.get(index).join()));
			} catch (CompletionException ex) {
				outcomes[index] = BatchOutcome.failed(unwrap(ex));
			}
		}
		return items;
	}

	/** @return the exception {@code verify} threw; an {@link Error} is rethrown as the sequential path would */
	private static RuntimeException unwrap(CompletionException wrapper) {
		Throwable cause = wrapper.getCause();
		if (cause instanceof RuntimeException) {
			return (RuntimeException) cause;
		}
		if (cause instanceof Error) {
			throw (Error) cause;
		}
		return wrapper;
	}

	/** One element that passed verification. */
	private static final class BatchItem {

		private final int index;

		private final VerifiedAssertion verified;

		private CorrelationPlan plan;

		/** For a replay of an earlier element of the batch: that element. */
		private BatchItem duplicateOf;

		/** For a replay of an earlier element of the batch: the flags that element had at this point. */
		private IngestionFlags snapshot;

		private final List<BatchItem> duplicates = new ArrayList<>();

		private BatchItem(int index, VerifiedAssertion verified) {
			this.index = index;
			this.verified = verified;
		}

	}

	private static final class BatchStats {

		private int attempts;

		private boolean fallback;

	}

	/** State shared between {@code runWithRange}'s action and re-plan hook. */
	private static final class CommitState {

		private List<BatchItem> remaining;

		private List<StoredTraceRecord> stored;

	}

	private void runBatch(List<BatchItem> items, TraceRequestContext ctx, BatchOutcome[] outcomes, BatchStats stats) {
		String domainId = ctx.getEvidenceDomainId();

		Set<RecordIdentity> identities = new LinkedHashSet<>();
		Set<ChainPosition> positions = new LinkedHashSet<>();
		for (BatchItem item : items) {
			identities.add(item.verified.getIdentity());
			ChainPosition position = correlation.positionOf(item.verified, ctx);
			if (position.getSequenceNumber() > 1) {
				positions.add(new ChainPosition(position.getChainIdentity(), position.getSequenceNumber() - 1));
			}
			positions.add(position);
			positions.add(new ChainPosition(position.getChainIdentity(), position.getSequenceNumber() + 1));
		}
		Map<RecordIdentity, StoredTraceRecord> existing = store.findRecords(identities);
		Map<ChainPosition, List<StoredTraceRecord>> prefetched = store.findRecordsByChainPositions(positions);
		Map<Long, TraceReceiptState> receiptStates = loadReceiptStates(domainId, existing.values());
		BatchOverlay overlay = new BatchOverlay(existing, prefetched);

		Map<RecordIdentity, BatchItem> firstByIdentity = new HashMap<>();
		List<BatchItem> candidates = new ArrayList<>();
		for (BatchItem item : items) {
			RecordIdentity identity = item.verified.getIdentity();
			String digest = item.verified.getContentDigest();

			Optional<StoredTraceRecord> known = overlay.find(identity);
			if (known.isPresent()) {
				if (!overlay.isProvisional(identity)) {
					outcomes[item.index] = classifyOutcome(known.get(), digest, receiptStates);
				} else if (digestsEqual(known.get().getContentDigest(), digest)) {
					item.duplicateOf = firstByIdentity.get(identity);
					item.snapshot = known.get().getFlags();
					item.duplicateOf.duplicates.add(item);
				} else {
					outcomes[item.index] = BatchOutcome.failed(
							new TraceConflictException(TraceErrorResponseType.RECORD_CONFLICT, REASON_DIGEST_MISMATCH));
				}
				continue;
			}

			CorrelationPlan plan;
			try {
				plan = correlation.plan(item.verified, ctx, overlay);
			} catch (RuntimeException ex) {
				outcomes[item.index] = BatchOutcome.failed(ex);
				continue;
			}
			item.plan = plan;
			overlay.add(buildRecord(item.verified, ctx, plan, 0L, TraceConstants.ZERO_HASH, TraceConstants.ZERO_HASH,
					ctx.getReceivedAtMs()));
			firstByIdentity.put(identity, item);
			candidates.add(item);
		}
		if (candidates.isEmpty()) {
			return;
		}

		CommitState state = new CommitState();
		state.remaining = candidates;
		TraceReceiptChain.RangeAction action = run -> {
			stats.attempts++;
			state.stored = insertRun(run, state.remaining, ctx);
		};
		Supplier<List<ReceiptBatchItem>> replan = () -> {
			state.remaining = dropExisting(state.remaining, domainId, outcomes);
			return receiptItems(state.remaining, ctx);
		};
		ReceiptRun run = receiptChain.runWithRange(domainId, receiptItems(candidates, ctx), ctx.getNodeId(), action,
				replan);
		if (run.isEmpty()) {
			return;
		}

		List<StoredTraceRecord> stored = state.stored;
		for (int i = 0; i < state.remaining.size(); i++) {
			BatchItem item = state.remaining.get(i);
			StoredTraceRecord record = stored.get(i);
			logAcceptance(record, false);
			outcomes[item.index] = BatchOutcome.accepted(AcceptanceResult.fromStored(record, false));
			for (BatchItem duplicate : item.duplicates) {
				StoredTraceRecord replayed = record.withFlags(duplicate.snapshot, record.getVersion());
				logAcceptance(replayed, true);
				outcomes[duplicate.index] = BatchOutcome.accepted(AcceptanceResult.fromStored(replayed, true));
			}
		}
		schedulePostCommit(state.remaining, stored);
	}

	/**
	 * Runs {@link #afterInsertBatch} on the post-commit pool, or here when none is configured. The
	 * outcomes are already final, and {@code afterInsert} only ever corrects neighbor flags through
	 * compare-and-swap, so running it later or concurrently with other requests changes no result.
	 */
	private void schedulePostCommit(List<BatchItem> items, List<StoredTraceRecord> records) {
		Executor pool = executors.correlationExecutor();
		if (pool == null) {
			afterInsertBatch(items, records);
			return;
		}
		pool.execute(() -> {
			try {
				afterInsertBatch(items, records);
			} catch (RuntimeException ex) {
				log.error("TRACE batch post-commit correlation failed: size={}", items.size(), ex);
			}
		});
	}

	private List<StoredTraceRecord> insertRun(ReceiptRun run, List<BatchItem> remaining, TraceRequestContext ctx)
			throws DuplicateEntryException {
		List<StoredTraceRecord> records = new ArrayList<>(remaining.size());
		for (int i = 0; i < remaining.size(); i++) {
			BatchItem item = remaining.get(i);
			ReceiptRun.Position position = run.getPositions().get(i);
			records.add(buildRecord(item.verified, ctx, item.plan, position.getSeq(), position.getPrevReceiptHash(),
					position.getReceiptHash(), ctx.getReceivedAtMs()));
		}
		List<TraceReceiptEntry> rows = run.getRows();
		store.insertIngestBatch(rows, records);
		return records;
	}

	private List<ReceiptBatchItem> receiptItems(List<BatchItem> candidates, TraceRequestContext ctx) {
		List<ReceiptBatchItem> list = new ArrayList<>(candidates.size());
		for (BatchItem item : candidates) {
			list.add(new ReceiptBatchItem(item.verified.getProducer(), item.verified.getRecordId(),
					traceKeys.recordKey(item.verified.getIdentity()), item.verified.getContentDigest(),
					ctx.getReceivedAtMs()));
		}
		return list;
	}

	/**
	 * After a collision: records that another writer stored for the identities of {@code remaining}
	 * are classified (replay or conflict) and dropped, together with the in-batch replays of them.
	 *
	 * @return the candidates that still need to be stored
	 */
	private List<BatchItem> dropExisting(List<BatchItem> remaining, String domainId, BatchOutcome[] outcomes) {
		Set<RecordIdentity> identities = new LinkedHashSet<>();
		for (BatchItem item : remaining) {
			identities.add(item.verified.getIdentity());
		}
		Map<RecordIdentity, StoredTraceRecord> now = store.findRecords(identities);
		if (now.isEmpty()) {
			return remaining;
		}
		Map<Long, TraceReceiptState> receiptStates = loadReceiptStates(domainId, now.values());
		List<BatchItem> kept = new ArrayList<>(remaining.size());
		for (BatchItem item : remaining) {
			StoredTraceRecord found = now.get(item.verified.getIdentity());
			if (found == null) {
				kept.add(item);
				continue;
			}
			BatchOutcome outcome = classifyOutcome(found, item.verified.getContentDigest(), receiptStates);
			outcomes[item.index] = outcome;
			for (BatchItem duplicate : item.duplicates) {
				outcomes[duplicate.index] = outcome;
			}
		}
		return kept;
	}

	private BatchOutcome classifyOutcome(StoredTraceRecord existing, String digest,
			Map<Long, TraceReceiptState> receiptStates) {
		try {
			boolean orphan = receiptStates.get(existing.getReceipt().getReceiptSequence()) == TraceReceiptState.VOID;
			return BatchOutcome.accepted(classifyExisting(existing, digest, orphan));
		} catch (RuntimeException ex) {
			return BatchOutcome.failed(ex);
		}
	}

	/** One read of the receipts of the given records, by sequence. */
	private Map<Long, TraceReceiptState> loadReceiptStates(String domainId, Collection<StoredTraceRecord> records) {
		if (records.isEmpty()) {
			return Collections.emptyMap();
		}
		Set<Long> sequences = new LinkedHashSet<>();
		for (StoredTraceRecord record : records) {
			sequences.add(record.getReceipt().getReceiptSequence());
		}
		Map<Long, TraceReceiptState> states = new HashMap<>();
		for (TraceReceiptEntry receipt : store.findReceipts(domainId, sequences)) {
			states.put(receipt.getReceiptSeq(), receipt.getReceiptStateEnum());
		}
		return states;
	}

	private static final int POSITION_READ_ATTEMPTS = 3;

	/** Best-effort: the commit already happened, so a failure here never changes an outcome. */
	private void afterInsertBatch(List<BatchItem> items, List<StoredTraceRecord> records) {
		Set<ChainPosition> positions = new LinkedHashSet<>();
		for (BatchItem item : items) {
			positions.add(item.plan.getPosition());
		}
		Map<ChainPosition, List<StoredTraceRecord>> rows = null;
		for (int attempt = 1; attempt <= POSITION_READ_ATTEMPTS && rows == null; attempt++) {
			try {
				rows = store.findRecordsByChainPositions(positions);
			} catch (RuntimeException ex) {
				log.warn("TRACE batch afterInsert chain position read failed, attempt {}", attempt, ex);
			}
		}
		for (int i = 0; i < items.size(); i++) {
			try {
				correlation.afterInsert(records.get(i), items.get(i).plan, rows);
			} catch (RuntimeException ex) {
				log.error("TRACE batch afterInsert failed: inserted={}", records.get(i).getIdentity(), ex);
			}
		}
	}

	/**
	 * The batch could not be committed as a unit; nothing was stored by the failed attempt. Every
	 * element without an outcome goes through the one-by-one path, whose outcome it keeps.
	 */
	private void fallbackToSinglePath(List<BatchItem> items, TraceRequestContext ctx, BatchOutcome[] outcomes,
			BatchStats stats, RuntimeException cause) {
		stats.fallback = true;
		int pending = 0;
		for (BatchItem item : items) {
			if (outcomes[item.index] == null) {
				pending++;
			}
		}
		log.warn("TRACE batch ingest falls back to per-record ingest: size={}, pending={}, reason={}", outcomes.length,
				pending, describe(cause));
		log.debug("TRACE batch ingest failure", cause);
		for (BatchItem item : items) {
			if (outcomes[item.index] != null) {
				continue;
			}
			try {
				outcomes[item.index] = BatchOutcome.accepted(ingestVerified(item.verified, ctx));
			} catch (RuntimeException ex) {
				outcomes[item.index] = BatchOutcome.failed(ex);
			}
		}
	}

	private static String describe(RuntimeException cause) {
		if (cause instanceof TraceStorageException) {
			return "TraceStorageException:" + ((TraceStorageException) cause).getReason();
		}
		return cause.getClass().getSimpleName();
	}

	private void logBatch(BatchOutcome[] outcomes, BatchStats stats) {
		if (!log.isDebugEnabled()) {
			return;
		}
		int accepted = 0;
		int replays = 0;
		int rejected = 0;
		for (BatchOutcome outcome : outcomes) {
			if (!outcome.isAccepted()) {
				rejected++;
			} else if (outcome.getResult().isIdempotentReplay()) {
				replays++;
			} else {
				accepted++;
			}
		}
		log.debug("TRACE batch ingest: size={}, accepted={}, replays={}, rejected={}, attempts={}, fallback={}",
				outcomes.length, accepted, replays, rejected, stats.attempts, stats.fallback);
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
