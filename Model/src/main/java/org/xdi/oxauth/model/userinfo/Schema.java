/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.userinfo;

/**
 * @author Javier Rojas Date: 11.28.2011
 */
public enum Schema {

    OPEN_ID("openid");

    private final String paramName;

    private Schema(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link Schema} for a given parameter.
     *
     * @param param The schema parameter
     * @return The corresponding schema if found, otherwise <code>null</code>.
     */
    public static Schema fromString(String param) {
        if (param != null) {
            for (Schema s : Schema.values()) {
                if (param.equals(s.paramName)) {
                    return s;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     */
    @Override
    public String toString() {
        return paramName;
    }
}