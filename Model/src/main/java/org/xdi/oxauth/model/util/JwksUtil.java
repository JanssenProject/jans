/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.util;

import static org.xdi.oxauth.model.jwk.JWKParameter.ALGORITHM;
import static org.xdi.oxauth.model.jwk.JWKParameter.D;
import static org.xdi.oxauth.model.jwk.JWKParameter.EXPONENT;
import static org.xdi.oxauth.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_ID;
import static org.xdi.oxauth.model.jwk.JWKParameter.MODULUS;
import static org.xdi.oxauth.model.jwk.JWKParameter.PRIVATE_KEY;

import java.math.BigInteger;

import javax.ws.rs.HttpMethod;

import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.xdi.oxauth.model.crypto.PrivateKey;
import org.xdi.oxauth.model.crypto.signature.ECDSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.RSAPrivateKey;
import org.xdi.oxauth.model.crypto.signature.SignatureAlgorithm;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan
 * @version May 5, 2016
 */
public class JwksUtil {

    private static final Logger log = Logger.getLogger(JwksUtil.class);

    public static JSONObject getJsonKey(String jwksUri, String jwks, String keyId) {
        log.debug("Retrieving JWK Key...");

        JSONObject jsonKey = null;
        try {
            if (StringHelper.isEmpty(jwks)) {
                ClientRequest clientRequest = new ClientRequest(jwksUri);
                clientRequest.setHttpMethod(HttpMethod.GET);
                ClientResponse<String> clientResponse = clientRequest.get(String.class);

                int status = clientResponse.getStatus();
                log.debug(String.format("Status: %n%d", status));

                if (status == 200) {
                    jwks = clientResponse.getEntity(String.class);
                    log.debug(String.format("JWK: %s", jwks));
                }
            }
            if (StringHelper.isNotEmpty(jwks)) {
                JSONObject jsonObject = new JSONObject(jwks);
                JSONArray keys = jsonObject.getJSONArray(JSON_WEB_KEY_SET);
                if (keys.length() > 0) {
                    if (StringHelper.isEmpty(keyId)) {
                        jsonKey = keys.getJSONObject(0);
                    } else {
                        for (int i = 0; i < keys.length(); i++) {
                            JSONObject kv = keys.getJSONObject(i);
                            if (kv.getString(KEY_ID).equals(keyId)) {
                                jsonKey = kv;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return jsonKey;
    }

    public static PrivateKey getPrivateKey(String jwksUri, String jwks, String keyId) {
        log.debug("Retrieving JWK Private Key...");

        JSONObject jsonKeyValue = getJsonKey(jwksUri, jwks, keyId);
        if (jsonKeyValue == null) {
            return null;
        }

        PrivateKey privateKey = null;

        try {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromName(jsonKeyValue.getString(ALGORITHM));
            String resultKeyId = jsonKeyValue.getString(KEY_ID);
            if (signatureAlgorithm == null) {
                log.error(String.format("Failed to determine key '%s' signature algorithm", resultKeyId));
                return null;
            }

            JSONObject jsonPrivateKey = jsonKeyValue.getJSONObject(PRIVATE_KEY);
            if (signatureAlgorithm == SignatureAlgorithm.RS256 || signatureAlgorithm == SignatureAlgorithm.RS384 || signatureAlgorithm == SignatureAlgorithm.RS512) {
                String exp = jsonPrivateKey.getString(EXPONENT);
                String mod = jsonPrivateKey.getString(MODULUS);

                BigInteger privateExponent = new BigInteger(1, JwtUtil.base64urldecode(exp));
                BigInteger modulus = new BigInteger(1, JwtUtil.base64urldecode(mod));

                privateKey = new RSAPrivateKey(modulus, privateExponent);
            } else if (signatureAlgorithm == SignatureAlgorithm.ES256 || signatureAlgorithm == SignatureAlgorithm.ES384 || signatureAlgorithm == SignatureAlgorithm.ES512) {
                String dd = jsonPrivateKey.getString(D);

                BigInteger d = new BigInteger(1, JwtUtil.base64urldecode(dd));

                privateKey = new ECDSAPrivateKey(d);
            }

            if (privateKey != null) {
                privateKey.setSignatureAlgorithm(signatureAlgorithm);
                privateKey.setKeyId(resultKeyId);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return privateKey;
    }

}