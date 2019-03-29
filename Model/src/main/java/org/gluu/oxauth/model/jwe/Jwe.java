/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jwe;

import java.security.PrivateKey;

import org.gluu.oxauth.model.exception.InvalidJweException;
import org.gluu.oxauth.model.exception.InvalidJwtException;
import org.gluu.oxauth.model.jwt.Jwt;
import org.gluu.oxauth.model.token.JsonWebResponse;

/**
 * @author Javier Rojas Blum
 * @version July 29, 2016
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
        String additionalAuthenticatedData = encodedHeader + "."
                + encodedEncryptedKey + "."
                + encodedInitializationVector;

        return additionalAuthenticatedData;
    }

    public static Jwe parse(String encodedJwe, PrivateKey privateKey, byte[] sharedSymmetricKey) throws InvalidJweException, InvalidJwtException {
        Jwe jwe = null;

        if (privateKey != null) {
            JweDecrypter jweDecrypter = new JweDecrypterImpl(privateKey);
            jwe = jweDecrypter.decrypt(encodedJwe);
        } else if (sharedSymmetricKey != null) {
            JweDecrypter jweDecrypter = new JweDecrypterImpl(sharedSymmetricKey);
            jwe = jweDecrypter.decrypt(encodedJwe);
        }

        return jwe;
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