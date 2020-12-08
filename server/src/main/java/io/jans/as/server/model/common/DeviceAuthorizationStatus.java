/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.common;

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
