/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.jwk;

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

    private Use(String paramName) {
        this.paramName = paramName;
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
                if (param.equals(use.paramName)) {
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