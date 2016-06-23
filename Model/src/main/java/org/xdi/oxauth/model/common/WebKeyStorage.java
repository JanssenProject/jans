/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonValue;

/**
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public enum WebKeyStorage {
    KEYSTORE("keystore"),
    PKCS11("pkcs11");

    private final String value;

    private WebKeyStorage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @JsonCreator
    public static WebKeyStorage fromString(String string) {
        for (WebKeyStorage v : values()) {
            if (v.getValue().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return KEYSTORE;
    }

    /**
     * Returns a string representation of the object. In this case the parameter name.
     *
     * @return The string representation of the object.
     */
    @Override
    @JsonValue
    public String toString() {
        return value;
    }
}
