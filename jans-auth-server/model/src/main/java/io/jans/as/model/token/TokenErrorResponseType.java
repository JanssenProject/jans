/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.token;

import io.jans.as.model.error.IErrorType;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
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
    INVALID_SCOPE("invalid_scope"),

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
     * Indicates invalid requested token type.
     *
     * For transaction token value must be: urn:ietf:params:oauth:token-type:txn_token
     */
    INVALID_REQUESTED_TOKEN_TYPE("invalid_requested_token_type"),

    /**
     * Indicates invalid subject token type.
     *
     * For transaction token value must be: urn:ietf:params:oauth:token-type:id_token or
     * urn:ietf:params:oauth:token-type:access_token.
     */
    INVALID_SUBJECT_TOKEN_TYPE("invalid_subject_token_type"),

    /**
     * CIBA. The authorization request is still pending as the end-user hasn't yet been authenticated.
     */
    AUTHORIZATION_PENDING("authorization_pending"),

    /**
     * CIBA. A variant of "authorization_pending", the authorization request is still pending and
     * polling should continue, but the interval MUST be increased by at least 5 seconds
     * for this and all subsequent requests.
     */
    SLOW_DOWN("slow_down"),

    /**
     * CIBA. The auth_req_id has expired. The Client will need to make a new Authentication Request.
     */
    EXPIRED_TOKEN("expired_token"),

    /**
     * CIBA. The end-user denied the authorization request.
     */
    ACCESS_DENIED("access_denied"),

    /**
     * Use DPoP nonce. Returned when nonce claim is not in DPoP jwt.
     */
    USE_DPOP_NONCE("use_dpop_nonce"),

    /**
     * Use new DPoP nonce. Returned when new nonce claim is required in DPoP jwt.
     */
    USE_NEW_DPOP_NONCE("use_new_dpop_nonce"),

    /**
     * DPoP. If the DPoP proof is invalid, the authorization server issues an error response with "invalid_dpop_proof"
     * as the value of the "error" parameter.
     */
    INVALID_DPOP_PROOF("invalid_dpop_proof");

    private final String paramName;

    TokenErrorResponseType(String paramName) {
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
