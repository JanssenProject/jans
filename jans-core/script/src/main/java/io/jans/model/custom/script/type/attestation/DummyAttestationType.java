/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.model.custom.script.type.attestation;

import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;

/**
 * Default attestation script: accepts challenges, rejects every evidence.
 */
public class DummyAttestationType implements AttestationType {

    @Override
    public boolean createChallenge(Object attestationContext) {
        return true;
    }

    @Override
    public boolean modifyChallengeResponse(Object responseAsJsonObject, Object attestationContext) {
        return true;
    }

    @Override
    public boolean verifyEvidence(Object attestationContext) {
        return false;
    }

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object attestationContext) {
        return true;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        return true;
    }

    @Override
    public int getApiVersion() {
        return 1;
    }
}
