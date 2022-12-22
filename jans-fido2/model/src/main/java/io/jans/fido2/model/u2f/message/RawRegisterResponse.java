/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.model.u2f.message;

import java.security.cert.X509Certificate;

/**
 * The register response produced by the token/key, which is transformed by the
 * client into an RegisterResponse and sent to the server.
 *
 * @author Yuriy Movchan Date: 05/14/2015
 */
public class RawRegisterResponse {

    /**
     * The (uncompressed) x,y-representation of a curve point on the P-256 NIST
     * elliptic curve.
     */
    private final byte[] userPublicKey;

    /**
     * A handle that allows the U2F token to identify the generated key pair.
     */
    private final byte[] keyHandle;
    private final X509Certificate attestationCertificate;

    /**
     * A ECDSA signature (on P-256)
     */
    private final byte[] signature;

    public RawRegisterResponse(byte[] userPublicKey, byte[] keyHandle, X509Certificate attestationCertificate, byte[] signature) {
        this.userPublicKey = userPublicKey;
        this.keyHandle = keyHandle;
        this.attestationCertificate = attestationCertificate;
        this.signature = signature;
    }

    public byte[] getUserPublicKey() {
        return userPublicKey;
    }

    public byte[] getKeyHandle() {
        return keyHandle;
    }

    public X509Certificate getAttestationCertificate() {
        return attestationCertificate;
    }

    public byte[] getSignature() {
        return signature;
    }

}
