/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

/**
 * Represents an OpenId Configuration request to send to the authorization server.
 *
 * @author Javier Rojas Blum Date: 12.6.2011
 */
public class OpenIdConfigurationRequest extends BaseRequest{

    @Override
    public String getQueryString() {
        return null;
    }
}