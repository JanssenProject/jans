/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.fido2.service.audit.trace;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Base64;

import com.fasterxml.jackson.databind.JsonNode;

import io.jans.util.security.SecurityProviderUtility;

/**
 * Signs TRACE records with an Ed25519 producer key over their RFC 8785 canonical form (RFC 8032 signatures).
 *
 * <p>Key loading is not part of this class. The caller supplies the private key. The key-store reference that
 * supplies it was removed from #15276 and returns with key-store validation.
 */
public final class TraceSigner {

    private static final String ALGORITHM = "Ed25519";

    private final PrivateKey privateKey;

    public TraceSigner(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * @param message bytes to sign
     * @return raw 64-byte Ed25519 signature
     */
    public byte[] signBytes(byte[] message) {
        try {
            Signature signer = Signature.getInstance(ALGORITHM, SecurityProviderUtility.getBCProvider());
            signer.initSign(privateKey);
            signer.update(message);
            return signer.sign();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("TRACE signing failed", e);
        }
    }

    /**
     * @param unsignedRecord record without its signature member
     * @return base64url signature without padding
     */
    public String sign(JsonNode unsignedRecord) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(signBytes(TraceCanonicalizer.canonicalBytes(unsignedRecord)));
    }
}
