/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.userinfo;

/**
 * @author Javier Rojas Date: 11.28.2011
 */
public enum Schema {

    OPEN_ID("openid");

    private final String paramName;

    Schema(String paramName) {
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