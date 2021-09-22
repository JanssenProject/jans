/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.model;

import io.jans.as.client.util.ClientUtil;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwt.JwtHeader;
import io.jans.as.model.util.Base64Util;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

/**
 * @author Javier Rojas Blum
 * @version December 4, 2018
 */
public class SoftwareStatement {

    private static final Logger LOG = Logger.getLogger(JwtState.class);

    // Header
    private SignatureAlgorithm signatureAlgorithm;
    private String keyId;

    // Payload
    private JSONObject claims;

    // Signature/Encryption Keys
    private final String sharedKey;
    private final AbstractCryptoProvider cryptoProvider;

    public SoftwareStatement(SignatureAlgorithm signatureAlgorithm, AbstractCryptoProvider cryptoProvider) {
        this(signatureAlgorithm, cryptoProvider, null);
    }

    public SoftwareStatement(SignatureAlgorithm signatureAlgorithm,
                             String sharedKey, AbstractCryptoProvider cryptoProvider) {
        this(signatureAlgorithm, cryptoProvider, sharedKey);
    }

    private SoftwareStatement(SignatureAlgorithm signatureAlgorithm, AbstractCryptoProvider cryptoProvider, String sharedKey) {
        this.signatureAlgorithm = signatureAlgorithm;
        this.cryptoProvider = cryptoProvider;
        this.sharedKey = sharedKey;
        this.claims = new JSONObject();
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    /**
     * Identifier of the key used to sign this state token at the issuer.
     * Identifier of the key used to encrypt this JWT state token at the issuer.
     *
     * @return The key identifier
     */
    public String getKeyId() {
        return keyId;
    }

    /**
     * Identifier of the key used to sign this state token at the issuer.
     * Identifier of the key used to encrypt this JWT state token at the issuer.
     *
     * @param keyId The key identifier
     */
    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public JSONObject getClaims() {
        return claims;
    }

    public void setClaims(JSONObject claims) {
        this.claims = claims;
    }

    public String getEncodedJwt(JSONObject jwks) throws Exception {
        String encodedJwt = null;

        if (cryptoProvider == null) {
            throw new Exception("The Crypto Provider cannot be null.");
        }

        JSONObject headerJsonObject = headerToJSONObject();
        JSONObject payloadJsonObject = getClaims();
        String headerString = ClientUtil.toPrettyJson(headerJsonObject);
        String payloadString = ClientUtil.toPrettyJson(payloadJsonObject);
        String encodedHeader = Base64Util.base64urlencode(headerString.getBytes(StandardCharsets.UTF_8));
        String encodedPayload = Base64Util.base64urlencode(payloadString.getBytes(StandardCharsets.UTF_8));
        String signingInput = encodedHeader + "." + encodedPayload;
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, sharedKey, signatureAlgorithm);

        encodedJwt = encodedHeader + "." + encodedPayload + "." + encodedSignature;

        return encodedJwt;
    }

    public String getEncodedJwt() throws Exception {
        return getEncodedJwt(null);
    }

    protected JSONObject headerToJSONObject() throws InvalidJwtException {
        JwtHeader jwtHeader = new JwtHeader();

        jwtHeader.setAlgorithm(signatureAlgorithm);
        jwtHeader.setKeyId(keyId);

        return jwtHeader.toJsonObject();
    }

}