/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.clientinfo;

import io.jans.as.model.error.IErrorType;

/**
 * @author Javier Rojas Date: 07.19.2012
 */
public enum ClientInfoErrorResponseType implements IErrorType {

    INVALID_REQUEST("invalid_request"),
    INVALID_TOKEN("invalid_token");

    private final String paramName;

    ClientInfoErrorResponseType(String paramName) {
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