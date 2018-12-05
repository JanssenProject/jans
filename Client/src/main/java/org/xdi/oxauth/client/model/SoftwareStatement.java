/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.model;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

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
    private String sharedKey;
    private AbstractCryptoProvider cryptoProvider;

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
        String headerString = headerJsonObject.toString();
        String payloadString = payloadJsonObject.toString();
        String encodedHeader = Base64Util.base64urlencode(headerString.getBytes(Util.UTF8_STRING_ENCODING));
        String encodedPayload = Base64Util.base64urlencode(payloadString.getBytes(Util.UTF8_STRING_ENCODING));
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