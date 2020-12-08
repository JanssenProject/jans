/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

/**
 * @author Javier Rojas Blum
 * @version December 17, 2015
 */
public enum JwtType {

    JWT;

    /**
     * Returns the corresponding {@link JwtType} for a parameter.
     *
     * @param param The parameter.
     * @return The corresponding JWT Type if found, otherwise <code>null</code>.
     */
    public static JwtType fromString(String param) {
        if (param != null) {
            for (JwtType t : JwtType.values()) {
                if (param.equals(t.toString())) {
                    return t;
                }
            }
        }
        return null;
    }
}