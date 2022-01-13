/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.register;

/**
 * Listed all standard parameters involved in client registration response.
 *
 * @author Javier Rojas Blum
 * @version 0.9, 03/23/2013
 */
public enum RegisterResponseParam {

    /**
     * Unique Client identifier.
     */
    CLIENT_ID("client_id"),

    /**
     * Client secret.
     */
    CLIENT_SECRET("client_secret"),

    /**
     * Access Token that is used by the Client to perform subsequent operations upon the resulting
     * Client registration.
     */
    REGISTRATION_ACCESS_TOKEN("registration_access_token"),

    /**
     * Location where the Access Token can be used to perform subsequent operations upon the resulting
     * Client registration.
     */
    REGISTRATION_CLIENT_URI("registration_client_uri"),

    /**
     * Time when the Client Identifier was issued.
     */
    CLIENT_ID_ISSUED_AT("client_id_issued_at"),

    /**
     * Time at which the client_secret will expire or 0 if it will not expire.
     */
    CLIENT_SECRET_EXPIRES_AT("client_secret_expires_at");

    /**
     * Parameter name
     */
    private final String name;

    /**
     * Constructor
     *
     * @param name parameter name
     */
    RegisterResponseParam(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}