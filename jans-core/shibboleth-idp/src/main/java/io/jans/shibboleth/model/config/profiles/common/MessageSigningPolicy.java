package io.jans.shibboleth.model.config.profiles.common;

public enum MessageSigningPolicy {

    SIGN_BOTH("Sign both requests and responses"),
    SIGN_RESPONSES_ONLY("Sign responses only"),
    SIGN_REQUESTS_ONLY("Sign requests only"),
    SIGN_NONE("Do not sign requests or responses");

    private final String description;

    MessageSigningPolicy(String description) {

        this.description = description;
    }

    public boolean shouldSignRequests() {

        return this == SIGN_BOTH || this == SIGN_REQUESTS_ONLY;
    }

    public boolean shouldSignResponses() {

        return this == SIGN_BOTH || this == SIGN_RESPONSES_ONLY;
    }
}