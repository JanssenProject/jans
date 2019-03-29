/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.authorize;

import org.gluu.oxauth.model.error.IErrorType;

/**
 * Error codes for authorization error responses.
 *
 * @author Javier Rojas Blum Date: 09.22.2011
 */
public enum AuthorizeErrorResponseType implements IErrorType {

    /**
     * The request is missing a required parameter, includes an
     * invalid parameter value, includes a parameter more than
     * once, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),

    /**
     * The client is not authorized to request an authorization
     * code / access token using this method.
     */
    UNAUTHORIZED_CLIENT("unauthorized_client"),

    /**
     * The client is disabled and can't request an access token using this method.
     */
    DISABLED_CLIENT("disabled_client"),

    /**
     * The resource owner or authorization server denied the request.
     */
    ACCESS_DENIED("access_denied"),

    /**
     * The authorization server does not support obtaining an access token using
     * this method.
     */
    UNSUPPORTED_RESPONSE_TYPE("unsupported_response_type"),

    /**
     * The requested scope is invalid, unknown, or malformed.
     */
    INVALID_SCOPE("invalid_scope"),

    /**
     * The authorization server encountered an unexpected condition which
     * prevented it from fulfilling the request.
     */
    SERVER_ERROR("server_error"),

    /**
     * The authorization server is currently unable to handle the request due to
     * a temporary overloading or maintenance of the server.
     */
    TEMPORARILY_UNAVAILABLE("temporarily_unavailable"),

    /**
     * The redirect_uri in the Authorization Request does not match any of the
     * Client's pre-registered redirect_uris.
     */
    INVALID_REQUEST_REDIRECT_URI("invalid_request_redirect_uri"),

    /**
     * The Authorization Server requires End-User authentication. This error MAY
     * be returned when the prompt parameter in the Authorization Request is set
     * to none to request that the Authorization Server should not display any
     * user interfaces to the End-User, but the Authorization Request cannot be
     * completed without displaying a user interface for user authentication.
     */
    LOGIN_REQUIRED("login_required"),

    /**
     * The End-User is required to select a session at the Authorization Server.
     * The End-User MAY be authenticated at the Authorization Server with
     * different associated accounts, but the End-User did not select a session.
     * This error MAY be returned when the prompt parameter in the Authorization
     * Request is set to none to request that the Authorization Server should
     * not display any user interfaces to the End-User, but the Authorization
     * Request cannot be completed without displaying a user interface to
     * prompt for a session to use.
     */
    SESSION_SELECTION_REQUIRED("session_selection_required"),

    /**
     * The Authorization Server requires End-User consent. This error MAY be
     * returned when the prompt parameter in the Authorization Request is set to
     * none to request that the Authorization Server should not display any user
     * interfaces to the End-User, but the Authorization Request cannot be
     * completed without displaying a user interface for End-User consent.
     */
    CONSENT_REQUIRED("consent_required"),

    /**
     * The current logged in End-User at the Authorization Server does not match
     * the requested user. This error MAY be returned when the prompt parameter
     * in the Authorization Request is set to none to request that the Authorization
     * Server should not display any user interfaces to the End-User, but the
     * Authorization Request cannot be completed without displaying a user interface
     * to prompt for the correct End-User authentication.
     */
    USER_MISMATCHED("user_mismatched"),

    /**
     * The request_uri in the Authorization Request returns an error or invalid data.
     */
    INVALID_REQUEST_URI("invalid_request_uri"),

    /**
     * The request parameter contains an invalid OpenID Request Object.
     */
    INVALID_OPENID_REQUEST_OBJECT("invalid_openid_request_object"),

    /**
     * The authorization server can't handle user authentication due to session expiration
     */
    AUTHENTICATION_SESSION_INVALID("authentication_session_invalid"),

    /**
     * The authorization server can't handle user authentication due to error caused by ACR
     */
    INVALID_AUTHENTICATION_METHOD("invalid_authentication_method");

    private final String paramName;

    private AuthorizeErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Return the corresponding enumeration from a string parameter.
     *
     * @param param The parameter to be match.
     * @return The <code>enumeration</code> if found, otherwise
     *         <code>null</code>.
     */
    public static AuthorizeErrorResponseType fromString(String param) {
        if (param != null) {
            for (AuthorizeErrorResponseType err : AuthorizeErrorResponseType
                    .values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }


    /**
     * Returns a string representation of the object. In this case, the lower
     * case code of the error.
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