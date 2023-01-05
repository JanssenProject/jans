/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */
package io.jans.as.model.jwt;

import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.AsymmetricSignatureAlgorithm;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.exception.InvalidParameterException;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.util.Base64Util;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.UUID;

import static io.jans.as.model.jwt.DPoPJwtPayloadParam.ATH;
import static io.jans.as.model.jwt.DPoPJwtPayloadParam.HTM;
import static io.jans.as.model.jwt.DPoPJwtPayloadParam.HTU;
import static io.jans.as.model.jwt.DPoPJwtPayloadParam.IAT;
import static io.jans.as.model.jwt.DPoPJwtPayloadParam.JTI;
import static io.jans.as.model.jwt.JwtType.DPOP_PLUS_JWT;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public class DPoP extends Jwt {

    private static final Logger LOG = LoggerFactory.getLogger(DPoP.class);

    private final String keyId;
    private String encodedJwt;

    // Signature Key
    private transient AbstractCryptoProvider cryptoProvider;

    public DPoP(AsymmetricSignatureAlgorithm asymmetricSignatureAlgorithm, JSONWebKey jwk, String jti, String htm, String htu,
                String keyId, AbstractCryptoProvider cryptoProvider) {
        getHeader().setType(DPOP_PLUS_JWT);
        this.keyId = keyId;
        setSignatureAlgorithm(asymmetricSignatureAlgorithm);
        setJwk(jwk);

        setJti(jti);
        setHtm(htm);
        setHtu(htu);
        setIat(new Date().getTime());

        this.cryptoProvider = cryptoProvider;
    }

    public static String generateJti() {
        String jti;

        String guid = UUID.randomUUID().toString();
        byte[] sig = Util.getBytes(guid);
        jti = Base64Util.base64urlencode(sig);

        return jti;
    }

    public static String generateAccessTokenHash(String accessToken) {
        String accessTokenHash = null;

        try {
            final byte[] digest = JwtUtil.getMessageDigestSHA256(accessToken);

            if (digest != null) {
                accessTokenHash = Base64Util.base64urlencode(digest);
            }
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            LOG.error(e.getMessage(), e);
        }

        return accessTokenHash;
    }

    /**
     * Returns the type header (typ) dpop+jwt
     *
     * @return The type header (typ) dpop+jwt
     */
    public JwtType getType() {
        return getHeader().getType();
    }

    /**
     * Returns the digital signature algorithm identifier (Asymmetric Algorithm, must not be none).
     *
     * @return The digital signature algorithm identifier.
     */
    public AsymmetricSignatureAlgorithm getSignatureAlgorithm() {
        SignatureAlgorithm signatureAlgorithm = getHeader().getSignatureAlgorithm();

        if (signatureAlgorithm == null) return null;

        return AsymmetricSignatureAlgorithm.fromString(signatureAlgorithm.getName());
    }

    /**
     * Sets the digital signature algorithm identifier (Asymmetric Algorithm, must not be none).
     *
     * @param asymmetricSignatureAlgorithm The digital signature algorithm identifier.
     */
    public void setSignatureAlgorithm(AsymmetricSignatureAlgorithm asymmetricSignatureAlgorithm) {
        getHeader().setAlgorithm(SignatureAlgorithm.fromString(asymmetricSignatureAlgorithm.getParamName()));
    }

    /**
     * Returns the public key chosen by the client, in JWK format.
     *
     * @return The public key.
     */
    public JSONWebKey getJwk() {
        return JSONWebKey.fromJSONObject(getHeader().getJwk());
    }

    /**
     * Sets the public key chosen by the client, in JWK format. Must not contain the private key.
     *
     * @param jwk The public key.
     */
    public void setJwk(JSONWebKey jwk) {
        getHeader().setJwk(jwk.toJSONObject());
    }

    /**
     * Returns the unique identifier for the DPoP proof JWT.
     *
     * @return The unique identifier for the DPoP proof JWT.
     */
    public String getJti() {
        return getClaims().getClaimAsString(JTI);
    }

    /**
     * Sets the unique identifier for the DPoP proof JWT.
     * The value must be assigned such that there is a negligible probability that the same value will be assigned
     * to any other DPoP proof used in the same context during the time window of validity.
     *
     * @param jti The unique identifier for the DPoP JWT.
     */
    public void setJti(String jti) {
        getClaims().setClaim(JTI, jti);
    }

    /**
     * Returns the HTTP method for the request to which the JWT is attached.
     *
     * @return The HTTP method.
     */
    public String getHtm() {
        return getClaims().getClaimAsString(HTM);
    }

    /**
     * Sets the HTTP method for the request to which the JWT is attached.
     *
     * @param htm The HTTP method.
     */
    public void setHtm(String htm) {
        getClaims().setClaim(HTM, htm);
    }

    /**
     * Returns the HTTP URI used for the request, without query and fragment parts.
     *
     * @return The HTTP URI used for the request.
     */
    public String getHtu() {
        return getClaims().getClaimAsString(HTU);
    }

    /**
     * Sets the HTTP URI used for the request, without query and fragment parts.
     *
     * @param htu The HTTP URI used for the request.
     */
    public void setHtu(String htu) {
        getClaims().setClaim(HTU, htu);
    }

    /**
     * Returns the time at which the JWT was created.
     *
     * @return The time at which the JWT was created.
     */
    public Long getIat() {
        return getClaims().getClaimAsLong(IAT);
    }

    /**
     * Sets the time at which the JWT was created.
     *
     * @param iat The time at which the JWT was created.
     */
    public void setIat(Long iat) {
        getClaims().setClaim(IAT, iat);
    }

    /**
     * Returns the Hash of the access token.
     * Required when the DPoP proof is used in conjunction with the presentation of an access token.
     *
     * @return The Hash of the access token.
     */
    public String getAth() {
        return getClaims().getClaimAsString(ATH);
    }

    public void setAth(String ath) {
        getClaims().setClaim(ATH, ath);
    }

    public AbstractCryptoProvider getCryptoProvider() {
        return cryptoProvider;
    }

    public void setCryptoProvider(AbstractCryptoProvider cryptoProvider) {
        this.cryptoProvider = cryptoProvider;
    }

    public String getEncodedJwt() throws InvalidJwtException, InvalidParameterException, CryptoProviderException {
        // Check header params:
        if (getType() != JwtType.DPOP_PLUS_JWT) {
            throw new InvalidJwtException("Type (typ) value must be dpop+jwt");
        }
        if (getSignatureAlgorithm() == null) {
            throw new InvalidJwtException("Algorithm (alg) must be an asymmetric algorithm");
        }
        if (getJwk() == null) {
            throw new InvalidJwtException("JWK (jwk) is required");
        }

        // Check Payload params:
        if (StringUtils.isBlank(getJti())) {
            throw new InvalidJwtException("The JWT Unique identifier (jti) is required");
        }
        if (StringUtils.isBlank(getHtm())) {
            throw new InvalidJwtException("The HTTP method (htm) is required");
        }
        if (StringUtils.isBlank(getHtu())) {
            throw new InvalidJwtException("The HTTP URI (htu) is required");
        }
        if (getIat() == null || getIat() <= 0) {
            throw new InvalidJwtException("The issued at (iat) is required");
        }

        if (cryptoProvider == null) {
            throw new InvalidParameterException("The Crypto Provider cannot be null.");
        }

        String encodedHeader = getHeader().toBase64JsonObject();
        String encodedPayload = getClaims().toBase64JsonObject();

        String signingInput = encodedHeader + "." + encodedPayload;
        String encodedSignature = cryptoProvider.sign(signingInput, keyId, null, getHeader().getSignatureAlgorithm());

        encodedJwt = encodedHeader + "." + encodedPayload + "." + encodedSignature;

        return encodedJwt;
    }

    @Override
    public String toString() {
        return encodedJwt;
    }
}
