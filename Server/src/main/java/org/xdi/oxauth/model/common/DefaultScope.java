/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

/**
 * @author Javier Rojas Date: 11.30.2011
 */
public enum DefaultScope {
    OPEN_ID("openid"),
    PROFILE("profile"),
    EMAIL("email"),
    ADDRESS("address"),
    PHONE("phone");

    private final String paramName;

    private DefaultScope(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link DefaultScope} for a default scope parameter.
     *
     * @param param The default scope parameter.
     * @return The corresponding scope if found, otherwise <code>null</code>.
     */
    public static DefaultScope fromString(String param) {
        if (param != null) {
            for (DefaultScope ds : DefaultScope.values()) {
                if (param.equals(ds.paramName)) {
                    return ds;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name for the default scope.
     */
    @Override
    public String toString() {
        return paramName;
    }
}
