/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.crypto.signature;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.gluu.oxauth.model.exception.SignatureException;

public interface SignatureVerification {

	boolean checkSignature(X509Certificate attestationCertificate, byte[] signedBytes, byte[] signature) throws SignatureException;

    boolean checkSignature(PublicKey publicKey, byte[] signedBytes, byte[] signature) throws SignatureException;

    PublicKey decodePublicKey(byte[] encodedPublicKey) throws SignatureException;

    byte[] hash(byte[] bytes);

    byte[] hash(String str);

}
