/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.clientinfo;

import org.xdi.oxauth.model.error.IErrorType;

/**
 * @author Javier Rojas Date: 07.19.2012
 */
public enum ClientInfoErrorResponseType implements IErrorType {

    INVALID_REQUEST("invalid_request"),
    INVALID_TOKEN("invalid_token");

    private final String paramName;

    private ClientInfoErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    public static ClientInfoErrorResponseType fromString(String param) {
        if (param != null) {
            for (ClientInfoErrorResponseType err : ClientInfoErrorResponseType.values()) {
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