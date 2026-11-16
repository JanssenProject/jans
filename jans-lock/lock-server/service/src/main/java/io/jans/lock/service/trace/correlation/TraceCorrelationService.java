/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;

import io.jans.lock.model.config.AppConfiguration;
import io.jans.lock.model.trace.config.ChainRegistrationMode;
import io.jans.lock.service.trace.error.TraceStorageException;
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.parse.ParsedAssertion;
import io.jans.lock.service.trace.registry.ProducerChainRegistry;
import io.jans.lock.service.trace.store.TraceKeys;
import io.jans.lock.service.trace.store.TraceStore;
import io.jans.lock.service.trace.validate.CorrelationInputs;
import io.jans.lock.service.trace.verify.VerifiedAssertion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Computes the producer-chain checks, ingestion flags and index projection for a verified record
 * before it is stored, and, once the store confirms the insert, updates the derived flags on
 * neighboring rows (design §8, §12 table rows for missing/wrong predecessor, equivocation and
 * lateness, §13).
 *
 * <p>{@link #plan} never touches the store for writes; {@link #afterInsert} is the only method
 * that mutates other rows, and it does so best-effort: a failure updating
 * one neighbor is logged (identities only) and never fails the request. Each neighbor write goes
 * through {@link TraceStore#updateRecordFlags}'s compare-and-swap, keyed off the version last
 * observed on that row; a lost race ({@link TraceStore#VERSION_CONFLICT_REASON}) re-reads the row
 * fresh, recomputes the flag(s) from that fresh state and retries, bounded by
 * {@code traceConfiguration.correlationRetryLimit} (default 3), so a lost race never overwrites
 * another writer's flag change. Exhausting the retry budget is logged at WARN and the update is given up on, same as any other best-effort
 * failure here.
 *
 * <p>{@link #afterInsert} also corrects the inserted record itself. Its {@code plan} read the chain
 * before the insert, so a predecessor that another writer stored in the meantime was not seen. After
 * the insert the predecessor rows are read again, and {@code coverage_gap} of the inserted record is
 * cleared when a predecessor now exists, and {@code chain_link_failure} is set when predecessors now
 * exist and none of their digests matches its {@code prev_record_hash}. Both corrections only go in
 * the direction the one-by-one ingest of the same records would reach: a coverage gap is never set
 * here and a chain link failure is never cleared (design §8). {@code equivocation} is only ever
 * set, never cleared, and {@code late} and {@code policy_mismatch} are never touched.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceCorrelationService {

	private static final int POSITION_READ_ATTEMPTS = 3;

	@Inject
	private Logger log;

	@Inject
	private TraceStore store;

	@Inject
	private ProducerChainRegistry chainRegistry;

	@Inject
	private AppConfiguration appConfiguration;

	@Inject
	private TraceKeys traceKeys;

	@Inject
	private PolicyMismatchCheck policyMismatchCheck;

	private final PlanLookup storeLookup = position -> store.findRecordsByChainPosition(position);

	/** Last unknown {@code chainRegistrationMode} value already reported, so it is logged once. */
	private volatile String warnedChainRegistrationMode;

	/**
	 * @throws io.jans.lock.service.trace.error.TraceValidationException {@code chain_not_registered}
	 * or {@code invalid_genesis}
	 */
	public CorrelationPlan plan(VerifiedAssertion verified, TraceRequestContext ctx) {
		return plan(verified, ctx, storeLookup);
	}

	/**
	 * Same as {@link #plan(VerifiedAssertion, TraceRequestContext)}, reading the existing records
	 * from {@code lookup} instead of the store.
	 *
	 * @throws io.jans.lock.service.trace.error.TraceValidationException {@code chain_not_registered}
	 * or {@code invalid_genesis}
	 */
	public CorrelationPlan plan(VerifiedAssertion verified, TraceRequestContext ctx, PlanLookup lookup) {
		CorrelationInputs inputs = verified.getInputs();
		ParsedAssertion.ProducerChain producerChain = inputs.getChainPosition();
		String domainId = ctx.getEvidenceDomainId();

		boolean late = ProducerChainRules.isLate(ctx.getReceivedAtMs(), inputs.getSignedAt(),
				appConfiguration.getTraceConfiguration().getLatenessThresholdSeconds());
		// signed_at is a producer claim: judge at receipt unless the record is already flagged late
		long policyJudgedAtMs = late ? inputs.getSignedAt() * 1000L : ctx.getReceivedAtMs();
		boolean policyMismatch = policyMismatchCheck.check(inputs.getPolicy(), policyJudgedAtMs);

		ChainPosition position = positionOf(verified, ctx);
		ChainIdentity chainIdentity = position.getChainIdentity();
		long sequenceNumber = position.getSequenceNumber();

		if (!lookup.isChainConfirmed(chainIdentity)) {
			if (chainRegistrationMode() == ChainRegistrationMode.FIRST_OBSERVED) {
				chainRegistry.requireRegisteredOrObserveGenesis(chainIdentity, sequenceNumber,
						producerChain.getPrevRecordHash(), ctx.getClientId(), ctx.getReceivedAtMs());
			} else {
				chainRegistry.requireRegistered(chainIdentity);
			}
			lookup.confirmChain(chainIdentity);
		}
		chainRegistry.checkGenesis(sequenceNumber, producerChain.getPrevRecordHash());

		List<StoredTraceRecord> predecessors = sequenceNumber > 1
				? lookup.findRecordsByChainPosition(new ChainPosition(chainIdentity, sequenceNumber - 1))
				: Collections.emptyList();
		boolean coverageGap = ProducerChainRules.isCoverageGap(sequenceNumber, predecessors);
		boolean chainLinkFailure = ProducerChainRules.isChainLinkFailure(sequenceNumber, predecessors,
				producerChain.getPrevRecordHash());

		List<StoredTraceRecord> rowsAtPosition = lookup.findRecordsByChainPosition(position);
		List<RecordIdentity> equivocationPeers = ProducerChainRules.equivocationPeers(verified.getIdentity(),
				rowsAtPosition);
		boolean equivocation = !equivocationPeers.isEmpty();

		List<StoredTraceRecord> successors = lookup
				.findRecordsByChainPosition(new ChainPosition(chainIdentity, sequenceNumber + 1));

		IngestionFlags flags = new IngestionFlags(coverageGap, chainLinkFailure, equivocation, late, policyMismatch);
		List<String> reservedMembers = verified.getParsed().getReservedMembers();
		if (!reservedMembers.isEmpty()) {
			log.warn("TRACE record carries {} Lock-derived members, ignored: producer={}, record_id={}, first={}",
					verified.getParsed().getReservedMemberCount(), verified.getProducer(), verified.getRecordId(),
					reservedMembers);
			flags.setReservedMembers(reservedMembers);
		}

		ExecutionIdentity execution = new ExecutionIdentity(domainId, inputs.getExecutionAuthority(),
				inputs.getTraceExecutionId());

		List<String> tokenKeys = dedupKeys(inputs.getTokenRefs(), token -> traceKeys.tokenKey(domainId, token));

		return new CorrelationPlan(flags, position, execution, tokenKeys, equivocationPeers,
				successors, rowsAtPosition, inputs.getWarnings());
	}

	/**
	 * @return the producer-chain position the record claims in the request's evidence domain
	 */
	public ChainPosition positionOf(VerifiedAssertion verified, TraceRequestContext ctx) {
		ParsedAssertion.ProducerChain producerChain = verified.getInputs().getChainPosition();
		ChainIdentity chainIdentity = new ChainIdentity(ctx.getEvidenceDomainId(), producerChain.getProducerId(),
				producerChain.getProducerInstanceId(), producerChain.getProducerChainId());
		return new ChainPosition(chainIdentity, producerChain.getSequenceNumber());
	}

	private ChainRegistrationMode chainRegistrationMode() {
		String configured = appConfiguration.getTraceConfiguration().getChainRegistrationMode();
		ChainRegistrationMode mode = ChainRegistrationMode.fromString(configured);
		if (mode != null) {
			return mode;
		}
		if (!configured.equals(warnedChainRegistrationMode)) {
			warnedChainRegistrationMode = configured;
			log.warn("Unknown TRACE chainRegistrationMode '{}', treating as {}", configured,
					ChainRegistrationMode.PRE_REGISTERED);
		}
		return ChainRegistrationMode.PRE_REGISTERED;
	}

	/**
	 * Reads the predecessor, own and successor positions of {@code position} with one
	 * {@code findRecordsByChainPositions} call, retrying a transient failure.
	 *
	 * @return the rows per position, or {@code null} when every attempt failed: the rows are then
	 *         unknown, the inserted record keeps its own flags, only the planned successors and
	 *         peers are updated, and a neighbor that arrived after {@code plan()} goes uncorrelated,
	 *         which is logged at error level
	 */
	private Map<ChainPosition, List<StoredTraceRecord>> readNeighborhood(StoredTraceRecord inserted,
			ChainPosition position) {
		List<ChainPosition> positions = ProducerChainRules.neighborhood(position);
		for (int attempt = 1; attempt <= POSITION_READ_ATTEMPTS; attempt++) {
			try {
				return store.findRecordsByChainPositions(positions);
			} catch (RuntimeException ex) {
				if (attempt == POSITION_READ_ATTEMPTS) {
					log.error("TRACE correlation afterInsert failed to re-query chain positions after {} attempts: inserted={}",
							attempt, inserted.getIdentity(), ex);
				} else {
					log.warn("TRACE correlation afterInsert chain positions re-query failed, attempt {}: inserted={}",
							attempt, inserted.getIdentity(), ex);
				}
			}
		}
		return null;
	}

	/**
	 * Corrects the flags around {@code inserted} after it became visible in the store. {@link #plan}
	 * read the chain before the insert, so any neighbor that another writer stored in between was
	 * not seen. This method reads the predecessor, own and successor positions of {@code inserted}
	 * once, after the insert, and then, in this order:
	 * <ol>
	 * <li>corrects the inserted record's own {@code coverage_gap} (cleared when a predecessor now
	 * exists) and {@code chain_link_failure} (set when predecessors now exist and none matches its
	 * {@code prev_record_hash}), with the same rules as {@link #plan};</li>
	 * <li>updates the records now stored at {@code sequence_number + 1}: every successor the plan
	 * knew, and every other one whose coverage gap is still set.
	 * It clears the coverage gap and sets the chain link failure when the successor's
	 * {@code prev_record_hash} matches no record now at the inserted position (the inserted one or
	 * an equivocation peer), the rule {@link #plan} applies to a record's own predecessors;</li>
	 * <li>flags as equivocating every other record at the inserted position, and the inserted record
	 * itself when a peer arrived that the plan did not know.</li>
	 * </ol>
	 * A record whose flags already agree is not written. Every write is a compare-and-swap on the
	 * version of the row as read; a lost race re-reads that row and recomputes. Best-effort
	 * throughout: a failure updating one row is logged and does not propagate.
	 */
	public void afterInsert(StoredTraceRecord inserted, CorrelationPlan plan) {
		afterInsert(inserted, plan, null);
	}

	/**
	 * Same as {@link #afterInsert(StoredTraceRecord, CorrelationPlan)} with the rows already read by
	 * the caller (one {@code findRecordsByChainPositions} for a whole batch, after its commit).
	 *
	 * @param positionRows rows per chain position as read after the insert; it must cover the
	 *                     positions of {@link ProducerChainRules#neighborhood} of the insert, and a
	 *                     position absent from the map counts as empty. {@code null} makes this method
	 *                     read them itself.
	 */
	public void afterInsert(StoredTraceRecord inserted, CorrelationPlan plan,
			Map<ChainPosition, List<StoredTraceRecord>> positionRows) {
		ChainPosition position = plan.getPosition();
		Map<ChainPosition, List<StoredTraceRecord>> rows = positionRows != null ? positionRows
				: readNeighborhood(inserted, position);
		if (rows != null) {
			correctOwnFlags(inserted, position, rows);
		}
		updateSuccessors(inserted, plan, rows);
		updateEquivocation(inserted, plan, rows == null ? Collections.<StoredTraceRecord>emptyList()
				: rowsAt(rows, position));
	}

	private void correctOwnFlags(StoredTraceRecord inserted, ChainPosition position,
			Map<ChainPosition, List<StoredTraceRecord>> rows) {
		long sequenceNumber = position.getSequenceNumber();
		List<StoredTraceRecord> predecessors = sequenceNumber > 1
				? rowsAt(rows, new ChainPosition(position.getChainIdentity(), sequenceNumber - 1))
				: Collections.<StoredTraceRecord>emptyList();
		Function<StoredTraceRecord, IngestionFlags> mutator = stored -> {
			IngestionFlags flags = stored.getFlags();
			boolean changed = false;
			if (flags.isCoverageGap() && !ProducerChainRules.isCoverageGap(sequenceNumber, predecessors)) {
				flags.setCoverageGap(false);
				changed = true;
			}
			// design §8: chainLinkFailure is never self-clearing, only ever set here.
			if (!flags.isChainLinkFailure() && ProducerChainRules.isChainLinkFailure(sequenceNumber, predecessors,
					stored.getPrevRecordHash())) {
				flags.setChainLinkFailure(true);
				changed = true;
			}
			return changed ? flags : null;
		};
		StoredTraceRecord observed = find(rowsAt(rows, position), inserted.getIdentity());
		if (observed == null && mutator.apply(inserted) == null) {
			// Both corrections are one-way, so flags that agree as planned agree as stored too.
			return;
		}
		updateFlags(inserted.getIdentity(), observed, mutator, "correct own flags", inserted.getIdentity());
	}

	/**
	 * A successor the plan knew was planned before {@code inserted} existed, so it is updated as the
	 * one-by-one path would update it. A successor the plan did not know may have been planned
	 * either before or after {@code inserted} became visible. It is updated only while its coverage
	 * gap is still set: its own plan then saw no predecessor, so it cannot have taken
	 * {@code inserted} into account. Otherwise its chain link was already judged against the
	 * predecessors it saw.
	 *
	 * <p>A successor's link is judged against every record at the inserted position, as read after
	 * the insert (or as the plan saw them when that read failed), plus {@code inserted}: one
	 * matching digest is enough, as in {@link ProducerChainRules#isChainLinkFailure}.
	 */
	private void updateSuccessors(StoredTraceRecord inserted, CorrelationPlan plan,
			Map<ChainPosition, List<StoredTraceRecord>> rows) {
		ChainPosition position = plan.getPosition();
		List<StoredTraceRecord> predecessors = withInserted(
				rows != null ? rowsAt(rows, position) : plan.getPositionRows(), inserted);
		long successorSequence = position.getSequenceNumber() + 1;
		Set<RecordIdentity> planned = new HashSet<>();
		for (StoredTraceRecord successor : plan.getSuccessors()) {
			planned.add(successor.getIdentity());
		}
		Set<RecordIdentity> updated = new HashSet<>();
		if (rows != null) {
			ChainPosition next = new ChainPosition(position.getChainIdentity(), successorSequence);
			for (StoredTraceRecord successor : rowsAt(rows, next)) {
				RecordIdentity successorId = successor.getIdentity();
				updated.add(successorId);
				boolean known = planned.contains(successorId);
				if (known || (successor.getFlags() != null && successor.getFlags().isCoverageGap())) {
					updateSuccessor(inserted, predecessors, successorSequence, successorId, successor, !known);
				}
			}
		}
		for (StoredTraceRecord successor : plan.getSuccessors()) {
			if (updated.add(successor.getIdentity())) {
				updateSuccessor(inserted, predecessors, successorSequence, successor.getIdentity(), null, false);
			}
		}
	}

	/** @return {@code rows} with {@code inserted} added when it is not among them */
	private static List<StoredTraceRecord> withInserted(List<StoredTraceRecord> rows, StoredTraceRecord inserted) {
		if (find(rows, inserted.getIdentity()) != null) {
			return rows;
		}
		List<StoredTraceRecord> all = new ArrayList<>(rows.size() + 1);
		all.addAll(rows);
		all.add(inserted);
		return all;
	}

	private void updateEquivocation(StoredTraceRecord inserted, CorrelationPlan plan,
			List<StoredTraceRecord> rowsAtPosition) {
		// equivocation is never cleared, so rows already flagged need no per-row re-read; this keeps
		// the work for one insert from growing with the number of records already at the position
		Set<RecordIdentity> unseenPlanPeers = new LinkedHashSet<>(plan.getEquivocationPeers());
		boolean newPeerFound = false;
		boolean insertedFlagged = inserted.getFlags() != null && inserted.getFlags().isEquivocation();
		for (StoredTraceRecord row : rowsAtPosition) {
			RecordIdentity rowId = row.getIdentity();
			boolean rowFlagged = row.getFlags() != null && row.getFlags().isEquivocation();
			if (rowId.equals(inserted.getIdentity())) {
				insertedFlagged = rowFlagged;
				continue;
			}
			if (!unseenPlanPeers.remove(rowId)) {
				newPeerFound = true;
			}
			if (!rowFlagged) {
				updatePeer(inserted.getIdentity(), rowId);
			}
		}
		for (RecordIdentity peerId : unseenPlanPeers) {
			updatePeer(inserted.getIdentity(), peerId);
		}
		if (newPeerFound && !insertedFlagged) {
			// inserted's own equivocation flag was computed against the same stale snapshot that
			// missed this peer, so it needs the same correction, via the same CAS path.
			updatePeer(inserted.getIdentity(), inserted.getIdentity());
		}
	}

	private static List<StoredTraceRecord> rowsAt(Map<ChainPosition, List<StoredTraceRecord>> rows,
			ChainPosition position) {
		List<StoredTraceRecord> found = rows.get(position);
		return found == null ? Collections.<StoredTraceRecord>emptyList() : found;
	}

	private static StoredTraceRecord find(List<StoredTraceRecord> rows, RecordIdentity identity) {
		for (StoredTraceRecord row : rows) {
			if (row.getIdentity().equals(identity)) {
				return row;
			}
		}
		return null;
	}

	private void updatePeer(RecordIdentity insertedId, RecordIdentity peerId) {
		updateFlags(peerId, null, stored -> {
			IngestionFlags flags = stored.getFlags();
			if (flags.isEquivocation()) {
				return null;
			}
			flags.setEquivocation(true);
			return flags;
		}, "flag equivocation peer", insertedId);
	}

	/**
	 * @param predecessors      the records at the inserted position, {@code inserted} included
	 * @param successorSequence the successor's {@code sequence_number}
	 * @param observed          the successor's row as read after the insert, or {@code null} to read it here
	 * @param whileGapOnly      {@code true} to change nothing once the successor's coverage gap is clear,
	 *                          also on a re-read after a lost compare-and-swap
	 */
	private void updateSuccessor(StoredTraceRecord inserted, List<StoredTraceRecord> predecessors,
			long successorSequence, RecordIdentity successorId, StoredTraceRecord observed, boolean whileGapOnly) {
		updateFlags(successorId, observed, stored -> {
			IngestionFlags flags = stored.getFlags();
			if (whileGapOnly && !flags.isCoverageGap()) {
				return null;
			}
			boolean changed = false;
			if (flags.isCoverageGap()) {
				flags.setCoverageGap(false);
				changed = true;
			}
			// design §8: chainLinkFailure is never self-clearing, only ever set here.
			if (!flags.isChainLinkFailure() && ProducerChainRules.isChainLinkFailure(successorSequence, predecessors,
					stored.getPrevRecordHash())) {
				flags.setChainLinkFailure(true);
				changed = true;
			}
			return changed ? flags : null;
		}, "update successor", inserted.getIdentity());
	}

	/**
	 * Shared retry-on-version-conflict logic for a single neighbor write.
	 * {@code mutator} recomputes the flags to write from the current {@link StoredTraceRecord} (the
	 * row passed as {@code observed} or else an initial read, or a fresh re-read after a lost CAS
	 * race), returning {@code null} when nothing needs to change (so no write is attempted). A
	 * {@link TraceStorageException} whose reason is not {@link TraceStore#VERSION_CONFLICT_REASON},
	 * or any other {@link RuntimeException}, is logged at ERROR and swallowed (today's best-effort
	 * contract, unchanged); exhausting the retry budget on a genuine version conflict is logged at
	 * WARN instead.
	 *
	 * @param observed the target's row as already read after the insert, or {@code null} to read it
	 */
	private void updateFlags(RecordIdentity targetId, StoredTraceRecord observed,
			Function<StoredTraceRecord, IngestionFlags> mutator, String action, RecordIdentity insertedId) {
		try {
			Optional<StoredTraceRecord> current = observed != null ? Optional.of(observed) : store.findRecord(targetId);
			int retryLimit = appConfiguration.getTraceConfiguration().getCorrelationRetryLimit();
			for (int attempt = 0; current.isPresent() && attempt <= retryLimit; attempt++) {
				StoredTraceRecord stored = current.get();
				IngestionFlags newFlags = mutator.apply(stored);
				if (newFlags == null || writeFlags(targetId, newFlags, stored.getVersion())) {
					return;
				}
				if (attempt == retryLimit) {
					log.warn(
							"TRACE correlation afterInsert exhausted {} retries to {} (version conflict): inserted={}, target={}",
							retryLimit, action, insertedId, targetId);
					return;
				}
				current = store.findRecord(targetId);
			}
		} catch (RuntimeException ex) {
			log.error("TRACE correlation afterInsert failed to {}: inserted={}, target={}", action, insertedId,
					targetId, ex);
		}
	}

	/**
	 * @return {@code true} when written, {@code false} on a version conflict
	 * @throws TraceStorageException for any other storage failure
	 */
	private boolean writeFlags(RecordIdentity targetId, IngestionFlags flags, long version) {
		try {
			store.updateRecordFlags(targetId, flags, version);
			return true;
		} catch (TraceStorageException ex) {
			if (!TraceStore.VERSION_CONFLICT_REASON.equals(ex.getReason())) {
				throw ex;
			}
			return false;
		}
	}

	private <T> List<String> dedupKeys(List<T> items, Function<T, String> keyFn) {
		Set<String> dedup = new LinkedHashSet<>();
		for (T item : items) {
			dedup.add(keyFn.apply(item));
		}
		return new ArrayList<>(dedup);
	}

}
