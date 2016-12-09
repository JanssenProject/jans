/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import javax.ws.rs.core.MultivaluedMap;

import org.jboss.resteasy.client.ClientResponse;

/**
 * @author Javier Rojas Blum Date: 01.31.2012
 */
public abstract class BaseResponse {

    protected int status;
    protected String location;
    protected String entity;
    protected MultivaluedMap<String, String> headers;

    public BaseResponse() {
    }

    // TODO: remove
    @Deprecated
    public BaseResponse(int status) {
        this.status = status;
    }

    public BaseResponse(ClientResponse<String> clientResponse) {
        if (clientResponse != null) {
            status = clientResponse.getStatus();
            if (clientResponse.getLocationLink() != null) {
                location = clientResponse.getLocationLink().getHref();
            }
            entity = clientResponse.getEntity(String.class);
            headers = clientResponse.getResponseHeaders();
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

    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(MultivaluedMap<String, String> headers) {
        this.headers = headers;
    }
}