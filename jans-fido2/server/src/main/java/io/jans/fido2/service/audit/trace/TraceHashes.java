/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;

/**
 * SHA-256 helpers for TRACE. The prefixed form uses the {@code sha256:} convention of the Lock server.
 */
public final class TraceHashes {

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    private TraceHashes() {
    }

    /**
     * @param input bytes to hash, typically RFC 8785 canonical bytes
     * @return lowercase hex SHA-256 digest
     */
    public static String sha256Hex(byte[] input) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            char[] out = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) {
                out[i * 2] = HEX[(digest[i] >> 4) & 0xF];
                out[i * 2 + 1] = HEX[digest[i] & 0xF];
            }
            return new String(out);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /**
     * @param input bytes to hash
     * @return {@code sha256:} followed by the lowercase hex digest
     */
    public static String sha256Prefixed(byte[] input) {
        return "sha256:" + sha256Hex(input);
    }
}
