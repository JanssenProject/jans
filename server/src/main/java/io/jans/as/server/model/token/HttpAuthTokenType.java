/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.token;

public enum  HttpAuthTokenType {
    Basic("Basic "),
    Bearer("Bearer "),
    AccessToken("AccessToken "),
    Negotiate("Negotiate ");

    private final String prefix;

    HttpAuthTokenType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {

        return this.prefix;
    }
}