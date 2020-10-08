/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.ciba;

import org.gluu.oxauth.model.error.IErrorType;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
public enum PushErrorResponseType implements IErrorType {

    /**
     * The end-user denied the authorization request.
     */
    ACCESS_DENIED("access_denied"),

    /**
     * The auth_req_id has expired. The Client will need to make a new Authentication Request.
     */
    EXPIRED_TOKEN("expired_token"),

    /**
     * The OpenID Provider encountered an unexpected condition that prevented it from successfully completing the
     * transaction. This general case error code can be used to inform the Client that the CIBA transaction was
     * unsuccessful for reasons other than those explicitly defined by access_denied and expired_token.
     */
    TRANSACTION_FAILED("transaction_failed");

    private final String paramName;

    PushErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link PushErrorResponseType} from a given string.
     *
     * @param param The string value to convert.
     * @return The corresponding {@link PushErrorResponseType}, otherwise <code>null</code>.
     */
    public static PushErrorResponseType fromString(String param) {
        if (param != null) {
            for (PushErrorResponseType err : PushErrorResponseType.values()) {
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
