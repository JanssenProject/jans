/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
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

    private HttpAuthTokenType(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {

        return this.prefix;
    }
}