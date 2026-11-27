/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

/**
 * Purpose of an attestation challenge: enrollment of a key, or issuance for an enrolled key.
 */
public enum AttestationPurpose {

    ENROLL("enroll"),
    ISSUE("issue");

    private final String value;

    AttestationPurpose(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static AttestationPurpose fromString(String value) {
        if (value != null) {
            for (AttestationPurpose purpose : values()) {
                if (purpose.value.equals(value)) {
                    return purpose;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return value;
    }
}
