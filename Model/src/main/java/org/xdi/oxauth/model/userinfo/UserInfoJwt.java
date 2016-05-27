/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.userinfo;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Javier Rojas Blum
 * @version May 5, 2016
 */
public class UserInfoJwt {

    // Header
    private JwtType type;
    private SignatureAlgorithm algorithm;
    private String jsonWebKeyUrl;
    private String keyId;

    // Claims
    private Map<String, List<String>> claims;

    private String encodedHeader;
    private String encodedClaim;
    private String encodedSignature;

    public UserInfoJwt() {
        type = JwtType.JWT;
        algorithm = SignatureAlgorithm.NONE;
        claims = new HashMap<String, List<String>>();
    }

    public String getEncodedJwt(String hsKey) throws InvalidJwtException {
        return getEncodedJwt(hsKey, null, null);
    }

    public String getEncodedJwt(RSAPrivateKey rsaPrivateKey) throws InvalidJwtException {
        return getEncodedJwt(null, rsaPrivateKey, null);
    }

    public String getEncodedJwt(ECDSAPrivateKey ecdsaPrivateKey) throws InvalidJwtException {
        return getEncodedJwt(null, null, ecdsaPrivateKey);
    }

    private String getEncodedJwt(String hsKey, RSAPrivateKey rsaPrivateKey, ECDSAPrivateKey ecdsaPrivateKey) throws InvalidJwtException {
        StringBuilder builder = new StringBuilder();

        try {
            // Header
            JSONObject jsonHeader = new JSONObject();

            jsonHeader.put("typ", type);
            if (algorithm != null) {
                jsonHeader.put("alg", algorithm.toString());

                if (StringUtils.isNotBlank(jsonWebKeyUrl)) {
                    jsonHeader.put("jku", jsonWebKeyUrl);
                }
                if (StringUtils.isNotBlank(keyId)) {
                    jsonHeader.put("kid", keyId);
                }
            }

            encodedHeader = JwtUtil.base64urlencode(jsonHeader.toString().getBytes(Util.UTF8_STRING_ENCODING));

            // Claims
            JSONObject jsonClaim = new JSONObject();

            for (String key : claims.keySet()) {
                jsonClaim.put(key, claims.get(key));
            }

            encodedClaim = JwtUtil.base64urlencode(jsonClaim.toString().getBytes(Util.UTF8_STRING_ENCODING));

            // Signature
            String signingInput = encodedHeader + "." + encodedClaim;
            byte[] sign = null;

            if (algorithm != null) {
                switch (algorithm) {
                    case HS256:
                        sign = JwtUtil.getSignatureHS256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), hsKey.getBytes(Util.UTF8_STRING_ENCODING));
                        break;
                    case HS384:
                        sign = JwtUtil.getSignatureHS384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), hsKey.getBytes(Util.UTF8_STRING_ENCODING));
                        break;
                    case HS512:
                        sign = JwtUtil.getSignatureHS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), hsKey.getBytes(Util.UTF8_STRING_ENCODING));
                        break;
                    case RS256:
                        sign = JwtUtil.getSignatureRS256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), rsaPrivateKey);
                        break;
                    case RS384:
                        sign = JwtUtil.getSignatureRS384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), rsaPrivateKey);
                        break;
                    case RS512:
                        sign = JwtUtil.getSignatureRS512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), rsaPrivateKey);
                        break;
                    case ES256:
                        sign = JwtUtil.getSignatureES256(signingInput.getBytes(Util.UTF8_STRING_ENCODING), ecdsaPrivateKey);
                        break;
                    case ES384:
                        sign = JwtUtil.getSignatureES384(signingInput.getBytes(Util.UTF8_STRING_ENCODING), ecdsaPrivateKey);
                        break;
                    case ES512:
                        sign = JwtUtil.getSignatureES512(signingInput.getBytes(Util.UTF8_STRING_ENCODING), ecdsaPrivateKey);
                        break;
                    default:
                        sign = null;
                        break;
                }
            }

            if (sign != null) {
                encodedSignature = JwtUtil.base64urlencode(sign);
                builder.append(encodedHeader)
                        .append(".")
                        .append(encodedClaim)
                        .append(".")
                        .append(encodedSignature);
            } else {
                builder.append(encodedHeader)
                        .append(".")
                        .append(encodedClaim);
            }
        } catch (JSONException e) {
            throw new InvalidJwtException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJwtException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidJwtException(e);
        } catch (InvalidKeyException e) {
            throw new InvalidJwtException(e);
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }

        return builder.toString();
    }

    public JwtType getType() {
        return type;
    }

    public void setType(JwtType type) {
        this.type = type;
    }

    public SignatureAlgorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(SignatureAlgorithm algorithm) {
        this.algorithm = algorithm;
    }

    public String getJsonWebKeyUrl() {
        return jsonWebKeyUrl;
    }

    public void setJsonWebKeyUrl(String jsonWebKeyUrl) {
        this.jsonWebKeyUrl = jsonWebKeyUrl;
    }

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public Map<String, List<String>> getClaims() {
        return claims;
    }

    public List<String> getClaim(String name) {
        return claims.get(name);
    }

    public void setClaim(String name, List<String> value) {
        this.claims.put(name, value);
    }
}