/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.persistence.model.attestation;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Non-indexed fields of an {@link AttestationChallenge}. Times are epoch milliseconds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttestationChallengeAttributes implements Serializable {

    private static final long serialVersionUID = -3121785441125410318L;

    @JsonProperty
    private String nonce;
    @JsonProperty
    private String purpose;
    @JsonProperty
    private Long expiresAt;
    @JsonProperty
    private Long appraisalGeneration;
    @JsonProperty
    private Integer retryCount;
    @JsonProperty
    private String leaseHolder;
    @JsonProperty
    private Long leaseEpoch;
    @JsonProperty
    private Long leaseExpiresAt;
    @JsonProperty
    private String operationId;
    @JsonProperty
    private String completedAssessmentId;

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Long getAppraisalGeneration() {
        return appraisalGeneration;
    }

    public void setAppraisalGeneration(Long appraisalGeneration) {
        this.appraisalGeneration = appraisalGeneration;
    }

    public int getRetryCount() {
        return retryCount == null ? 0 : retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getLeaseHolder() {
        return leaseHolder;
    }

    public void setLeaseHolder(String leaseHolder) {
        this.leaseHolder = leaseHolder;
    }

    public long getLeaseEpoch() {
        return leaseEpoch == null ? 0L : leaseEpoch;
    }

    public void setLeaseEpoch(Long leaseEpoch) {
        this.leaseEpoch = leaseEpoch;
    }

    public Long getLeaseExpiresAt() {
        return leaseExpiresAt;
    }

    public void setLeaseExpiresAt(Long leaseExpiresAt) {
        this.leaseExpiresAt = leaseExpiresAt;
    }

    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getCompletedAssessmentId() {
        return completedAssessmentId;
    }

    public void setCompletedAssessmentId(String completedAssessmentId) {
        this.completedAssessmentId = completedAssessmentId;
    }

    @Override
    public String toString() {
        return "AttestationChallengeAttributes{purpose='" + purpose + "', expiresAt=" + expiresAt
                + ", appraisalGeneration=" + appraisalGeneration + ", retryCount=" + retryCount
                + ", leaseHolder='" + leaseHolder + "', leaseEpoch=" + leaseEpoch + ", leaseExpiresAt=" + leaseExpiresAt
                + ", operationId='" + operationId + "', completedAssessmentId='" + completedAssessmentId + "'}";
    }
}
