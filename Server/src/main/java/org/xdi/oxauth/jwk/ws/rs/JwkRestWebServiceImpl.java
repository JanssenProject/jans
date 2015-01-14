/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.jwk.ws.rs;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.jwk.JSONWebKey;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import static org.xdi.oxauth.model.jwk.JWKParameter.*;

/**
 * Provides interface for JWK REST web services
 *
 * @author Javier Rojas Blum Date: 11.15.2011
 */
@Name("requestJwkRestWebService")
public class JwkRestWebServiceImpl implements JwkRestWebService {

    @Logger
    private Log log;

    @Override
    public Response requestJwk(SecurityContext sec) {
        log.debug("Attempting to request JWK, Is Secure = {0}", sec.isSecure());
        Response.ResponseBuilder builder = Response.ok();

        builder.entity(getJSonResponse());
        return builder.build();
    }

    /**
     * Builds a JSon String with the response parameters.
     */
    public String getJSonResponse() {
        JSONObject jsonObj = new JSONObject();
        JSONArray keys = new JSONArray();
        try {
            for (JSONWebKey jsonWebKey : ConfigurationFactory.getWebKeys().getKeys()) {
                JSONObject jsonKeyValue = new JSONObject();
                if (jsonWebKey.getKeyType() != null) {
                    jsonKeyValue.put(KEY_TYPE, jsonWebKey.getKeyType());
                }
                if (jsonWebKey.getKeyId() != null) {
                    jsonKeyValue.put(KEY_ID, jsonWebKey.getKeyId());
                }
                if (jsonWebKey.getExpirationTime() != null) {
                    jsonKeyValue.put(EXPIRATION_TIME, jsonWebKey.getExpirationTime());
                }
                if (jsonWebKey.getUse() != null) {
                    jsonKeyValue.put(KEY_USE, jsonWebKey.getUse());
                }
                if (jsonWebKey.getAlgorithm() != null) {
                    jsonKeyValue.put(ALGORITHM, jsonWebKey.getAlgorithm());
                }
                if (jsonWebKey.getCurve() != null) {
                    jsonKeyValue.put(CURVE, jsonWebKey.getCurve());
                }
                if (jsonWebKey.getPublicKey() != null) {
                    if (jsonWebKey.getPublicKey().getModulus() != null) {
                        jsonKeyValue.put(MODULUS, jsonWebKey.getPublicKey().getModulus());
                    }
                    if (jsonWebKey.getPublicKey().getExponent() != null) {
                        jsonKeyValue.put(EXPONENT, jsonWebKey.getPublicKey().getExponent());
                    }
                    if (jsonWebKey.getPublicKey().getX() != null) {
                        jsonKeyValue.put(X, jsonWebKey.getPublicKey().getX());
                    }
                    if (jsonWebKey.getPublicKey().getY() != null) {
                        jsonKeyValue.put(Y, jsonWebKey.getPublicKey().getY());
                    }
                    if (jsonWebKey.getCertificateChain() != null) {
                        jsonKeyValue.put(X5C, new JSONArray(jsonWebKey.getCertificateChain()));
                    }

                    keys.put(jsonKeyValue);
                }
            }

            jsonObj.put(JSON_WEB_KEY_SET, keys);
            return jsonObj.toString(4);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}