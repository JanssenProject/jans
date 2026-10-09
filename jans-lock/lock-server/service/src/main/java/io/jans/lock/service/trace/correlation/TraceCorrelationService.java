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
import io.jans.lock.service.trace.identity.TraceRequestContext;
import io.jans.lock.service.trace.model.ChainIdentity;
import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;
import io.jans.lock.service.trace.model.TokenRef;
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
 * lateness, §13; design decisions D-5, D-6, D-9, D-11).
 *
 * <p>{@link #plan} never touches the store for writes; {@link #afterInsert} is the only method
 * that mutates other rows, and it does so best-effort and idempotently (design decision D-9): a
 * failure updating one neighbor is logged (identities only) and never fails the request, and
 * {@code afterInsert} may race with another node's {@code afterInsert} for the same neighbor —
 * last writer wins, which is acceptable because the flags are recomputable from the stored rows
 * at any time.
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

	/**
	 * @throws io.jans.lock.service.trace.error.TraceValidationException {@code chain_not_registered}
	 *         (design decision D-5) or {@code invalid_genesis} (design decision D-5)
	 */
	public CorrelationPlan plan(VerifiedAssertion verified, TraceRequestContext ctx) {
		CorrelationInputs inputs = verified.getInputs();
		ParsedAssertion.ProducerChain producerChain = inputs.getChainPosition();
		String domainId = ctx.getEvidenceDomainId();

		ChainIdentity chainIdentity = new ChainIdentity(domainId, producerChain.getProducerId(),
				producerChain.getProducerInstanceId(), producerChain.getProducerChainId());
		long sequenceNumber = producerChain.getSequenceNumber();
		ChainPosition position = new ChainPosition(chainIdentity, sequenceNumber);

		chainRegistry.requireRegistered(chainIdentity);
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

		IngestionFlags flags = new IngestionFlags(coverageGap, chainLinkFailure, equivocation, late);

		ExecutionIdentity execution = new ExecutionIdentity(domainId, inputs.getExecutionAuthority(),
				inputs.getTraceExecutionId());

		List<String> capabilityKeys = dedupKeys(inputs.getCapabilityIds(),
				capabilityId -> TraceKeys.capabilityKey(domainId, capabilityId));
		List<String> tokenKeys = dedupKeys(inputs.getTokenRefs(), token -> TraceKeys.tokenKey(domainId, token));

		return new CorrelationPlan(flags, position, execution, capabilityKeys, tokenKeys, equivocationPeers,
				successors, inputs.getWarnings());
	}

	/**
	 * Updates the equivocation peers and the successor at {@code sequence_number + 1} after
	 * {@code inserted} became visible in the store (design decision D-9). Best-effort: a failure
	 * updating one neighbor is logged and does not propagate.
	 */
	public void afterInsert(StoredTraceRecord inserted, CorrelationPlan plan) {
		for (RecordIdentity peerId : plan.getEquivocationPeers()) {
			updatePeer(inserted.getIdentity(), peerId);
		}
		for (StoredTraceRecord successor : plan.getSuccessors()) {
			updateSuccessor(inserted, successor.getIdentity());
		}
	}

	private void updatePeer(RecordIdentity insertedId, RecordIdentity peerId) {
		try {
			Optional<StoredTraceRecord> current = store.findRecord(peerId);
			if (!current.isPresent()) {
				return;
			}
			IngestionFlags flags = current.get().getFlags();
			if (!flags.isEquivocation()) {
				flags.setEquivocation(true);
				store.updateRecordFlags(peerId, flags);
			}
		} catch (RuntimeException ex) {
			log.error("TRACE correlation afterInsert failed to flag equivocation peer: inserted={}, peer={}",
					insertedId, peerId, ex);
		}
	}

	private void updateSuccessor(StoredTraceRecord inserted, RecordIdentity successorId) {
		try {
			Optional<StoredTraceRecord> current = store.findRecord(successorId);
			if (!current.isPresent()) {
				return;
			}
			StoredTraceRecord successor = current.get();
			IngestionFlags flags = successor.getFlags();
			boolean changed = false;
			if (flags.isCoverageGap()) {
				flags.setCoverageGap(false);
				changed = true;
			}
			// design §8: chainLinkFailure is never self-clearing, only ever set here.
			if (!flags.isChainLinkFailure()
					&& ProducerChainRules.successorLinkBroken(successor.getPrevRecordHash(), inserted.getContentDigest())) {
				flags.setChainLinkFailure(true);
				changed = true;
			}
			if (changed) {
				store.updateRecordFlags(successorId, flags);
			}
		} catch (RuntimeException ex) {
			log.error("TRACE correlation afterInsert failed to update successor: inserted={}, successor={}",
					inserted.getIdentity(), successorId, ex);
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
