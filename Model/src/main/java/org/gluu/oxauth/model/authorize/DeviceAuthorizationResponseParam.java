/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

/**
 * RFC8628 section 3.2
 */
public interface DeviceAuthorizationResponseParam {

    /**
     * REQUIRED.  The device verification code.
     */
    String DEVICE_CODE = "device_code";

    /**
     * REQUIRED.  The end-user verification code.
     */
    String USER_CODE = "user_code";

    /**
     * REQUIRED.  The end-user verification URI on the authorization
     * server.  The URI should be short and easy to remember as end users
     * will be asked to manually type it into their user agent.
     */
    String VERIFICATION_URI = "verification_uri";

    /**
     * OPTIONAL.  A verification URI that includes the "user_code" (or
     * other information with the same function as the "user_code"),
     * which is designed for non-textual transmission.
     */
    String VERIFICATION_URI_COMPLETE = "verification_uri_complete";

    /**
     * REQUIRED.  The lifetime in seconds of the "device_code" and "user_code".
     */
    String EXPIRES_IN = "expires_in";

    /**
     * OPTIONAL.  The minimum amount of time in seconds that the client
     * SHOULD wait between polling requests to the token endpoint.  If no
     * value is provided, clients MUST use 5 as the default.
     */
    String INTERVAL = "interval";

}