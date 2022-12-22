/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.fido2.u2f.signature;

import io.jans.as.model.exception.SignatureException;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

public interface SignatureVerification {

    boolean checkSignature(X509Certificate attestationCertificate, byte[] signedBytes, byte[] signature) throws SignatureException;

    boolean checkSignature(PublicKey publicKey, byte[] signedBytes, byte[] signature) throws SignatureException;

    PublicKey decodePublicKey(byte[] encodedPublicKey) throws SignatureException;

    byte[] hash(byte[] bytes);

    byte[] hash(String str);

}
