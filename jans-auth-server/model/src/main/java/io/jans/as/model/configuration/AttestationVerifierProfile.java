/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.configuration;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Server-side trust anchor and claim-path map of one attestation verifier (one instance of the
 * vendor-neutral binding contract). The attestation script names a profile by {@code kind}; the server
 * core verifies the verifier result and extracts binding facts only through this profile, never through
 * script-supplied keys or paths.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttestationVerifierProfile {

    public static final String MATURITY_EXPERIMENTAL = "experimental";
    public static final String MATURITY_VALIDATED = "validated";

    public static final String AK_BINDING_CERT_CHAIN = "ak_cert_chain";
    public static final String AK_BINDING_RUNTIME_DATA_COMMITMENT = "runtime_data_commitment";

    private String kind;
    private String jwksUri;
    private List<String> issuers;
    private List<String> verifierEndpoints;
    private List<String> evidenceFormats;
    private List<String> allowedAlgs;
    private Boolean evidenceIsResult;
    private String nonceClaimPath;
    private String keyClaimPath;
    private String measurementClaimPath;
    private String debugClaimPath;
    private String platformClaimPath;
    private String reportDataClaimPath;
    private String akClaimPath;
    private List<String> akBindingMethods;
    private List<String> akCertTrustAnchors;
    private Boolean akBindingProven;
    private String akBindingNote;
    private String trustAnchor;
    private String platform;
    private String keyBindingStrength;
    private Boolean keyBindingStrengthProvisional;
    private String maturity;

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public List<String> getIssuers() {
        if (issuers == null) issuers = new ArrayList<>();
        return issuers;
    }

    public void setIssuers(List<String> issuers) {
        this.issuers = issuers;
    }

    public List<String> getVerifierEndpoints() {
        if (verifierEndpoints == null) verifierEndpoints = new ArrayList<>();
        return verifierEndpoints;
    }

    public void setVerifierEndpoints(List<String> verifierEndpoints) {
        this.verifierEndpoints = verifierEndpoints;
    }

    public List<String> getEvidenceFormats() {
        if (evidenceFormats == null) evidenceFormats = new ArrayList<>();
        return evidenceFormats;
    }

    public void setEvidenceFormats(List<String> evidenceFormats) {
        this.evidenceFormats = evidenceFormats;
    }

    public List<String> getAllowedAlgs() {
        if (allowedAlgs == null) allowedAlgs = new ArrayList<>();
        return allowedAlgs;
    }

    public void setAllowedAlgs(List<String> allowedAlgs) {
        this.allowedAlgs = allowedAlgs;
    }

    public Boolean getEvidenceIsResult() {
        return evidenceIsResult;
    }

    public void setEvidenceIsResult(Boolean evidenceIsResult) {
        this.evidenceIsResult = evidenceIsResult;
    }

    public String getNonceClaimPath() {
        return nonceClaimPath;
    }

    public void setNonceClaimPath(String nonceClaimPath) {
        this.nonceClaimPath = nonceClaimPath;
    }

    public String getKeyClaimPath() {
        return keyClaimPath;
    }

    public void setKeyClaimPath(String keyClaimPath) {
        this.keyClaimPath = keyClaimPath;
    }

    public String getMeasurementClaimPath() {
        return measurementClaimPath;
    }

    public void setMeasurementClaimPath(String measurementClaimPath) {
        this.measurementClaimPath = measurementClaimPath;
    }

    public String getDebugClaimPath() {
        return debugClaimPath;
    }

    public void setDebugClaimPath(String debugClaimPath) {
        this.debugClaimPath = debugClaimPath;
    }

    public String getPlatformClaimPath() {
        return platformClaimPath;
    }

    public void setPlatformClaimPath(String platformClaimPath) {
        this.platformClaimPath = platformClaimPath;
    }

    public String getReportDataClaimPath() {
        return reportDataClaimPath;
    }

    public void setReportDataClaimPath(String reportDataClaimPath) {
        this.reportDataClaimPath = reportDataClaimPath;
    }

    public String getAkClaimPath() {
        return akClaimPath;
    }

    public void setAkClaimPath(String akClaimPath) {
        this.akClaimPath = akClaimPath;
    }

    public List<String> getAkBindingMethods() {
        if (akBindingMethods == null) akBindingMethods = new ArrayList<>();
        return akBindingMethods;
    }

    public void setAkBindingMethods(List<String> akBindingMethods) {
        this.akBindingMethods = akBindingMethods;
    }

    public List<String> getAkCertTrustAnchors() {
        if (akCertTrustAnchors == null) akCertTrustAnchors = new ArrayList<>();
        return akCertTrustAnchors;
    }

    public void setAkCertTrustAnchors(List<String> akCertTrustAnchors) {
        this.akCertTrustAnchors = akCertTrustAnchors;
    }

    public Boolean getAkBindingProven() {
        return akBindingProven;
    }

    public void setAkBindingProven(Boolean akBindingProven) {
        this.akBindingProven = akBindingProven;
    }

    public String getAkBindingNote() {
        return akBindingNote;
    }

    public void setAkBindingNote(String akBindingNote) {
        this.akBindingNote = akBindingNote;
    }

    public String getTrustAnchor() {
        return trustAnchor;
    }

    public void setTrustAnchor(String trustAnchor) {
        this.trustAnchor = trustAnchor;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
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

    public String getMaturity() {
        if (maturity == null) maturity = MATURITY_EXPERIMENTAL;
        return maturity;
    }

    public void setMaturity(String maturity) {
        this.maturity = maturity;
    }

    public boolean isExperimental() {
        return !MATURITY_VALIDATED.equalsIgnoreCase(getMaturity());
    }

    public boolean hasAkBindingMethod(String method) {
        return getAkBindingMethods().contains(method);
    }

    @Override
    public String toString() {
        return "AttestationVerifierProfile{kind='" + kind + "', platform='" + platform + "', maturity='" + getMaturity() + "'}";
    }
}
