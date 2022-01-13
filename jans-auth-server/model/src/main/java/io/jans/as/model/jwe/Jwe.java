/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwe;

import io.jans.as.model.exception.InvalidJweException;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.token.JsonWebResponse;

import java.security.PrivateKey;

/**
 * Encrypted and Signed JWT.
 * 
 * @author Javier Rojas Blum
 * @author Sergey Manoylo
 * @version September 13, 2021
 */
public class Jwe extends JsonWebResponse {

    private String encodedHeader;
    private String encodedEncryptedKey;
    private String encodedInitializationVector;
    private String encodedCiphertext;
    private String encodedIntegrityValue;

    private Jwt signedJWTPayload;

    public Jwe() {
        encodedHeader = null;
        encodedEncryptedKey = null;
        encodedInitializationVector = null;
        encodedCiphertext = null;
        encodedIntegrityValue = null;
    }

    public static Jwe parse(String encodedJwe, PrivateKey privateKey, byte[] sharedSymmetricKey, String sharedSymmetricPassword) throws InvalidJweException {
        Jwe jwe = null;
        JweDecrypter jweDecrypter = null;
        if (privateKey != null) {
            jweDecrypter = new JweDecrypterImpl(privateKey);
        } else if (sharedSymmetricKey != null) {
            jweDecrypter = new JweDecrypterImpl(sharedSymmetricKey);
        } else if (sharedSymmetricPassword != null) {
            jweDecrypter = new JweDecrypterImpl(sharedSymmetricPassword);
        } else {
            throw new InvalidJweException("privateKey, sharedSymmetricKey, sharedSymmetricPassword: keys aren't defined");
        }
        jwe = jweDecrypter.decrypt(encodedJwe);
        return jwe;
    }

    public static Jwe parse(String encodedJwe, PrivateKey privateKey, byte[] sharedSymmetricKey) throws InvalidJweException {
        return parse(encodedJwe, privateKey, sharedSymmetricKey, null);
    }

    public static Jwe parse(String encodedJwe, PrivateKey privateKey) throws InvalidJweException {
        return parse(encodedJwe, privateKey, null, null);
    }

    public String getEncodedHeader() {
        return encodedHeader;
    }

    public void setEncodedHeader(String encodedHeader) {
        this.encodedHeader = encodedHeader;
    }

    public String getEncodedEncryptedKey() {
        return encodedEncryptedKey;
    }

    public void setEncodedEncryptedKey(String encodedEncryptedKey) {
        this.encodedEncryptedKey = encodedEncryptedKey;
    }

    public String getEncodedInitializationVector() {
        return encodedInitializationVector;
    }

    public void setEncodedInitializationVector(String encodedInitializationVector) {
        this.encodedInitializationVector = encodedInitializationVector;
    }

    public String getEncodedCiphertext() {
        return encodedCiphertext;
    }

    public void setEncodedCiphertext(String encodedCiphertext) {
        this.encodedCiphertext = encodedCiphertext;
    }

    public String getEncodedIntegrityValue() {
        return encodedIntegrityValue;
    }

    public void setEncodedIntegrityValue(String encodedIntegrityValue) {
        this.encodedIntegrityValue = encodedIntegrityValue;
    }

    public String getAdditionalAuthenticatedData() {
        return encodedHeader + "."
                + encodedEncryptedKey + "."
                + encodedInitializationVector;
    }

    public Jwt getSignedJWTPayload() {
        return signedJWTPayload;
    }

    public void setSignedJWTPayload(Jwt signedJWTPayload) {
        this.signedJWTPayload = signedJWTPayload;
    }

    @Override
    public String toString() {
        return encodedHeader + "."
                + encodedEncryptedKey + "."
                + encodedInitializationVector + "."
                + encodedCiphertext + "."
                + encodedIntegrityValue;
    }
}