/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.token;

import org.xdi.oxauth.model.error.IErrorType;

/**
 * @author Javier Rojas Blum Date: 10.27.2011
 */
public enum ValidateTokenErrorResponseType implements IErrorType {
    /**
     * The request is missing a required parameter, includes an unsupported
     * parameter or parameter value, repeats a parameter, includes multiple
     * credentials, utilizes more than one mechanism for authenticating the
     * client, or is otherwise malformed.
     */
    INVALID_REQUEST("invalid_request"),
    /**
     * The provided access token is invalid, or was issued to another client.
     */
    INVALID_GRANT("invalid_grant");

    private final String paramName;

    private ValidateTokenErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    public static ValidateTokenErrorResponseType fromString(String param) {
        if (param != null) {
            for (ValidateTokenErrorResponseType err : ValidateTokenErrorResponseType
                    .values()) {
                if (param.equals(err.paramName)) {
                    return err;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return paramName;
    }


    @Override
    public String getParameter() {
        return paramName;
    }
}
