/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * @author Javier Rojas Blum
 * @version June 15, 2016
 */
public enum WebKeyStorage {
    KEYSTORE("keystore"),
    PKCS11("pkcs11");

    private final String value;

    WebKeyStorage(String value) {
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
