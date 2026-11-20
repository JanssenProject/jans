/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.persistence.model.attestation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Non-indexed fields of a {@link KeyAttestationState}. Times are epoch milliseconds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KeyAttestationStateAttributes implements Serializable {

    private static final long serialVersionUID = -4880187370713441361L;

    @JsonProperty
    private Long appraisalGeneration;
    @JsonProperty
    private Long appraisedAt;
    @JsonProperty
    private Long expiresAt;
    @JsonProperty
    private String assessmentId;
    @JsonProperty
    private Boolean consumed;
    @JsonProperty
    private String consumedByJti;
    @JsonProperty
    private List<String> reasons;

    public long getAppraisalGeneration() {
        return appraisalGeneration == null ? 0L : appraisalGeneration;
    }

    public void setAppraisalGeneration(Long appraisalGeneration) {
        this.appraisalGeneration = appraisalGeneration;
    }

    public Long getAppraisedAt() {
        return appraisedAt;
    }

    public void setAppraisedAt(Long appraisedAt) {
        this.appraisedAt = appraisedAt;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getAssessmentId() {
        return assessmentId;
    }

    public void setAssessmentId(String assessmentId) {
        this.assessmentId = assessmentId;
    }

    public boolean isConsumed() {
        return Boolean.TRUE.equals(consumed);
    }

    public void setConsumed(Boolean consumed) {
        this.consumed = consumed;
    }

    public String getConsumedByJti() {
        return consumedByJti;
    }

    public void setConsumedByJti(String consumedByJti) {
        this.consumedByJti = consumedByJti;
    }

    public List<String> getReasons() {
        if (reasons == null) reasons = new ArrayList<>();
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    @Override
    public String toString() {
        return "KeyAttestationStateAttributes{appraisalGeneration=" + appraisalGeneration + ", appraisedAt=" + appraisedAt
                + ", expiresAt=" + expiresAt + ", assessmentId='" + assessmentId + "', consumed=" + consumed
                + ", consumedByJti='" + consumedByJti + "', reasons=" + reasons + "}";
    }
}
