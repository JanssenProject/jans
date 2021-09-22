/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.jwk;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public enum Use {

    /**
     * Use this constant when the key is being used for signature.
     */
    SIGNATURE("sig"),
    /**
     * Use this constant when the key is being used for encryption.
     */
    ENCRYPTION("enc");

    private final String paramName;

    Use(String paramName) {
        this.paramName = paramName;
    }

    public String getParamName() {
        return paramName;
    }

    /**
     * Returns the corresponding {@link Use} for a parameter use of the JWK endpoint.
     *
     * @param param The use parameter.
     * @return The corresponding use if found, otherwise <code>null</code>.
     */
    @JsonCreator
    public static Use fromString(String param) {
        if (param != null) {
            for (Use use : Use.values()) {
                if (param.equals(use.paramName) || param.equalsIgnoreCase(use.name())) {
                    return use;
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
    @JsonValue
    public String toString() {
        return paramName;
    }
}