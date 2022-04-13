/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.authorize;

/**
 * RFC8628 section 3.2
 */
public class DeviceAuthorizationResponseParam {

    private DeviceAuthorizationResponseParam() {
    }

    /**
     * REQUIRED.  The device verification code.
     */
    public static final String DEVICE_CODE = "device_code";

    /**
     * REQUIRED.  The end-user verification code.
     */
    public static final String USER_CODE = "user_code";

    /**
     * REQUIRED.  The end-user verification URI on the authorization
     * server.  The URI should be short and easy to remember as end users
     * will be asked to manually type it into their user agent.
     */
    public static final String VERIFICATION_URI = "verification_uri";

    /**
     * OPTIONAL.  A verification URI that includes the "user_code" (or
     * other information with the same function as the "user_code"),
     * which is designed for non-textual transmission.
     */
    public static final String VERIFICATION_URI_COMPLETE = "verification_uri_complete";

    /**
     * REQUIRED.  The lifetime in seconds of the "device_code" and "user_code".
     */
    public static final String EXPIRES_IN = "expires_in";

    /**
     * OPTIONAL.  The minimum amount of time in seconds that the client
     * SHOULD wait between polling requests to the token endpoint.  If no
     * value is provided, clients MUST use 5 as the default.
     */
    public static final String INTERVAL = "interval";

}