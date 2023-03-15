/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;

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
     * Indicates that client authentication to the authorization server
     * will occur with mutual TLS utilizing the PKI method of associating
     * a certificate to a client.
     */
    TLS_CLIENT_AUTH("tls_client_auth"),

    /**
     * Indicates that client authentication to the authorization server
     * will occur using mutual TLS with the client utilizing a self-
     * signed certificate.
     */
    SELF_SIGNED_TLS_CLIENT_AUTH("self_signed_tls_client_auth"),

    /**
     * The Client does not authenticate itself at the Token Endpoint, either because it uses only the Implicit Flow
     * (and so does not use the Token Endpoint) or because it is a Public Client with no Client Secret or other
     * authentication mechanism.
     */
    NONE("none");

    private final String paramName;

    AuthenticationMethod(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link AuthenticationMethod} for an authentication method parameter.
     *
     * @param param The parameter.
     * @return The corresponding authentication method if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
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

    public static List<AuthenticationMethod> fromList(List<String> list) {
        List<AuthenticationMethod> result = new ArrayList<>();
        for (String s : list) {
            final AuthenticationMethod authenticationMethod = fromString(s);
            if (authenticationMethod != null) {
                result.add(authenticationMethod);
            }
        }
        return result;
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the authentication method parameter.
     */
    @Override
    @JsonValue
    public String toString() {
        return paramName;
    }
}