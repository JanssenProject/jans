/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.lock.model.trace.config;

/**
 * How a producer chain comes to exist (design §8, TRACE MVP design decision D-23).
 *
 * @author Yuriy Movchan
 */
public enum ChainRegistrationMode {

    /** A chain must be registered through the admin API before its records are accepted (D-5). */
    PRE_REGISTERED,

    /**
     * A genesis record ({@code sequence_number == 1} with the zero-hash sentinel) of an unregistered
     * chain registers that chain; Lock only anchors the chain from the moment it sees it.
     */
    FIRST_OBSERVED;

    /**
     * @return the mode named by {@code value} (trimmed, case-insensitive), or {@code null} when
     *         {@code value} is not the name of a mode
     */
    public static ChainRegistrationMode fromString(String value) {
        if (value == null) {
            return null;
        }
        String name = value.trim();
        for (ChainRegistrationMode mode : values()) {
            if (mode.name().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        return null;
    }

}
