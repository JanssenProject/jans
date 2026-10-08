/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.model.conf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.jans.doc.annotation.DocProperty;

/**
 * Assurance policy for a single relying party, overriding the corresponding global
 * {@link Fido2Configuration} values.
 * <p>
 * Every field is nullable, and null means "not set for this RP" rather than a default: the resolver
 * falls back to the global value. An RP carrying no policy at all therefore behaves exactly as it did
 * before per-RP policy existed.
 * <p>
 * Fields are added here alongside the change that enforces them. A field stored but not read would look
 * to an administrator like a policy that is in effect when it is not, which is worse than its absence.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestedPartyPolicy {

    @DocProperty(description = "Attestation mode for this relying party - disabled, monitor or enforced. "
            + "Unset falls back to the global attestationMode.")
    private String attestationMode;

    public String getAttestationMode() {
        return attestationMode;
    }

    public void setAttestationMode(String attestationMode) {
        this.attestationMode = attestationMode;
    }

    @Override
    public String toString() {
        return "RequestedPartyPolicy [attestationMode=" + attestationMode + "]";
    }
}
