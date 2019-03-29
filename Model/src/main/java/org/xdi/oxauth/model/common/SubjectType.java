/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

/**
 * @author Javier Rojas Blum Date: 05.11.2012
 */
public enum SubjectType {

    PAIRWISE("pairwise"),
    PUBLIC("public");

    private final String paramName;

    private SubjectType(String paramName) {
        this.paramName = paramName;
    }

    /**
     * Returns the corresponding {@link SubjectType} for an user id type parameter.
     *
     * @param param The parameter.
     * @return The corresponding user id type if found, otherwise
     *         <code>null</code>.
     */
    public static SubjectType fromString(String param) {
        if (param != null) {
            for (SubjectType uit : SubjectType.values()) {
                if (param.equals(uit.paramName)) {
                    return uit;
                }
            }
        }
        return null;
    }

    /**
     * Returns a string representation of the object. In this case the parameter
     * name for the user id type parameter.
     */
    @Override
    public String toString() {
        return paramName;
    }
}