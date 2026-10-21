/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.api;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The Lock-derived ingestion flags shared by the acceptance response and the {@code ingestion}
 * member of a record envelope.
 */
public abstract class TraceFlagsResponse {

	@JsonProperty("coverage_gap_flag")
	@Schema(description = "True when this record's predecessor in its producer chain is missing")
	private boolean coverageGapFlag;

	@JsonProperty("chain_link_failure_flag")
	@Schema(description = "True when this record's prev_record_hash does not match its predecessor")
	private boolean chainLinkFailureFlag;

	@JsonProperty("equivocation_flag")
	@Schema(description = "True when another record already occupies this producer-chain position")
	private boolean equivocationFlag;

	@JsonProperty("late_flag")
	@Schema(description = "True when the record arrived after the configured lateness threshold")
	private boolean lateFlag;

	@JsonProperty("policy_mismatch_flag")
	@Schema(description = "True when trace.policy names a policy store Lock serves but its bundle_hash or policy_store_version differs from the served release")
	private boolean policyMismatchFlag;

	@JsonProperty("reserved_member_flag")
	@Schema(description = "True when Lock-derived members the producer placed in its signed body (for example evidence_domain_id or trust_tier) were ignored")
	private boolean reservedMemberFlag;

	@JsonProperty("reserved_members")
	@Schema(description = "Paths of the ignored Lock-derived members, for example trace.trust_tier; their values inside assertion are producer-supplied and unused")
	private List<String> reservedMembers = new ArrayList<>();

	public boolean isCoverageGapFlag() {
		return coverageGapFlag;
	}

	public void setCoverageGapFlag(boolean coverageGapFlag) {
		this.coverageGapFlag = coverageGapFlag;
	}

	public boolean isChainLinkFailureFlag() {
		return chainLinkFailureFlag;
	}

	public void setChainLinkFailureFlag(boolean chainLinkFailureFlag) {
		this.chainLinkFailureFlag = chainLinkFailureFlag;
	}

	public boolean isEquivocationFlag() {
		return equivocationFlag;
	}

	public void setEquivocationFlag(boolean equivocationFlag) {
		this.equivocationFlag = equivocationFlag;
	}

	public boolean isLateFlag() {
		return lateFlag;
	}

	public void setLateFlag(boolean lateFlag) {
		this.lateFlag = lateFlag;
	}

	public boolean isPolicyMismatchFlag() {
		return policyMismatchFlag;
	}

	public void setPolicyMismatchFlag(boolean policyMismatchFlag) {
		this.policyMismatchFlag = policyMismatchFlag;
	}

	public boolean isReservedMemberFlag() {
		return reservedMemberFlag;
	}

	public void setReservedMemberFlag(boolean reservedMemberFlag) {
		this.reservedMemberFlag = reservedMemberFlag;
	}

	public List<String> getReservedMembers() {
		return reservedMembers;
	}

	public void setReservedMembers(List<String> reservedMembers) {
		this.reservedMembers = reservedMembers == null ? new ArrayList<>() : new ArrayList<>(reservedMembers);
	}

}
