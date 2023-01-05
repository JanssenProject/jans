/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

/**
 * @author Javier Rojas Blum
 * @version December 26, 2016
 */
public abstract class BaseResponse {

    protected int status;
    protected String location;
    protected String entity;
    protected MultivaluedMap<String, Object> headers;

    public BaseResponse() {
    }

    // TODO: remove
    @Deprecated
    public BaseResponse(int status) {
        this.status = status;
    }

    protected BaseResponse(Response clientResponse) {
        if (clientResponse != null) {
            status = clientResponse.getStatus();
            if (clientResponse.getLocation() != null) {
                location = clientResponse.getLocation().toString();
            }
            if (clientResponse.getEntity() != null) {
                entity = clientResponse.readEntity(String.class);
            }
            headers = clientResponse.getMetadata();
        }
    }

    /**
     * Returns the HTTP status code of the response.
     *
     * @return The HTTP status code.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Returns the location of the response in the header.
     *
     * @return The location of the response.
     */
    public String getLocation() {
        return location;
    }

    /**
     * Sets the location of the response in the header.
     *
     * @param location The location of the response.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Sets the HTTP status code of the response.
     *
     * @param status The HTTP status code.
     */
    public void setStatus(int status) {
        this.status = status;
    }

    /**
     * Returns the entity or body content of the response.
     *
     * @return The entity or body content of the response.
     */
    public String getEntity() {
        return entity;
    }

    /**
     * Sets the entity or body content of the response.
     *
     * @param entity The entity or body content of the response.
     */
    public void setEntity(String entity) {
        this.entity = entity;
    }

    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    public void setHeaders(MultivaluedMap<String, Object> headers) {
        this.headers = headers;
    }
}