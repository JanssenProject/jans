/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.authorize;

import io.jans.as.model.error.IErrorType;

/**
 * Error codes for authorization error responses.
 *
 * @author Javier Rojas Blum
 */
public enum AuthorizeErrorResponseType implements IErrorType {

    /**
     * The Authorization Server is unable to meet the requirements of the Relying Party for
     * the authentication of the End-User. OP is unable to use acr specified in request.
     */
    UNMET_AUTHENTICATION_REQUIREMENTS("unmet_authentication_requirements"),

    /**
     * "request" parameter is supported by AS. But if it's switched off in configuration by setting
     * requestParameterSupported=false then this error is returned from authorization endpoint.
     */
    REQUEST_NOT_SUPPORTED("request_not_supported"),

    /**
     * "request_uri" parameter is supported by AS. But if it's switched off in configuration by setting
     * requestUriParameterSupported=false then this error is returned from authorization endpoint.
     */
    REQUEST_URI_NOT_SUPPORTED("request_uri_not_supported"),

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
     * AS requires RP to re-send authorization request.
     */
    RETRY("retry"),

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
     * invalid_authorization_details is returned to the client if any of the
     * following are true of the objects in the authorization_details structure:
     *
     * - contains an unknown authorization details type value,
     * - is an object of known type but containing unknown fields,
     * - contains fields of the wrong type for the authorization details type,
     * - contains fields with invalid values for the authorization details type, or
     * - is missing required fields for the authorization details type.
     */
    INVALID_AUTHORIZATION_DETAILS("invalid_authorization_details "),

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
    INVALID_REQUEST_OBJECT("invalid_request_object"),

    /**
     * The authorization server can't handle user authentication due to session expiration
     */
    AUTHENTICATION_SESSION_INVALID("authentication_session_invalid"),

    /**
     * The authorization server can't handle user authentication due to error caused by ACR
     */
    INVALID_AUTHENTICATION_METHOD("invalid_authentication_method");

    private final String paramName;

    AuthorizeErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Return the corresponding enumeration from a string parameter.
     *
     * @param param The parameter to be match.
     * @return The <code>enumeration</code> if found, otherwise
     * <code>null</code>.
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