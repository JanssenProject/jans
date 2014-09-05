/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.jwk.ws.rs;

import static org.xdi.oxauth.model.jwk.JWKParameter.ALGORITHM;
import static org.xdi.oxauth.model.jwk.JWKParameter.CURVE;
import static org.xdi.oxauth.model.jwk.JWKParameter.EXPONENT;
import static org.xdi.oxauth.model.jwk.JWKParameter.JSON_WEB_KEY_SET;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_ID;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_TYPE;
import static org.xdi.oxauth.model.jwk.JWKParameter.KEY_USE;
import static org.xdi.oxauth.model.jwk.JWKParameter.MODULUS;
import static org.xdi.oxauth.model.jwk.JWKParameter.X;
import static org.xdi.oxauth.model.jwk.JWKParameter.X5C;
import static org.xdi.oxauth.model.jwk.JWKParameter.Y;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.jwk.JSONWebKey;

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
            for (JSONWebKey JSONWebKey : ConfigurationFactory.getWebKeys().getKeys()) {
                JSONObject jsonKeyValue = new JSONObject();
                if (JSONWebKey.getKeyType() != null) {
                    jsonKeyValue.put(KEY_TYPE, JSONWebKey.getKeyType());
                }
                if (JSONWebKey.getKeyId() != null) {
                    jsonKeyValue.put(KEY_ID, JSONWebKey.getKeyId());
                }
                if (JSONWebKey.getUse() != null) {
                    jsonKeyValue.put(KEY_USE, JSONWebKey.getUse());
                }
                if (JSONWebKey.getAlgorithm() != null) {
                    jsonKeyValue.put(ALGORITHM, JSONWebKey.getAlgorithm());
                }
                if (JSONWebKey.getCurve() != null) {
                    jsonKeyValue.put(CURVE, JSONWebKey.getCurve());
                }
                if (JSONWebKey.getPublicKey() != null) {
                    if (JSONWebKey.getPublicKey().getModulus() != null) {
                        jsonKeyValue.put(MODULUS, JSONWebKey.getPublicKey().getModulus());
                    }
                    if (JSONWebKey.getPublicKey().getExponent() != null) {
                        jsonKeyValue.put(EXPONENT, JSONWebKey.getPublicKey().getExponent());
                    }
                    if (JSONWebKey.getPublicKey().getX() != null) {
                        jsonKeyValue.put(X, JSONWebKey.getPublicKey().getX());
                    }
                    if (JSONWebKey.getPublicKey().getY() != null) {
                        jsonKeyValue.put(Y, JSONWebKey.getPublicKey().getY());
                    }
                    if (JSONWebKey.getCertificateChain() != null) {
                        jsonKeyValue.put(X5C, new JSONArray(JSONWebKey.getCertificateChain()));
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