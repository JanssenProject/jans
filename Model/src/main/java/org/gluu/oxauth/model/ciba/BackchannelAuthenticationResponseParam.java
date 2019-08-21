/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ciba;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public interface BackchannelAuthenticationResponseParam {

    /**
     * A unique identifier to identify the authentication request made by the Client.
     */
    String AUTH_REQ_ID = "auth_req_id";

    /**
     * The expiration time of the "auth_req_id" in seconds since the authentication request was received.
     */
    String EXPIRES_IN = "expires_in";

    /**
     * The minimum amount of time in seconds that the Client must wait between polling requests to the token endpoint.
     * This parameter will only be present if the Client is registered to use the Poll or Ping modes.
     */
    String INTERVAL = "interval";
}