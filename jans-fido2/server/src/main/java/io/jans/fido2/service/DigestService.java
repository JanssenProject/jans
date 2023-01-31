/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Method to calculate digests
 *
 */
@ApplicationScoped
public class DigestService {

    public byte[] hashSha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] hashSha256(String str) {
        return hashSha256(str.getBytes());
    }

}
