package org.gluu.oxauth.model.token;

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