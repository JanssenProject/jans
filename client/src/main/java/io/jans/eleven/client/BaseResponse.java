/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.client.ClientResponse;
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

    public BaseResponse(ClientResponse<String> clientResponse) {
        if (clientResponse != null) {
            status = clientResponse.getStatus();
            entity = clientResponse.getEntity(String.class);
            headers = clientResponse.getHeaders();
            if (clientResponse.getLocationLink() != null) {
                location = clientResponse.getLocationLink().getHref();
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
        if (entity != null) {
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
