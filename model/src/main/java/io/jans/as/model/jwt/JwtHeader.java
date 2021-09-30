/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

import io.jans.as.model.crypto.encryption.BlockEncryptionAlgorithm;
import io.jans.as.model.crypto.encryption.KeyEncryptionAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import org.json.JSONObject;

import static io.jans.as.model.jwt.JwtHeaderName.*;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class JwtHeader extends JwtClaimSet {

    public JwtHeader() {
        super();
    }

    public JwtHeader(JSONObject jsonObject) {
        super(jsonObject);
    }

    public JwtHeader(String base64JsonObject) throws InvalidJwtException {
        super(base64JsonObject);
    }

    public static JwtHeader instance() {
        return new JwtHeader();
    }

    /**
     * Declares the type of this object.
     *
     * @param type The type of this object.
     */
    public JwtHeader setType(JwtType type) {
        if (type == null) {
            setNullClaim(TYPE);
        } else {
            setClaim(TYPE, type.toString());
        }
        return this;
    }

    public JwtType getType() {
        String typ = getClaimAsString(TYPE);
        return JwtType.fromString(typ);
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String alg = getClaimAsString(ALGORITHM);
        return SignatureAlgorithm.fromString(alg);
    }

    /**
     * Identifies the cryptographic algorithm used to secure the JWS.
     *
     * @param algorithm The cryptographic algorithm.
     */
    public JwtHeader setAlgorithm(SignatureAlgorithm algorithm) {
        if (algorithm == null) {
            setNullClaim(ALGORITHM);
        } else {
            setClaim(ALGORITHM, algorithm.toString());
        }
        return this;
    }

    /**
     * Identifies the cryptographic algorithm used to encrypt the JWE.
     *
     * @param algorithm The cryptographic algorithm.
     */
    public JwtHeader setAlgorithm(KeyEncryptionAlgorithm algorithm) {
        if (algorithm == null) {
            setNullClaim(ALGORITHM);
        } else {
            setClaim(ALGORITHM, algorithm.toString());
        }
        return this;
    }

    public String getKeyId() {
        return getClaimAsString(KEY_ID);
    }

    /**
     * Indicates which key was used to secure/encrypt the JWS/JWE.
     *
     * @param keyId The key id.
     */
    public JwtHeader setKeyId(String keyId) {
        setClaim(KEY_ID, keyId);
        return this;
    }

    public JSONObject getJwk() {
        return getClaimAsJSON(JWK);
    }

    public JwtHeader setJwk(JSONObject jwk) {
        setClaim(JWK, jwk);
        return this;
    }

    /**
     * In a JWS it is used to declare the type of the secured content (the Payload).
     * In a JWE it is used to declare the type of the encrypted content (the Plaintext).
     *
     * @param contentType The content type.
     */
    public void setContentType(JwtType contentType) {
        if (contentType == null) {
            setNullClaim(CONTENT_TYPE);
        } else {
            setClaim(CONTENT_TYPE, contentType.toString());
        }
    }

    public JwtType getContentType() {
        return JwtType.fromString(getClaimAsString(CONTENT_TYPE));
    }

    /**
     * Identifies the block encryption algorithm used to encrypt the Plaintext to produce the Cipher Text.
     *
     * @param encryptionMethod The JWE Encryption Method
     */
    public void setEncryptionMethod(BlockEncryptionAlgorithm encryptionMethod) {
        if (encryptionMethod == null) {
            setNullClaim(ENCRYPTION_METHOD);
        } else {
            setClaim(ENCRYPTION_METHOD, encryptionMethod.toString());
        }
    }

    public BlockEncryptionAlgorithm getEncryptionMethod() {
        return BlockEncryptionAlgorithm.fromName(getClaimAsString(ENCRYPTION_METHOD));
    }

    /**
     * Value created by the originator for the use in key agreement algorithms.
     *
     * @param ephemeralPublicKey The Ephemeral Public Key.
     */
    public void setEphemeralPublicKey(String ephemeralPublicKey) {
        setClaim(EPHEMERAL_PUBLIC_KEY, ephemeralPublicKey);
    }

    /**
     * The "zip" (compression algorithm) applied to the Plaintext before encryption, if any.
     * If present, the value of the "zip" header parameter MUST be the case sensitive string "DEF".
     * Compression is performed with the DEFLATE algorithm.
     *
     * @param compressionAlgorithm The compression algorithm.
     */
    public void setCompressionAlgorithm(String compressionAlgorithm) {
        setClaim(COMPRESSION_ALGORITHM, compressionAlgorithm);
    }

    /**
     * The "apu" (agreement PartyUInfo) value for key agreement algorithms using it (such as "ECDH-ES"),
     * represented as a base64url encoded string.
     *
     * @param agreementPartyUInfo The Agreement PartyUInfo.
     */
    public void setAgreementPartyUInfo(String agreementPartyUInfo) {
        setClaim(AGREEMENT_PARTY_U_INFO, agreementPartyUInfo);
    }

    /**
     * The "apv" (agreement PartyVInfo) value for key agreement algorithms using it (such as "ECDH-ES"),
     * represented as a base64url encoded string.
     *
     * @param agreementPartyVInfo The Agreement PartyVInfo.
     */
    public void setAgreementPartyVInfo(String agreementPartyVInfo) {
        setClaim(AGREEMENT_PARTY_V_INFO, agreementPartyVInfo);
    }

    /**
     * The "epu" (encryption PartyUInfo) value for plaintext encryption algorithms using it
     * (such as "A128CBC+HS256"), represented as a base64url encoded string.
     *
     * @param encryptionPartyUInfo The Encryption PartyUInfo.
     */
    public void setEncryptionPartyUInfo(String encryptionPartyUInfo) {
        setClaim(ENCRYPTION_PARTY_U_INFO, encryptionPartyUInfo);
    }

    /**
     * The "epv" (encryption PartyVInfo) value for plaintext encryption algorithms using it
     * (such as "A128CBC+HS256"), represented as a base64url encoded string.
     *
     * @param encryptionPartyVInfo The Encryption PartyVInfo.
     */
    public void setEncryptionPartyVInfo(String encryptionPartyVInfo) {
        setClaim(ENCRYPTION_PARTY_V_INFO, encryptionPartyVInfo);
    }
}
