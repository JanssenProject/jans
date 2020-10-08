/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.gluu;

import java.util.HashMap;
import java.util.Map;

import org.gluu.oxauth.model.error.IErrorType;

public enum GluuErrorResponseType implements IErrorType {

    /**
     * The server encountered an unexpected condition which
     * prevented it from fulfilling the request.
     */
    SERVER_ERROR("server_error");


    private static Map<String, GluuErrorResponseType> lookup = new HashMap<String, GluuErrorResponseType>();

    static {
        for (GluuErrorResponseType enumType : values()) {
            lookup.put(enumType.getParameter(), enumType);
        }
    }

    private final String paramName;

    private GluuErrorResponseType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Return the corresponding enumeration from a string parameter.
     *
     * @param param The parameter to be match.
     * @return The <code>enumeration</code> if found, otherwise
     * <code>null</code>.
     */
    public static GluuErrorResponseType fromString(String param) {
        return lookup.get(param);
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