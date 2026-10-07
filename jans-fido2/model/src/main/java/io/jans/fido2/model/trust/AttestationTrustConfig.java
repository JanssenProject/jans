/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.trust;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Read-only view of the attestation policy the server is actually enforcing. Lets an administrator see
 * whether a strict mode is the reason certain authenticators are being rejected, instead of that
 * surfacing to end users as a generic registration failure.
 *
 * @author Janssen Project
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttestationTrustConfig {

    private String attestationMode;

    private boolean attestationModeRecognized;

    private boolean unattestedAuthenticatorsAllowed;

    private boolean enterpriseAttestation;

    private boolean metadataServiceDisabled;

    private boolean appleRootCaPresent;

    private List<String> enabledFidoAlgorithms = new ArrayList<>();

    private List<String> hints = new ArrayList<>();

    /**
     * The configured attestation mode, verbatim. Reported as configured rather than normalized so an
     * administrator can see a typo for what it is.
     */
    public String getAttestationMode() {
        return attestationMode;
    }

    public void setAttestationMode(String attestationMode) {
        this.attestationMode = attestationMode;
    }

    /**
     * Whether the configured value matches one of the supported modes (disabled / monitor / enforced).
     * When false the server falls back to lenient behaviour, which is otherwise invisible.
     */
    public boolean isAttestationModeRecognized() {
        return attestationModeRecognized;
    }

    public void setAttestationModeRecognized(boolean attestationModeRecognized) {
        this.attestationModeRecognized = attestationModeRecognized;
    }

    /**
     * Whether an authenticator that fails attestation validation is still accepted. True for every mode
     * except {@code enforced} — only that mode applies the stricter MDS trust rules.
     */
    public boolean isUnattestedAuthenticatorsAllowed() {
        return unattestedAuthenticatorsAllowed;
    }

    public void setUnattestedAuthenticatorsAllowed(boolean unattestedAuthenticatorsAllowed) {
        this.unattestedAuthenticatorsAllowed = unattestedAuthenticatorsAllowed;
    }

    public boolean isEnterpriseAttestation() {
        return enterpriseAttestation;
    }

    public void setEnterpriseAttestation(boolean enterpriseAttestation) {
        this.enterpriseAttestation = enterpriseAttestation;
    }

    /**
     * When true, MDS download and validation are switched off and attestation cannot be validated
     * against FIDO metadata.
     */
    public boolean isMetadataServiceDisabled() {
        return metadataServiceDisabled;
    }

    public void setMetadataServiceDisabled(boolean metadataServiceDisabled) {
        this.metadataServiceDisabled = metadataServiceDisabled;
    }

    /**
     * Whether the Apple WebAuthn root CA was loaded at startup. When false, Apple anonymous attestation
     * cannot be validated.
     */
    public boolean isAppleRootCaPresent() {
        return appleRootCaPresent;
    }

    public void setAppleRootCaPresent(boolean appleRootCaPresent) {
        this.appleRootCaPresent = appleRootCaPresent;
    }

    public List<String> getEnabledFidoAlgorithms() {
        return enabledFidoAlgorithms;
    }

    public void setEnabledFidoAlgorithms(List<String> enabledFidoAlgorithms) {
        this.enabledFidoAlgorithms = enabledFidoAlgorithms;
    }

    public List<String> getHints() {
        return hints;
    }

    public void setHints(List<String> hints) {
        this.hints = hints;
    }

    @Override
    public String toString() {
        return "AttestationTrustConfig [attestationMode=" + attestationMode + ", attestationModeRecognized="
                + attestationModeRecognized + ", unattestedAuthenticatorsAllowed=" + unattestedAuthenticatorsAllowed
                + ", enterpriseAttestation=" + enterpriseAttestation + ", metadataServiceDisabled="
                + metadataServiceDisabled + ", appleRootCaPresent=" + appleRootCaPresent + ", enabledFidoAlgorithms="
                + enabledFidoAlgorithms + ", hints=" + hints + "]";
    }
}
