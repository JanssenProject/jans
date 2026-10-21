/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
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
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class TraceCorrelationService {

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

	/** Last unknown {@code chainRegistrationMode} value already reported, so it is logged once. */
	private volatile String warnedChainRegistrationMode;

	/**
	 * @throws io.jans.lock.service.trace.error.TraceValidationException {@code chain_not_registered}
	 * or {@code invalid_genesis}
	 */
	public CorrelationPlan plan(VerifiedAssertion verified, TraceRequestContext ctx) {
		CorrelationInputs inputs = verified.getInputs();
		ParsedAssertion.ProducerChain producerChain = inputs.getChainPosition();
		String domainId = ctx.getEvidenceDomainId();

		boolean policyMismatch = policyMismatchCheck.check(inputs.getPolicy(), inputs.getSignedAt() * 1000L);

		ChainIdentity chainIdentity = new ChainIdentity(domainId, producerChain.getProducerId(),
				producerChain.getProducerInstanceId(), producerChain.getProducerChainId());
		long sequenceNumber = producerChain.getSequenceNumber();
		ChainPosition position = new ChainPosition(chainIdentity, sequenceNumber);

		if (chainRegistrationMode() == ChainRegistrationMode.FIRST_OBSERVED) {
			chainRegistry.requireRegisteredOrObserveGenesis(chainIdentity, sequenceNumber,
					producerChain.getPrevRecordHash(), ctx.getClientId(), ctx.getReceivedAtMs());
		} else {
			chainRegistry.requireRegistered(chainIdentity);
		}
		chainRegistry.checkGenesis(sequenceNumber, producerChain.getPrevRecordHash());

		List<StoredTraceRecord> predecessors = sequenceNumber > 1
				? store.findRecordsByChainPosition(new ChainPosition(chainIdentity, sequenceNumber - 1))
				: Collections.emptyList();
		boolean coverageGap = ProducerChainRules.isCoverageGap(sequenceNumber, predecessors);
		boolean chainLinkFailure = ProducerChainRules.isChainLinkFailure(sequenceNumber, predecessors,
				producerChain.getPrevRecordHash());

		List<StoredTraceRecord> rowsAtPosition = store.findRecordsByChainPosition(position);
		List<RecordIdentity> equivocationPeers = ProducerChainRules.equivocationPeers(verified.getIdentity(),
				rowsAtPosition);
		boolean equivocation = !equivocationPeers.isEmpty();

		List<StoredTraceRecord> successors = store
				.findRecordsByChainPosition(new ChainPosition(chainIdentity, sequenceNumber + 1));

		boolean late = ProducerChainRules.isLate(ctx.getReceivedAtMs(), inputs.getSignedAt(),
				appConfiguration.getTraceConfiguration().getLatenessThresholdSeconds());

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
				successors, inputs.getWarnings());
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
	 * Updates the equivocation peers and the successor at {@code sequence_number + 1} after
	 * {@code inserted} became visible in the store, then re-queries the
	 * insert's own chain position: {@link #plan} computed {@code equivocationPeers} from a
	 * pre-insert snapshot, so a peer that landed at the same position between this node's
	 * {@code plan()} and its own {@code insertRecord()} would otherwise never be flagged, and
	 * neither would {@code inserted}'s own {@code equivocation} (computed against that same stale
	 * snapshot). Best-effort throughout: a failure updating one neighbor is logged and does not
	 * propagate.
	 */
	public void afterInsert(StoredTraceRecord inserted, CorrelationPlan plan) {
		for (RecordIdentity peerId : plan.getEquivocationPeers()) {
			updatePeer(inserted.getIdentity(), peerId);
		}
		for (StoredTraceRecord successor : plan.getSuccessors()) {
			updateSuccessor(inserted, successor.getIdentity());
		}

		Set<RecordIdentity> knownPeers = new LinkedHashSet<>(plan.getEquivocationPeers());
		boolean newPeerFound = false;
		for (StoredTraceRecord row : store.findRecordsByChainPosition(plan.getPosition())) {
			RecordIdentity rowId = row.getIdentity();
			if (rowId.equals(inserted.getIdentity()) || knownPeers.contains(rowId)) {
				continue;
			}
			newPeerFound = true;
			updatePeer(inserted.getIdentity(), rowId);
		}
		if (newPeerFound) {
			// inserted's own equivocation flag was computed against the same stale snapshot that
			// missed this peer, so it needs the same correction, via the same CAS path.
			updatePeer(inserted.getIdentity(), inserted.getIdentity());
		}
	}

	private void updatePeer(RecordIdentity insertedId, RecordIdentity peerId) {
		updateFlags(peerId, stored -> {
			IngestionFlags flags = stored.getFlags();
			if (flags.isEquivocation()) {
				return null;
			}
			flags.setEquivocation(true);
			return flags;
		}, "flag equivocation peer", insertedId);
	}

	private void updateSuccessor(StoredTraceRecord inserted, RecordIdentity successorId) {
		updateFlags(successorId, stored -> {
			IngestionFlags flags = stored.getFlags();
			boolean changed = false;
			if (flags.isCoverageGap()) {
				flags.setCoverageGap(false);
				changed = true;
			}
			// design §8: chainLinkFailure is never self-clearing, only ever set here.
			if (!flags.isChainLinkFailure()
					&& ProducerChainRules.successorLinkBroken(stored.getPrevRecordHash(), inserted.getContentDigest())) {
				flags.setChainLinkFailure(true);
				changed = true;
			}
			return changed ? flags : null;
		}, "update successor", inserted.getIdentity());
	}

	/**
	 * Shared retry-on-version-conflict logic for a single neighbor write.
	 * {@code mutator} recomputes the flags to write from the current {@link StoredTraceRecord} (the
	 * initial read, or a fresh re-read after a lost CAS race), returning {@code null} when nothing
	 * needs to change (so no write is attempted). A {@link TraceStorageException} whose reason is
	 * not {@link TraceStore#VERSION_CONFLICT_REASON}, or any other {@link RuntimeException}, is
	 * logged at ERROR and swallowed (today's best-effort contract, unchanged); exhausting the retry
	 * budget on a genuine version conflict is logged at WARN instead.
	 */
	private void updateFlags(RecordIdentity targetId, Function<StoredTraceRecord, IngestionFlags> mutator,
			String action, RecordIdentity insertedId) {
		try {
			Optional<StoredTraceRecord> current = store.findRecord(targetId);
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
