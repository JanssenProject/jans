/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * RATS EAR appraisal status (draft-ietf-rats-ar4si). {@code none} means no appraisal happened.
 */
public enum AttestationStatus {

    AFFIRMING("affirming", 3),
    WARNING("warning", 2),
    CONTRAINDICATED("contraindicated", 1),
    NONE("none", 0);

    private final String value;
    private final int rank;

    AttestationStatus(String value, int rank) {
        this.value = value;
        this.rank = rank;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static AttestationStatus fromString(String value) {
        if (value != null) {
            for (AttestationStatus status : values()) {
                if (status.value.equalsIgnoreCase(value)) {
                    return status;
                }
            }
        }
        return null;
    }

    /**
     * Combines two verdicts so the result is never better than either input. {@code none} (no appraisal)
     * dominates: when either side produced no verdict, the combination has none either.
     */
    public static AttestationStatus min(AttestationStatus a, AttestationStatus b) {
        if (a == null || b == null || a == NONE || b == NONE) {
            return NONE;
        }
        return a.rank <= b.rank ? a : b;
    }

    /**
     * Whether this status is a verdict about the hardware (not an outage).
     */
    public boolean isVerdict() {
        return this != NONE;
    }

    @Override
    public String toString() {
        return value;
    }
}
