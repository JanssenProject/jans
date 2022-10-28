/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import io.jans.as.model.common.HasParamName;

/**
 * An authorization grant is a credential representing the resource owner's
 * authorization (to access its protected resources) used by the client to
 * obtain an access token. This specification defines four grant types:
 * authorization code, implicit, resource owner password credentials, and client
 * credentials.
 *
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public enum AuthorizationGrantType implements HasParamName {

    /**
     * The authorization code is obtained by using an authorization server as an
     * intermediary between the client and resource owner. Instead of requesting
     * authorization directly from the resource owner, the client directs the
     * resource owner to an authorization server (via its user- agent as defined
     * in [RFC2616]), which in turn directs the resource owner back to the
     * client with the authorization code.
     */
    AUTHORIZATION_CODE("authorization_code"),
    /**
     * The implicit grant is a simplified authorization code flow optimized for
     * clients implemented in a browser using a scripting language such as
     * JavaScript. In the implicit flow, instead of issuing the client an
     * authorization code, the client is issued an access token directly (as the
     * result of the resource owner authorization). The grant type is implicit
     * as no intermediate credentials (such as an authorization code) are issued
     * (and later used to obtain an access token).
     */
    IMPLICIT("implicit"),
    /**
     * The client credentials (or other forms of client authentication) can be
     * used as an authorization grant when the authorization scope is limited to
     * the protected resources under the control of the client, or to protected
     * resources previously arranged with the authorization server. Client
     * credentials are used as an authorization grant typically when the client
     * is acting on its own behalf (the client is also the resource owner), or
     * is requesting access to protected resources based on an authorization
     * previously arranged with the authorization server.
     */
    CLIENT_CREDENTIALS("client_credentials"),
    /**
     * The resource owner password credentials (i.e. username and password) can
     * be used directly as an authorization grant to obtain an access token. The
     * credentials should only be used when there is a high degree of trust
     * between the resource owner and the client (e.g. its device operating
     * system or a highly privileged application), and when other authorization
     * grant types are not available (such as an authorization code).
     */
    RESOURCE_OWNER_PASSWORD_CREDENTIALS("resource_owner_password_credentials"),

    TOKEN_EXCHANGE("urn:ietf:params:oauth:grant-type:token-exchange"),
    /**
     * An extension grant for Client Initiated Backchannel Authentication.
     */
    CIBA("urn:openid:params:grant-type:ciba"),

    /**
     * Device Authorization Grant Type for OAuth 2.0
     */
    DEVICE_CODE("urn:ietf:params:oauth:grant-type:device_code"),
    ;

    private final String paramName;

    AuthorizationGrantType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link AuthorizationGrantType} for a given parameter.
     *
     * @param param The parameter.
     * @return The corresponding authorization grant type if found, otherwise
     * <code>null</code>.
     */
    @JsonCreator
    public static AuthorizationGrantType fromString(String param) {
        if (param != null) {
            for (AuthorizationGrantType agt : AuthorizationGrantType.values()) {
                if (param.equals(agt.paramName)) {
                    return agt;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the authorization grant type parameter.
     */
    @Override
    @JsonValue
    public String toString() {
        return paramName;
    }

    @Override
    public String getParamName() {
        return paramName;
    }
}