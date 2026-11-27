/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

/**
 * State of a persisted attestation challenge.
 */
public enum ChallengeState {

    PENDING("pending"),
    PROCESSING("processing"),
    COMPLETED("completed"),
    EXPIRED("expired");

    private final String value;

    ChallengeState(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ChallengeState fromString(String value) {
        if (value != null) {
            for (ChallengeState state : values()) {
                if (state.value.equals(value)) {
                    return state;
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
