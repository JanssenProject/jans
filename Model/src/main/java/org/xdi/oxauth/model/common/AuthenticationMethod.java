/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

/**
 * @author Javier Rojas Blum Date: 03.23.2012
 */
public enum AuthenticationMethod {

    /**
     * Clients in possession of a client password authenticate with the Authorization Server
     * using HTTP Basic authentication scheme. Default one if not client authentication is specified.
     */
    CLIENT_SECRET_BASIC("client_secret_basic"),

    /**
     * Clients in possession of a client password authenticate with the Authorization Server
     * by including the client credentials in the request body.
     */
    CLIENT_SECRET_POST("client_secret_post"),

    /**
     * Clients in possession of a client password create a JWT using the HMAC-SHA algorithm.
     * The HMAC (Hash-based Message Authentication Code) is calculated using the client_secret
     * as the shared key.
     */
    CLIENT_SECRET_JWT("client_secret_jwt"),

    /**
     * Clients that have registered a public key sign a JWT using the RSA algorithm if a RSA
     * key was registered or the ECDSA algorithm if an Elliptic Curve key was registered.
     */
    PRIVATE_KEY_JWT("private_key_jwt"),

    /**
     * Authenticates client by access token.
     */
    ACCESS_TOKEN("access_token"),

    /**
     * The Client does not authenticate itself at the Token Endpoint, either because it uses only the Implicit Flow
     * (and so does not use the Token Endpoint) or because it is a Public Client with no Client Secret or other
     * authentication mechanism.
     */
    NONE("none");

    private final String paramName;

    private AuthenticationMethod(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link AuthenticationMethod} for an authentication method parameter.
     *
     * @param param The parameter.
     * @return The corresponding authentication method if found, otherwise
     *         <code>null</code>.
     */
    public static AuthenticationMethod fromString(String param) {
        if (param != null) {
            for (AuthenticationMethod rt : AuthenticationMethod.values()) {
                if (param.equals(rt.paramName)) {
                    return rt;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the authentication method parameter.
     */
    @Override
    public String toString() {
        return paramName;
    }
}