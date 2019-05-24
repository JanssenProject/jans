/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jwk;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;

import org.gluu.oxauth.model.crypto.signature.AlgorithmFamily;
import org.gluu.oxauth.model.crypto.signature.SignatureAlgorithm;

import static org.gluu.oxauth.model.jwk.JWKParameter.JSON_WEB_KEY_SET;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class JSONWebKeySet {

    private static final Logger LOG = Logger.getLogger(JSONWebKeySet.class);

    private List<JSONWebKey> keys;

    public JSONWebKeySet() {
        keys = new ArrayList<JSONWebKey>();
    }

    public List<JSONWebKey> getKeys() {
        return keys;
    }

    public void setKeys(List<JSONWebKey> keys) {
        this.keys = keys;
    }

    public JSONWebKey getKey(String keyId) {
        for (JSONWebKey jsonWebKey : keys) {
            if (jsonWebKey.getKid().equals(keyId)) {
                return jsonWebKey;
            }
        }

        return null;
    }

    @Deprecated
    public List<JSONWebKey> getKeys(SignatureAlgorithm algorithm) {
        List<JSONWebKey> jsonWebKeys = new ArrayList<JSONWebKey>();

        if (AlgorithmFamily.RSA.equals(algorithm.getFamily())) {
            for (JSONWebKey jsonWebKey : keys) {
                if (jsonWebKey.getAlg().equals(algorithm.getName())) {
                    jsonWebKeys.add(jsonWebKey);
                }
            }
        } else if (AlgorithmFamily.EC.equals(algorithm.getFamily())) {
            for (JSONWebKey jsonWebKey : keys) {
                if (jsonWebKey.getAlg().equals(algorithm.getName())) {
                    jsonWebKeys.add(jsonWebKey);
                }
            }
        }

        Collections.sort(jsonWebKeys);
        return jsonWebKeys;
    }

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObj = new JSONObject();
        JSONArray keys = new JSONArray();

        for (JSONWebKey key : getKeys()) {
            JSONObject jsonKeyValue = key.toJSONObject();

            keys.put(jsonKeyValue);
        }

        jsonObj.put(JSON_WEB_KEY_SET, keys);
        return jsonObj;
    }

    @Override
    public String toString() {
        try {
            JSONObject jwks = toJSONObject();
            return toPrettyJson(jwks).replace("\\/", "/");
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } catch (JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
            return null;
		}
    }

	private String toPrettyJson(JSONObject jsonObject) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JsonOrgModule());
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
	}

    public static JSONWebKeySet fromJSONObject(JSONObject jwksJSONObject) throws JSONException {
        JSONWebKeySet jwks = new JSONWebKeySet();

        JSONArray jwksJsonArray = jwksJSONObject.getJSONArray(JSON_WEB_KEY_SET);
        for (int i = 0; i < jwksJsonArray.length(); i++) {
            JSONObject jwkJsonObject = jwksJsonArray.getJSONObject(i);

            JSONWebKey jwk = JSONWebKey.fromJSONObject(jwkJsonObject);
            jwks.getKeys().add(jwk);
        }

        return jwks;
    }
}