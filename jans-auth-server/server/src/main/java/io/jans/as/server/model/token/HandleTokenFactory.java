/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

import java.util.UUID;

/**
 * Handle (or artifact) a reference to some internal data structure within the
 * authorization server, the internal data structure contains the attributes of
 * the token, such as user id, scope, etc. Handles typically require a
 * communication between resource server and token server in order to validate
 * the token and obtain token- bound data. Handles enable simple revocation and
 * do not require cryptographic mechanisms to protected token content from being
 * modified. As a disadvantage, they require additional resource/ token server
 * communication impacting on performance and scalability. An authorization code
 * is an example of a 'handle' token. An access token may also be implemented as
 * a handle token. A 'handle' token is often referred to as an 'opaque' token
 * because the resource server does not need to be able to interpret the token
 * directly, it simply uses the token.
 *
 * @author Javier Rojas Date: 10.31.2011
 */
public class HandleTokenFactory {

    /**
     * When creating token handles, the authorization server MUST include a
     * reasonable level of entropy in order to mitigate the risk of guessing
     * attacks. The token value MUST be constructed from a cryptographically
     * strong random or pseudo-random number sequence [RFC1750] generated by the
     * Authorization Server. The probability of any two Authorization Code
     * values being identical MUST be less than or equal to 2^(-128) and SHOULD
     * be less than or equal to 2^(-160).
     *
     * @return The generated handle token.
     */
    public static String generateHandleToken() {
        return UUID.randomUUID().toString();
    }

    public static String generateDeviceSecret() {
        return UUID.randomUUID().toString();
    }
}