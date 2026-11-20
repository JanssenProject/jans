/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.persistence.model.attestation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Non-indexed fields of an {@link AttestationAssessment}. Times are epoch milliseconds.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class AttestationAssessmentAttributes implements Serializable {

    private static final long serialVersionUID = 4508447095236012655L;

    @JsonProperty
    private String platform;
    @JsonProperty
    private String measurement;
    @JsonProperty
    private String policyVersion;
    @JsonProperty
    private String verifier;
    @JsonProperty
    private String verifierKind;
    @JsonProperty
    private String verifierResultJwt;
    @JsonProperty
    private String verifierResultDigest;
    @JsonProperty
    private String evidenceFormat;
    @JsonProperty
    private String evidenceDigest;
    @JsonProperty
    private String evidence;
    @JsonProperty
    private Boolean keyCertified;
    @JsonProperty
    private String keyBindingStrength;
    @JsonProperty
    private Boolean keyBindingStrengthProvisional;
    @JsonProperty
    private String profileMaturity;
    @JsonProperty
    private String challengeId;
    @JsonProperty
    private Long leaseEpoch;
    @JsonProperty
    private Long appraisalGeneration;
    @JsonProperty
    private Long expiresAt;
    @JsonProperty
    private Boolean supersedesEligible;
    @JsonProperty
    private String purpose;
    @JsonProperty
    private String traceOperationId;
    @JsonProperty
    private String traceRecordId;
    @JsonProperty
    private List<String> reasons;
    @JsonProperty
    private Map<String, Object> claims;

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getMeasurement() {
        return measurement;
    }

    public void setMeasurement(String measurement) {
        this.measurement = measurement;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getVerifier() {
        return verifier;
    }

    public void setVerifier(String verifier) {
        this.verifier = verifier;
    }

    public String getVerifierKind() {
        return verifierKind;
    }

    public void setVerifierKind(String verifierKind) {
        this.verifierKind = verifierKind;
    }

    public String getVerifierResultJwt() {
        return verifierResultJwt;
    }

    public void setVerifierResultJwt(String verifierResultJwt) {
        this.verifierResultJwt = verifierResultJwt;
    }

    public String getVerifierResultDigest() {
        return verifierResultDigest;
    }

    public void setVerifierResultDigest(String verifierResultDigest) {
        this.verifierResultDigest = verifierResultDigest;
    }

    public String getEvidenceFormat() {
        return evidenceFormat;
    }

    public void setEvidenceFormat(String evidenceFormat) {
        this.evidenceFormat = evidenceFormat;
    }

    public String getEvidenceDigest() {
        return evidenceDigest;
    }

    public void setEvidenceDigest(String evidenceDigest) {
        this.evidenceDigest = evidenceDigest;
    }

    public String getEvidence() {
        return evidence;
    }

    public void setEvidence(String evidence) {
        this.evidence = evidence;
    }

    public Boolean getKeyCertified() {
        return keyCertified;
    }

    public void setKeyCertified(Boolean keyCertified) {
        this.keyCertified = keyCertified;
    }

    public String getKeyBindingStrength() {
        return keyBindingStrength;
    }

    public void setKeyBindingStrength(String keyBindingStrength) {
        this.keyBindingStrength = keyBindingStrength;
    }

    public Boolean getKeyBindingStrengthProvisional() {
        return keyBindingStrengthProvisional;
    }

    public void setKeyBindingStrengthProvisional(Boolean keyBindingStrengthProvisional) {
        this.keyBindingStrengthProvisional = keyBindingStrengthProvisional;
    }

    public String getProfileMaturity() {
        return profileMaturity;
    }

    public void setProfileMaturity(String profileMaturity) {
        this.profileMaturity = profileMaturity;
    }

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public Long getLeaseEpoch() {
        return leaseEpoch;
    }

    public void setLeaseEpoch(Long leaseEpoch) {
        this.leaseEpoch = leaseEpoch;
    }

    public Long getAppraisalGeneration() {
        return appraisalGeneration;
    }

    public void setAppraisalGeneration(Long appraisalGeneration) {
        this.appraisalGeneration = appraisalGeneration;
    }

    public Long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isSupersedesEligible() {
        return Boolean.TRUE.equals(supersedesEligible);
    }

    public void setSupersedesEligible(Boolean supersedesEligible) {
        this.supersedesEligible = supersedesEligible;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getTraceOperationId() {
        return traceOperationId;
    }

    public void setTraceOperationId(String traceOperationId) {
        this.traceOperationId = traceOperationId;
    }

    public String getTraceRecordId() {
        return traceRecordId;
    }

    public void setTraceRecordId(String traceRecordId) {
        this.traceRecordId = traceRecordId;
    }

    public List<String> getReasons() {
        if (reasons == null) reasons = new ArrayList<>();
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public Map<String, Object> getClaims() {
        if (claims == null) claims = new LinkedHashMap<>();
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    @Override
    public String toString() {
        return "AttestationAssessmentAttributes{platform='" + platform + "', measurement='" + measurement
                + "', policyVersion='" + policyVersion + "', verifier='" + verifier + "', evidenceFormat='" + evidenceFormat
                + "', keyBindingStrength='" + keyBindingStrength + "', challengeId='" + challengeId + "', leaseEpoch=" + leaseEpoch
                + ", appraisalGeneration=" + appraisalGeneration + ", expiresAt=" + expiresAt
                + ", supersedesEligible=" + supersedesEligible + ", reasons=" + reasons + "}";
    }
}
