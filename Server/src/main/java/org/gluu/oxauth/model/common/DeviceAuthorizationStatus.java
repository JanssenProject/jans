/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.common;

/**
 * Contains a list of values of  status for OAuth2 Device Flow requests.
 */
public enum DeviceAuthorizationStatus {
    PENDING("pending"),
    DENIED("denied"),
    EXPIRED("expired");

    private final String value;

    DeviceAuthorizationStatus(String name) {
        value = name;
    }

    public String getValue() {
        return value;
    }

}
