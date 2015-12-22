/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.userinfo;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.crypto.signature.*;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtType;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * @author Javier Rojas Blum
 * @version December 17, 2015
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

    public UserInfoJwt(String jwtCode, String hsKey) throws InvalidJwtException {
        try {
            if (StringUtils.isNotBlank(jwtCode)) {
                String[] parts = jwtCode.split("\\.");

                if (parts.length == 2) {
                    encodedHeader = parts[0];
                    encodedClaim = parts[1];
                    encodedSignature = null;
                } else if (parts.length == 3) {
                    encodedHeader = parts[0];
                    encodedClaim = parts[1];
                    encodedSignature = parts[2];
                } else {
                    throw new InvalidJwtException("The JWT is not well formed");
                }

                String header = new String(JwtUtil.base64urldecode(encodedHeader), Util.UTF8_STRING_ENCODING);
                String claim = new String(JwtUtil.base64urldecode(encodedClaim), Util.UTF8_STRING_ENCODING);
                claim = claim.replace("\\", "");

                // Header
                JSONObject jsonHeader = new JSONObject(header);

                if (jsonHeader.has("typ")) {
                    String typ = jsonHeader.getString("typ");
                    type = JwtType.fromString(typ);
                }
                if (jsonHeader.has("alg")) {
                    String alg = jsonHeader.getString("alg");
                    algorithm = SignatureAlgorithm.fromName(alg);
                }
                if (jsonHeader.has("jku")) {
                    jsonWebKeyUrl = jsonHeader.getString("jku");
                }
                if (jsonHeader.has("kid")) {
                    keyId = jsonHeader.getString("kid");
                }

                // Claims
                claims = new HashMap<String, List<String>>();
                JSONObject jsonClaim = new JSONObject(claim);

                for (Iterator i = jsonClaim.keys(); i.hasNext(); ) {
                    String claimName = (String) i.next();
                    List<String> values = new ArrayList<String>();

                    JSONArray jsonArray = jsonClaim.optJSONArray(claimName);
                    if (jsonArray != null) {
                        for (int j = 0; j < jsonArray.length(); j++) {
                            String value = jsonArray.optString(j);
                            if (value != null) {
                                values.add(value);
                            }
                        }
                    } else {
                        String claimValue = jsonClaim.getString(claimName);
                        values.add(claimValue);
                    }

                    claims.put(claimName, values);
                }

                // Signature
                boolean validSignature = false;    // todo variable is not used, should it be removed?
                byte[] signature = encodedSignature != null ? JwtUtil.base64urldecode(encodedSignature) : null;
                String signingInput = encodedHeader + "." + encodedClaim;

                if ((algorithm == null || algorithm == SignatureAlgorithm.NONE)
                        && encodedSignature == null
                        && (type == null || type == JwtType.JWT)) {
                    validSignature = true;
                } else if (algorithm.getFamily() == SignatureAlgorithmFamily.HMAC
                        || algorithm.getFamily() == SignatureAlgorithmFamily.RSA
                        || algorithm.getFamily() == SignatureAlgorithmFamily.EC) {
                    RSAPublicKey rsaPublicKey = null;
                    ECDSAPublicKey ecdsaPublicKey = null;

                    switch (algorithm) {
                        case HS256:
                            validSignature = JwtUtil.verifySignatureHS256(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    hsKey);
                            break;
                        case HS384:
                            validSignature = JwtUtil.verifySignatureHS384(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    hsKey);
                            break;
                        case HS512:
                            validSignature = JwtUtil.verifySignatureHS512(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    hsKey);
                            break;
                        case RS256:
                            rsaPublicKey = (RSAPublicKey) JwtUtil.getPublicKey(jsonWebKeyUrl, null, algorithm, keyId);
                            validSignature = JwtUtil.verifySignatureRS256(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    rsaPublicKey);
                            break;
                        case RS384:
                            rsaPublicKey = (RSAPublicKey) JwtUtil.getPublicKey(jsonWebKeyUrl, null, algorithm, keyId);
                            validSignature = JwtUtil.verifySignatureRS384(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    rsaPublicKey);
                            break;
                        case RS512:
                            rsaPublicKey = (RSAPublicKey) JwtUtil.getPublicKey(jsonWebKeyUrl, null, algorithm, keyId);
                            validSignature = JwtUtil.verifySignatureRS512(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    rsaPublicKey);
                            break;
                        case ES256:
                            ecdsaPublicKey = (ECDSAPublicKey) JwtUtil.getPublicKey(jsonWebKeyUrl, null, algorithm, keyId);
                            validSignature = JwtUtil.verifySignatureES256(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    ecdsaPublicKey);
                            break;
                        case ES384:
                            ecdsaPublicKey = (ECDSAPublicKey) JwtUtil.getPublicKey(jsonWebKeyUrl, null, algorithm, keyId);
                            validSignature = JwtUtil.verifySignatureES384(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    ecdsaPublicKey);
                            break;
                        case ES512:
                            ecdsaPublicKey = (ECDSAPublicKey) JwtUtil.getPublicKey(jsonWebKeyUrl, null, algorithm, keyId);
                            validSignature = JwtUtil.verifySignatureES512(
                                    signingInput.getBytes(Util.UTF8_STRING_ENCODING),
                                    signature,
                                    ecdsaPublicKey);
                            break;
                        default:
                            validSignature = false;
                            break;
                    }
                } else {
                    throw new InvalidJwtException("Cannot validate the JWT Cryptographic segment");
                }

                if (!validSignature) {
                    throw new InvalidJwtException("The signature is not valid");
                }
            }
        } catch (JSONException e) {
            throw new InvalidJwtException(e);
        } catch (UnsupportedEncodingException e) {
            throw new InvalidJwtException(e);
        } catch (SignatureException e) {
            throw new InvalidJwtException(e);
        } catch (InvalidKeySpecException e) {
            throw new InvalidJwtException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidJwtException(e);
        } catch (BadPaddingException e) {
            throw new InvalidJwtException(e);
        } catch (InvalidKeyException e) {
            throw new InvalidJwtException(e);
        } catch (NoSuchPaddingException e) {
            throw new InvalidJwtException(e);
        } catch (IOException e) {
            throw new InvalidJwtException(e);
        } catch (NoSuchProviderException e) {
            throw new InvalidJwtException(e);
        } catch (IllegalBlockSizeException e) {
            throw new InvalidJwtException(e);
        } catch (Exception e) {
            throw new InvalidJwtException(e);
        }
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