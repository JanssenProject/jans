/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

/**
 * Represents a JSON Web Key (JWK) request to send to the authorization server.
 *
 * @author Javier Rojas Blum Date: 11.15.2011
 */
public class JwkRequest extends BaseRequest {

    @Override
    public String getQueryString() {
        return null;
    }
}
