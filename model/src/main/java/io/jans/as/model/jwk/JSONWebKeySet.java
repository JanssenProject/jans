/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version February 12, 2019
 */
public class JSONWebKeySet {

    @JsonIgnore
    private static final Logger LOG = LoggerFactory.getLogger(JSONWebKeySet.class);

    private List<JSONWebKey> keys;

    public JSONWebKeySet() {
        keys = new ArrayList<>();
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

    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObj = new JSONObject();
        JSONArray keyArray = new JSONArray();

        for (JSONWebKey key : getKeys()) {
            JSONObject jsonKeyValue = key.toJSONObject();

            keyArray.put(jsonKeyValue);
        }

        jsonObj.put(JWKParameter.JSON_WEB_KEY_SET, keyArray);
        return jsonObj;
    }

    @Override
    public String toString() {
        try {
            JSONObject jwks = toJSONObject();
            return toPrettyJson(jwks).replace("\\/", "/");
        } catch (JSONException | JsonProcessingException e) {
            LOG.error(e.getMessage(), e);
            return "";
        }
    }

	private String toPrettyJson(JSONObject jsonObject) throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JsonOrgModule());
		return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonObject);
	}

    public static JSONWebKeySet fromJSONObject(JSONObject jwksJSONObject) throws JSONException {
        JSONWebKeySet jwks = new JSONWebKeySet();

        JSONArray jwksJsonArray = jwksJSONObject.getJSONArray(JWKParameter.JSON_WEB_KEY_SET);
        for (int i = 0; i < jwksJsonArray.length(); i++) {
            JSONObject jwkJsonObject = jwksJsonArray.getJSONObject(i);

            JSONWebKey jwk = JSONWebKey.fromJSONObject(jwkJsonObject);
            jwks.getKeys().add(jwk);
        }

        return jwks;
    }
}