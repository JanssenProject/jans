/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import jakarta.ws.rs.core.MultivaluedMap;

import jakarta.ws.rs.core.Response;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public abstract class BaseResponse {

    protected int status;
    protected String location;
    protected String entity;
    protected MultivaluedMap<String, Object> headers;

    public BaseResponse(Response clientResponse) {
        if (clientResponse != null) {
            status = clientResponse.getStatus();
            entity = clientResponse.readEntity(String.class);
            headers = clientResponse.getHeaders();
            if (clientResponse.getLocation() != null) {
                location = clientResponse.getLocation().toString();
            }
        }
    }

    public int getStatus() {
        return status;
    }

    public String getLocation() {
        return location;
    }

    public String getEntity() {
        return entity;
    }

    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    public JSONObject getJSONEntity() {
        if ((entity != null) && (entity.length() > 0)) {
            try {
                JSONObject jsonObject = new JSONObject(entity);
                return jsonObject;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
