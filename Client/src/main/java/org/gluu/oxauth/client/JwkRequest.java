/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client;

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
