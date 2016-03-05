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
 * @author Javier Rojas Blum
 * @version February 17, 2016
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
            for (JSONWebKey jsonWebKey : ConfigurationFactory.instance().getWebKeys().getKeys()) {
                JSONObject jsonKeyValue = new JSONObject();
                if (jsonWebKey.getKty() != null) {
                    jsonKeyValue.put(KEY_TYPE, jsonWebKey.getKty());
                }
                if (jsonWebKey.getKid() != null) {
                    jsonKeyValue.put(KEY_ID, jsonWebKey.getKid());
                }
                if (ConfigurationFactory.instance().getConfiguration().getKeyRegenerationEnabled()
                        && jsonWebKey.getExp() != null) {
                    jsonKeyValue.put(EXPIRATION_TIME, jsonWebKey.getExp());
                }
                if (jsonWebKey.getUse() != null) {
                    jsonKeyValue.put(KEY_USE, jsonWebKey.getUse());
                }
                if (jsonWebKey.getAlg() != null) {
                    jsonKeyValue.put(ALGORITHM, jsonWebKey.getAlg());
                }
                if (jsonWebKey.getCrv() != null) {
                    jsonKeyValue.put(CURVE, jsonWebKey.getCrv());
                }
                if (jsonWebKey.getPublicKey() != null) {
                    if (jsonWebKey.getPublicKey().getN() != null) {
                        jsonKeyValue.put(MODULUS, jsonWebKey.getPublicKey().getN());
                    }
                    if (jsonWebKey.getPublicKey().getE() != null) {
                        jsonKeyValue.put(EXPONENT, jsonWebKey.getPublicKey().getE());
                    }
                    if (jsonWebKey.getPublicKey().getX() != null) {
                        jsonKeyValue.put(X, jsonWebKey.getPublicKey().getX());
                    }
                    if (jsonWebKey.getPublicKey().getY() != null) {
                        jsonKeyValue.put(Y, jsonWebKey.getPublicKey().getY());
                    }
                    if (jsonWebKey.getX5c() != null) {
                        //jsonKeyValue.put(X5C, new JSONArray(jsonWebKey.getCertificateChain()));
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