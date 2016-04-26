/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.model.common;

/**
 * @author Javier Rojas Blum
 * @version March 19, 2016
 */
public enum WebKeyStorage {
    LDAP("ldap"),
    PKCS11("pkcs11");

    private final String value;

    private WebKeyStorage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static WebKeyStorage fromString(String string) {
        for (WebKeyStorage v : values()) {
            if (v.getValue().equalsIgnoreCase(string)) {
                return v;
            }
        }
        return null;
    }
}
