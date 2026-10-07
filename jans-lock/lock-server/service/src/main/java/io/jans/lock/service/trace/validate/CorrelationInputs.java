/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.service.trace.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.jans.lock.service.trace.model.TokenRef;
import io.jans.lock.service.trace.parse.ParsedAssertion;

/**
 * The values the correlation service (task 17) indexes for one accepted record: execution
 * identity, capability ids, token references, chain position and parents. Built by
 * {@link EventKindValidatorRegistry#validate(ParsedAssertion)}: the common fields (design §7.1)
 * are filled in by the registry, the event-kind-specific ones ({@link #getCapabilityIds()},
 * {@link #getTokenRefs()}, {@link #getWarnings()}) by the matching {@link EventKindValidator}.
 *
 * @author Yuriy Movchan
 */
public final class CorrelationInputs {

	private final String executionAuthority;

	private final String traceExecutionId;

	private final List<String> capabilityIds;

	private final List<TokenRef> tokenRefs;

	private final ParsedAssertion.ProducerChain chainPosition;

	private final List<ParsedAssertion.ParentRecordId> parents;

	private final String eventKind;

	private final long signedAt;

	private final List<String> warnings;

	private CorrelationInputs(Builder builder) {
		this.executionAuthority = builder.executionAuthority;
		this.traceExecutionId = builder.traceExecutionId;
		this.capabilityIds = Collections.unmodifiableList(new ArrayList<>(builder.capabilityIds));
		this.tokenRefs = Collections.unmodifiableList(new ArrayList<>(builder.tokenRefs));
		this.chainPosition = builder.chainPosition;
		this.parents = Collections.unmodifiableList(new ArrayList<>(builder.parents));
		this.eventKind = builder.eventKind;
		this.signedAt = builder.signedAt;
		this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
	}

	public String getExecutionAuthority() {
		return executionAuthority;
	}

	public String getTraceExecutionId() {
		return traceExecutionId;
	}

	/**
	 * @return the capability ids this record carries, deduplicated with order preserved; empty for
	 *         a {@code RUNTIME_EFFECT} record
	 */
	public List<String> getCapabilityIds() {
		return capabilityIds;
	}

	public List<TokenRef> getTokenRefs() {
		return tokenRefs;
	}

	public ParsedAssertion.ProducerChain getChainPosition() {
		return chainPosition;
	}

	public List<ParsedAssertion.ParentRecordId> getParents() {
		return parents;
	}

	public String getEventKind() {
		return eventKind;
	}

	public long getSignedAt() {
		return signedAt;
	}

	/**
	 * @return non-rejecting findings surfaced during validation (e.g.
	 *         {@code runtime_effect_without_produced_effect_parent}); never {@code null}
	 */
	public List<String> getWarnings() {
		return warnings;
	}

	public static final class Builder {

		private String executionAuthority;

		private String traceExecutionId;

		private List<String> capabilityIds = Collections.emptyList();

		private List<TokenRef> tokenRefs = Collections.emptyList();

		private ParsedAssertion.ProducerChain chainPosition;

		private List<ParsedAssertion.ParentRecordId> parents = Collections.emptyList();

		private String eventKind;

		private long signedAt;

		private final List<String> warnings = new ArrayList<>();

		public Builder executionAuthority(String executionAuthority) {
			this.executionAuthority = executionAuthority;
			return this;
		}

		public Builder traceExecutionId(String traceExecutionId) {
			this.traceExecutionId = traceExecutionId;
			return this;
		}

		public Builder capabilityIds(List<String> capabilityIds) {
			this.capabilityIds = capabilityIds == null ? Collections.emptyList() : capabilityIds;
			return this;
		}

		public Builder tokenRefs(List<TokenRef> tokenRefs) {
			this.tokenRefs = tokenRefs == null ? Collections.emptyList() : tokenRefs;
			return this;
		}

		public Builder chainPosition(ParsedAssertion.ProducerChain chainPosition) {
			this.chainPosition = chainPosition;
			return this;
		}

		public Builder parents(List<ParsedAssertion.ParentRecordId> parents) {
			this.parents = parents == null ? Collections.emptyList() : parents;
			return this;
		}

		public Builder eventKind(String eventKind) {
			this.eventKind = eventKind;
			return this;
		}

		public Builder signedAt(long signedAt) {
			this.signedAt = signedAt;
			return this;
		}

		public Builder addWarning(String warning) {
			this.warnings.add(warning);
			return this;
		}

		public CorrelationInputs build() {
			return new CorrelationInputs(this);
		}

	}

}
