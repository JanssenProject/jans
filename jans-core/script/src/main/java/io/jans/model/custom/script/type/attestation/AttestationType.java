/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.custom.script.type.attestation;

import io.jans.model.custom.script.type.BaseExternalType;

/**
 * Script interface of the attestation endpoint ({@code /attestation/challenge}, {@code /attestation}).
 *
 * <p>The script makes policy decisions only: it calls the platform verifier and applies the measurement
 * policy. Binding facts (verifier result signature and issuer, nonce, attested key, key certification) are
 * verified by the server core and cannot be set by the script.</p>
 *
 * <p>The {@code context} parameter is {@code io.jans.as.server.service.external.context.AttestationContext}.</p>
 */
public interface AttestationType extends BaseExternalType {

    /**
     * Optional hook called when a challenge is created.
     *
     * @return {@code false} to refuse the challenge request
     */
    boolean createChallenge(Object attestationContext);

    /**
     * Optional hook to add or remove members of the {@code /attestation/challenge} response.
     *
     * @param responseAsJsonObject org.json.JSONObject
     */
    boolean modifyChallengeResponse(Object responseAsJsonObject, Object attestationContext);

    /**
     * Mandatory: appraise the evidence and fill the script result on the context (policy status,
     * verifier result JWT, platform, measurement, policy version, reasons).
     *
     * @return {@code false} to reject the evidence outright (verdict {@code contraindicated})
     */
    boolean verifyEvidence(Object attestationContext);

    /**
     * Optional hook to add or remove members of the {@code /attestation} response.
     *
     * @param responseAsJsonObject org.json.JSONObject
     */
    boolean modifyResponse(Object responseAsJsonObject, Object attestationContext);
}
