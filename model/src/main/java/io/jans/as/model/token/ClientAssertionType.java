/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.token;

/**
 * @author Javier Rojas Blum Date: 04.13.2012
 */
public enum ClientAssertionType {

    JWT_BEARER("urn:ietf:params:oauth:client-assertion-type:jwt-bearer");

    private final String paramName;

    ClientAssertionType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link ClientAssertionType} for a parameter client_assertion_type.
     *
     * @param param The client_assertion_type parameter.
     * @return The corresponding token type if found, otherwise
     *         <code>null</code>.
     */
    public static ClientAssertionType fromString(String param) {
        if (param != null) {
            for (ClientAssertionType cat : ClientAssertionType.values()) {
                if (param.equals(cat.paramName)) {
                    return cat;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the client_assertionType parameter.
     *
     * @return The string representation of the object.
     */
    @Override
    public String toString() {
        return paramName;
    }
}