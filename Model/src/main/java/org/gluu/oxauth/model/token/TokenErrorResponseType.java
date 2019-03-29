/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.token;

import org.gluu.oxauth.model.error.IErrorType;

/**
 * @author Javier Rojas Date: 09.22.2011
 */
public enum TokenErrorResponseType implements IErrorType {
    /**
     * The request is missing a required parameter, includes an unsupported
     * parameter or parameter value, repeats a parameter, includes multiple
     * credentials, utilizes more than one mechanism for authenticating the
     * client, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),
    /**
     * Client authentication failed (e.g. unknown client, no client
     * authentication included, or unsupported authentication method). The
     * authorization server MAY return an HTTP 401 (Unauthorized) status code to
     * indicate which HTTP authentication schemes are supported. If the client
     * attempted to authenticate via the Authorization request header field, the
     * authorization server MUST respond with an HTTP 401 (Unauthorized) status
     * code, and include the WWW-Authenticate response header field matching the
     * authentication scheme used by the client.
     */
    INVALID_CLIENT("invalid_client"),

    /**
     * The client is disabled and can't request an access token using this method.
     */
    DISABLED_CLIENT("disabled_client"),

    /**
     * The provided authorization grant is invalid, expired, revoked, does not
     * match the redirection URI used in the authorization request, or was
     * issued to another client.
     */
    INVALID_GRANT("invalid_grant"),
    /**
     * The authenticated client is not authorized to use this authorization
     * grant type.
     */
    UNAUTHORIZED_CLIENT("unauthorized_client"),
    /**
     * The authorization grant type is not supported by the authorization
     * server.
     */
    UNSUPPORTED_GRANT_TYPE("unsupported_grant_type"),
    /**
     * The requested scope is invalid, unknown, malformed, or exceeds the scope
     * granted by the resource owner.
     */
    INVALID_SCOPE("invalid_scope");

    private final String paramName;

    private TokenErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link TokenErrorResponseType} from a given string.
     *
     * @param param The string value to convert.
     * @return The corresponding {@link TokenErrorResponseType}, otherwise <code>null</code>.
     */
    public static TokenErrorResponseType fromString(String param) {
        if (param != null) {
            for (TokenErrorResponseType err : TokenErrorResponseType.values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     *
     * @return The string representation of the object.
     */
    @Override
    public String toString() {
        return paramName;
    }

    /**
     * Gets error parameter.
     *
     * @return error parameter
     */
    @Override
    public String getParameter() {
        return paramName;
    }
}
