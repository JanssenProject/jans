/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client.model;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.AbstractCryptoProvider;
import org.xdi.oxauth.model.crypto.encryption.BlockEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.encryption.KeyEncryptionAlgorithm;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwe.Jwe;
import org.xdi.oxauth.model.jwe.JweEncrypterImpl;
import org.xdi.oxauth.model.jwt.JwtClaims;
import org.xdi.oxauth.model.jwt.JwtHeader;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.util.Base64Util;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.PublicKey;

import static org.xdi.oxauth.model.jwt.JwtStateClaimName.*;

/**
 * @author Javier Rojas Blum
 * @version November 20, 2018
 */
public class JwtState {

    private static final Logger LOG = Logger.getLogger(JwtState.class);

    // Header
    private JwtType type;
    private SignatureAlgorithm signatureAlgorithm;
    private KeyEncryptionAlgorithm keyEncryptionAlgorithm;
    private BlockEncryptionAlgorithm blockEncryptionAlgorithm;
    private String keyId;

    // Payload
    private String rfp;
    private String iat;
    private String exp;
    private String iss;
    private String aud;
    private String targetLinkUri;
    private String as;
    private String jti;
    private String atHash;
    private String cHash;
    private JSONObject additionalClaims;

    // Signature/Encryption Keys
    private String sharedKey;
    private AbstractCryptoProvider cryptoProvider;

    public JwtState(SignatureAlgorithm signatureAlgorithm, AbstractCryptoProvider cryptoProvider) {
        this(signatureAlgorithm, cryptoProvider, null, null, null);
    }

    public JwtState(SignatureAlgorithm signatureAlgorithm,
                    String sharedKey, AbstractCryptoProvider cryptoProvider) {
        this(signatureAlgorithm, cryptoProvider, null, null, sharedKey);
    }

    public JwtState(KeyEncryptionAlgorithm keyEncryptionAlgorithm,
                    BlockEncryptionAlgorithm blockEncryptionAlgorithm, AbstractCryptoProvider cryptoProvider) {
        this(null, cryptoProvider, keyEncryptionAlgorithm, blockEncryptionAlgorithm, null);
    }

    public JwtState(KeyEncryptionAlgorithm keyEncryptionAlgorithm,
                    BlockEncryptionAlgorithm blockEncryptionAlgorithm, String sharedKey) {
        this(null, null, keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedKey);
    }

    private JwtState(SignatureAlgorithm signatureAlgorithm,
                     AbstractCryptoProvider cryptoProvider, KeyEncryptionAlgorithm keyEncryptionAlgorithm,
                     BlockEncryptionAlgorithm blockEncryptionAlgorithm, String sharedKey) {
        this.type = JwtType.JWT;
        this.signatureAlgorithm = signatureAlgorithm;
        this.cryptoProvider = cryptoProvider;
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
        this.blockEncryptionAlgorithm = blockEncryptionAlgorithm;
        this.sharedKey = sharedKey;
    }

    public JwtType getType() {
        return type;
    }

    public void setType(JwtType type) {
        this.type = type;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public void setSignatureAlgorithm(SignatureAlgorithm signatureAlgorithm) {
        this.signatureAlgorithm = signatureAlgorithm;
    }

    public KeyEncryptionAlgorithm getKeyEncryptionAlgorithm() {
        return keyEncryptionAlgorithm;
    }

    public void setKeyEncryptionAlgorithm(KeyEncryptionAlgorithm keyEncryptionAlgorithm) {
        this.keyEncryptionAlgorithm = keyEncryptionAlgorithm;
    }

    public BlockEncryptionAlgorithm getBlockEncryptionAlgorithm() {
        return blockEncryptionAlgorithm;
    }

    public void setBlockEncryptionAlgorithm(BlockEncryptionAlgorithm blockEncryptionAlgorithm) {
        this.blockEncryptionAlgorithm = blockEncryptionAlgorithm;
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

    /**
     * String containing a verifiable identifier for the browser session,
     * that cannot be guessed by a third party.
     * The verification of this element by the client protects it from
     * accepting authorization responses generated in response to forged
     * requests generated by third parties.
     *
     * @return The Request Forgery Protection value
     */
    public String getRfp() {
        return rfp;
    }

    /**
     * String containing a verifiable identifier for the browser session,
     * that cannot be guessed by a third party.
     * The verification of this element by the client protects it from
     * accepting authorization responses generated in response to forged
     * requests generated by third parties.
     *
     * @param rfp The Request Forgery Protection value
     */
    public void setRfp(String rfp) {
        this.rfp = rfp;
    }

    /**
     * Timestamp of when this Authorization Request was issued.
     *
     * @return The Issued at value
     */
    public String getIat() {
        return iat;
    }

    /**
     * Timestamp of when this Authorization Request was issued.
     *
     * @param iat The Issued at value
     */
    public void setIat(String iat) {
        this.iat = iat;
    }

    /**
     * The expiration time claim identifies the expiration time on or after which
     * the JWT MUST NOT be accepted for processing.
     * The processing of the "exp" claim requires that the current date/time MUST
     * be before the expiration date/time listed in the "exp" claim.
     * Implementers MAY provide for some small leeway, usually no more than a
     * few minutes, to account for clock skew.
     * Its value MUST be a number containing an IntDate value.
     *
     * @return The expiration time value
     */
    public String getExp() {
        return exp;
    }

    /**
     * The expiration time claim identifies the expiration time on or after which
     * the JWT MUST NOT be accepted for processing.
     * The processing of the "exp" claim requires that the current date/time MUST
     * be before the expiration date/time listed in the "exp" claim.
     * Implementers MAY provide for some small leeway, usually no more than a
     * few minutes, to account for clock skew.
     * Its value MUST be a number containing an IntDate value.
     *
     * @param exp The expiration time value
     */
    public void setExp(String exp) {
        this.exp = exp;
    }

    /**
     * String identifying the party that issued this state value.
     *
     * @return The issuer value
     */
    public String getIss() {
        return iss;
    }

    /**
     * String identifying the party that issued this state value.
     *
     * @param iss The issuer value
     */
    public void setIss(String iss) {
        this.iss = iss;
    }

    /**
     * String identifying the client that this state value is intended for.
     *
     * @return The audience
     */
    public String getAud() {
        return aud;
    }

    /**
     * String identifying the client that this state value is intended for.
     *
     * @param aud The audience
     */
    public void setAud(String aud) {
        this.aud = aud;
    }

    /**
     * URI containing the location the user agent is to be redirected to after authorization.
     *
     * @return The target link URI
     */
    public String getTargetLinkUri() {
        return targetLinkUri;
    }

    /**
     * URI containing the location the user agent is to be redirected to after authorization.
     *
     * @param targetLinkUri The target link URI
     */
    public void setTargetLinkUri(String targetLinkUri) {
        this.targetLinkUri = targetLinkUri;
    }

    /**
     * String identifying the authorization server that this request was sent to.
     *
     * @return The authorization server
     */
    public String getAs() {
        return as;
    }

    /**
     * String identifying the authorization server that this request was sent to.
     *
     * @param as The authorization server
     */
    public void setAs(String as) {
        this.as = as;
    }

    /**
     * The "jti" (JWT ID) claim provides a unique identifier for the JWT.
     * The identifier value MUST be assigned in a manner that ensures that
     * there is a negligible probability that the same value will be
     * accidentally assigned to a different data object.
     * The "jti" claim can be used to prevent the JWT from being replayed.
     * The "jti" value is a case-sensitive string.
     *
     * @return The JWT ID
     */
    public String getJti() {
        return jti;
    }

    /**
     * The "jti" (JWT ID) claim provides a unique identifier for the JWT.
     * The identifier value MUST be assigned in a manner that ensures that
     * there is a negligible probability that the same value will be
     * accidentally assigned to a different data object.
     * The "jti" claim can be used to prevent the JWT from being replayed.
     * The "jti" value is a case-sensitive string.
     *
     * @param jti The JWT ID
     */
    public void setJti(String jti) {
        this.jti = jti;
    }

    /**
     * Access Token hash value. Its value is the base64url encoding of the left-most half
     * of the hash of the octets of the ASCII representation of the "access_token" value,
     * where the hash algorithm used is the hash algorithm used in the "alg" parameter of
     * the State Token's JWS header.
     * For instance, if the "alg" is "RS256", hash the "access_token" value with SHA-256,
     * then take the left-most 128 bits and base64url encode them.
     * The "at_hash" value is a case sensitive string.
     * This is REQUIRED if the JWT [RFC7519] state token is being produced by the AS and
     * issued with a "access_token" in the authorization response.
     *
     * @return The access token hash value
     */
    public String getAtHash() {
        return atHash;
    }

    /**
     * Access Token hash value. Its value is the base64url encoding of the left-most half
     * of the hash of the octets of the ASCII representation of the "access_token" value,
     * where the hash algorithm used is the hash algorithm used in the "alg" parameter of
     * the State Token's JWS header.
     * For instance, if the "alg" is "RS256", hash the "access_token" value with SHA-256,
     * then take the left-most 128 bits and base64url encode them.
     * The "at_hash" value is a case sensitive string.
     * This is REQUIRED if the JWT [RFC7519] state token is being produced by the AS and
     * issued with a "access_token" in the authorization response.
     *
     * @param atHash The access token hash value
     */
    public void setAtHash(String atHash) {
        this.atHash = atHash;
    }

    /**
     * Code hash value. Its value is the base64url encoding of the left-most half of the
     * hash of the octets of the ASCII representation of the "code" value, where the hash
     * algorithm used is the hash algorithm used in the "alg" header parameter of the
     * State Token's JWS [RFC7515] header.
     * For instance, if the "alg" is "HS512", hash the "code" value with SHA-512, then
     * take the left-most 256 bits and base64url encode them.
     * The "c_hash" value is a case sensitive string.
     * This is REQUIRED if the JWT [RFC7519] state token is being produced by the AS and
     * issued with a "code" in the authorization response.
     *
     * @return The code hash value
     */
    public String getcHash() {
        return cHash;
    }

    /**
     * Code hash value. Its value is the base64url encoding of the left-most half of the
     * hash of the octets of the ASCII representation of the "code" value, where the hash
     * algorithm used is the hash algorithm used in the "alg" header parameter of the
     * State Token's JWS [RFC7515] header.
     * For instance, if the "alg" is "HS512", hash the "code" value with SHA-512, then
     * take the left-most 256 bits and base64url encode them.
     * The "c_hash" value is a case sensitive string.
     * This is REQUIRED if the JWT [RFC7519] state token is being produced by the AS and
     * issued with a "code" in the authorization response.
     *
     * @param cHash The code hash value
     */
    public void setcHash(String cHash) {
        this.cHash = cHash;
    }

    public JSONObject getAdditionalClaims() {
        return additionalClaims;
    }

    public void setAdditionalClaims(JSONObject additionalClaims) {
        this.additionalClaims = additionalClaims;
    }

    public String getEncodedJwt(JSONObject jwks) throws Exception {
        String encodedJwt = null;

        if (keyEncryptionAlgorithm != null && blockEncryptionAlgorithm != null) {
            JweEncrypterImpl jweEncrypter;
            if (cryptoProvider != null && jwks != null) {
                PublicKey publicKey = cryptoProvider.getPublicKey(keyId, jwks);
                jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, publicKey);
            } else {
                jweEncrypter = new JweEncrypterImpl(keyEncryptionAlgorithm, blockEncryptionAlgorithm, sharedKey.getBytes(Util.UTF8_STRING_ENCODING));
            }

            String header = headerToJSONObject().toString();
            String encodedHeader = Base64Util.base64urlencode(header.getBytes(Util.UTF8_STRING_ENCODING));

            String claims = payloadToJSONObject().toString();
            String encodedClaims = Base64Util.base64urlencode(claims.getBytes(Util.UTF8_STRING_ENCODING));

            Jwe jwe = new Jwe();
            jwe.setHeader(new JwtHeader(encodedHeader));
            jwe.setClaims(new JwtClaims(encodedClaims));
            jweEncrypter.encrypt(jwe);

            encodedJwt = jwe.toString();
        } else {
            if (cryptoProvider == null) {
                throw new Exception("The Crypto Provider cannot be null.");
            }

            JSONObject headerJsonObject = headerToJSONObject();
            JSONObject payloadJsonObject = payloadToJSONObject();
            String headerString = headerJsonObject.toString();
            String payloadString = payloadJsonObject.toString();
            String encodedHeader = Base64Util.base64urlencode(headerString.getBytes(Util.UTF8_STRING_ENCODING));
            String encodedPayload = Base64Util.base64urlencode(payloadString.getBytes(Util.UTF8_STRING_ENCODING));
            String signingInput = encodedHeader + "." + encodedPayload;
            String encodedSignature = cryptoProvider.sign(signingInput, keyId, sharedKey, signatureAlgorithm);

            encodedJwt = encodedHeader + "." + encodedPayload + "." + encodedSignature;
        }

        return encodedJwt;
    }

    public String getEncodedJwt() throws Exception {
        return getEncodedJwt(null);
    }

    protected JSONObject headerToJSONObject() throws InvalidJwtException {
        JwtHeader jwtHeader = new JwtHeader();

        jwtHeader.setType(type);
        if (keyEncryptionAlgorithm != null && blockEncryptionAlgorithm != null) {
            jwtHeader.setAlgorithm(keyEncryptionAlgorithm);
            jwtHeader.setEncryptionMethod(blockEncryptionAlgorithm);
        } else {
            jwtHeader.setAlgorithm(signatureAlgorithm);
        }
        jwtHeader.setKeyId(keyId);

        return jwtHeader.toJsonObject();
    }

    protected JSONObject payloadToJSONObject() throws JSONException {
        JSONObject obj = new JSONObject();

        try {
            if (StringUtils.isNotBlank(rfp)) {
                obj.put(RFP, rfp);
            }
            if (StringUtils.isNotBlank(keyId)) {
                obj.put(KID, keyId);
            }
            if (StringUtils.isNotBlank(iat)) {
                obj.put(IAT, iat);
            }
            if (StringUtils.isNotBlank(exp)) {
                obj.put(EXP, exp);
            }
            if (StringUtils.isNotBlank(iss)) {
                obj.put(ISS, iss);
            }
            if (StringUtils.isNotBlank(aud)) {
                obj.put(AUD, aud);
            }
            if (StringUtils.isNotBlank(targetLinkUri)) {
                obj.put(TARGET_LINK_URI, URLEncoder.encode(targetLinkUri, "UTF-8"));
            }
            if (StringUtils.isNotBlank(as)) {
                obj.put(AS, as);
            }
            if (StringUtils.isNotBlank(jti)) {
                obj.put(JTI, jti);
            }
            if (StringUtils.isNotBlank(atHash)) {
                obj.put(AT_HASH, atHash);
            }
            if (StringUtils.isNotBlank(cHash)) {
                obj.put(C_HASH, cHash);
            }
            if (additionalClaims != null) {
                obj.put(ADDITIONAL_CLAIMS, additionalClaims);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return obj;
    }
}
