/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class BackchannelAuthenticationResponseParam {

    private BackchannelAuthenticationResponseParam() {
    }

    /**
     * A unique identifier to identify the authentication request made by the Client.
     */
    public static final String AUTH_REQ_ID = "auth_req_id";

    /**
     * The expiration time of the "auth_req_id" in seconds since the authentication request was received.
     */
    public static final String EXPIRES_IN = "expires_in";

    /**
     * The minimum amount of time in seconds that the Client must wait between polling requests to the token endpoint.
     * This parameter will only be present if the Client is registered to use the Poll or Ping modes.
     */
    public static final String INTERVAL = "interval";
}