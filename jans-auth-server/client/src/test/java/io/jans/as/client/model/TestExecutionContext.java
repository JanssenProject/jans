package io.jans.as.client.model;

import io.jans.as.model.common.TokenTypeHint;

/**
 * @author Yuriy Z
 */
public class TestExecutionContext {

    private String clientId;
    private String clientSecret;
    private String token;
    private TokenTypeHint tokenTypeHint;

    public String getClientId() {
        return clientId;
    }

    public TestExecutionContext setClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public TestExecutionContext setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public String getToken() {
        return token;
    }

    public TestExecutionContext setToken(String token) {
        this.token = token;
        return this;
    }

    public TokenTypeHint getTokenTypeHint() {
        return tokenTypeHint;
    }

    public TestExecutionContext setTokenTypeHint(TokenTypeHint tokenTypeHint) {
        this.tokenTypeHint = tokenTypeHint;
        return this;
    }
}
