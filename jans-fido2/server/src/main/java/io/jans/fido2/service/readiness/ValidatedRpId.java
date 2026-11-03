/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.readiness;

/**
 * A relying party ID that {@link RpIdValidator} accepted: a normalized, lower-case, ASCII DNS hostname.
 * <p>
 * The constructor is package-private on purpose. Code that fetches from an RP ID takes this type instead of
 * a {@code String}, so a value that was never validated cannot reach it.
 */
public final class ValidatedRpId {

    private final String hostname;

    ValidatedRpId(String hostname) {
        this.hostname = hostname;
    }

    /** The hostname, lower-case ASCII (punycode for an internationalized name), without a trailing dot. */
    public String hostname() {
        return hostname;
    }

    @Override
    public boolean equals(Object other) {
        return (other instanceof ValidatedRpId) && hostname.equals(((ValidatedRpId) other).hostname);
    }

    @Override
    public int hashCode() {
        return hostname.hashCode();
    }

    @Override
    public String toString() {
        return hostname;
    }
}
