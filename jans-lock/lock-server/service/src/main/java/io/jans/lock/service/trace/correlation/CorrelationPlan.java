/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.correlation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.jans.lock.service.trace.model.ChainPosition;
import io.jans.lock.service.trace.model.ExecutionIdentity;
import io.jans.lock.service.trace.model.IngestionFlags;
import io.jans.lock.service.trace.model.RecordIdentity;
import io.jans.lock.service.trace.model.StoredTraceRecord;

/**
 * The output of {@link TraceCorrelationService#plan}: everything the ingestion pipeline (task 19)
 * needs to build the {@code StoredTraceRecord} it is about to insert, plus the neighbor rows
 * {@link TraceCorrelationService#afterInsert} must update once the insert succeeds (design §8,
 * §9, design decisions D-5, D-6, D-9, D-11).
 *
 * @author Yuriy Movchan
 */
public final class CorrelationPlan {

	private final IngestionFlags flags;

	private final ChainPosition position;

	private final ExecutionIdentity execution;

	private final List<String> capabilityKeys;

	private final List<String> tokenKeys;

	private final List<RecordIdentity> equivocationPeers;

	private final List<StoredTraceRecord> successors;

	private final List<String> warnings;

	public CorrelationPlan(IngestionFlags flags, ChainPosition position, ExecutionIdentity execution,
			List<String> capabilityKeys, List<String> tokenKeys, List<RecordIdentity> equivocationPeers,
			List<StoredTraceRecord> successors, List<String> warnings) {
		this.flags = new IngestionFlags(Objects.requireNonNull(flags, "flags"));
		this.position = Objects.requireNonNull(position, "position");
		this.execution = Objects.requireNonNull(execution, "execution");
		this.capabilityKeys = unmodifiable(capabilityKeys);
		this.tokenKeys = unmodifiable(tokenKeys);
		this.equivocationPeers = unmodifiable(equivocationPeers);
		this.successors = unmodifiable(successors);
		this.warnings = unmodifiable(warnings);
	}

	/**
	 * @return an independent copy of the computed ingestion flags
	 */
	public IngestionFlags getFlags() {
		return flags.copy();
	}

	public ChainPosition getPosition() {
		return position;
	}

	public ExecutionIdentity getExecution() {
		return execution;
	}

	/**
	 * @return the {@code key("cap", …)} values for this record's capability ids, deduplicated with
	 *         order preserved; never {@code null}
	 */
	public List<String> getCapabilityKeys() {
		return capabilityKeys;
	}

	/**
	 * @return the {@code key("tok", …)} values for this record's token references, deduplicated
	 *         with order preserved; never {@code null}
	 */
	public List<String> getTokenKeys() {
		return tokenKeys;
	}

	/**
	 * @return the identities of the other records already occupying this record's chain position,
	 *         empty unless this is an equivocation
	 */
	public List<RecordIdentity> getEquivocationPeers() {
		return equivocationPeers;
	}

	/**
	 * @return the rows already stored at {@code sequence_number + 1}, to be revisited by
	 *         {@link TraceCorrelationService#afterInsert}
	 */
	public List<StoredTraceRecord> getSuccessors() {
		return successors;
	}

	/**
	 * @return non-rejecting findings carried over from {@link io.jans.lock.service.trace.validate.CorrelationInputs}
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	private static <T> List<T> unmodifiable(List<T> source) {
		return source == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(source));
	}

	@Override
	public String toString() {
		return "CorrelationPlan [flags=" + flags + ", position=" + position + ", execution=" + execution
				+ ", capabilityKeys=" + capabilityKeys + ", tokenKeys=" + tokenKeys + ", equivocationPeers="
				+ equivocationPeers + ", successors=" + successors.size() + ", warnings=" + warnings + "]";
	}

}
