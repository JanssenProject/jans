/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.model.attestation;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * How strongly the evidence proves that the private half of the attested key lives in the appraised hardware.
 */
public enum KeyBindingStrength {

    /** A hardware key (TPM / vTPM) certified the attested key as non-exportable. */
    CERTIFIED("certified"),
    /** A measured, trusted guest image reports it holds the key (bound to report data, not certified). */
    MEASURED_GUEST("measured_guest"),
    /** No key custody established. */
    NONE("none");

    private final String value;

    KeyBindingStrength(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static KeyBindingStrength fromString(String value) {
        if (value != null) {
            for (KeyBindingStrength strength : values()) {
                if (strength.value.equalsIgnoreCase(value)) {
                    return strength;
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
