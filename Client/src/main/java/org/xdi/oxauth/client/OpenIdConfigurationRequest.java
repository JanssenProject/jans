/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

/**
 * Represents an OpenId Configuration request to send to the authorization server.
 *
 * @author Javier Rojas Blum Date: 12.6.2011
 */
public class OpenIdConfigurationRequest extends BaseRequest{

    /**
     * Construct an OpenID Configuration Request.
     */
    public OpenIdConfigurationRequest() {
    }

    @Override
    public String getQueryString() {
        return null;
    }
}