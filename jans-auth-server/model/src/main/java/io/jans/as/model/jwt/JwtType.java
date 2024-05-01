/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwt;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
public enum JwtType {

    JWT("JWT"),
    TX_TOKEN("txn_token"),
    DPOP_PLUS_JWT("dpop+jwt");

    private final String paramName;

    JwtType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link JwtType} for a parameter.
     *
     * @param param The parameter.
     * @return The corresponding JWT Type if found, otherwise <code>null</code>.
     */
    public static JwtType fromString(String param) {
        if (param != null) {
            for (JwtType t : JwtType.values()) {
                if (param.equalsIgnoreCase(t.paramName)) {
                    return t;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return paramName;
    }
}
