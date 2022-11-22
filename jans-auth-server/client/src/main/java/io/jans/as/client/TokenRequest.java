/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.jwt.DPoP;
import io.jans.as.model.token.ClientAssertionType;
import io.jans.as.model.uma.UmaScopeType;
import io.jans.as.model.util.QueryBuilder;

import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a token request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version April 25, 2022
 */
public class TokenRequest extends ClientAuthnRequest {

    private GrantType grantType;
    private String code;
    private String redirectUri;
    private String username;
    private String password;
    private String scope;
    private String assertion;
    private String refreshToken;
    private String codeVerifier;
    private String authReqId;
    private String deviceCode;
    private String subjectToken;
    private String subjectTokenType;
    private String actorToken;
    private String actorTokenType;
    private String requestedTokenType;
    private DPoP dpop;

    /**
     * Constructs a token request.
     *
     * @param grantType The grant type is mandatory and could be:
     *                  <code>authorization_code</code>, <code>password</code>,
     *                  <code>client_credentials</code>, <code>refresh_token</code>.
     */
    public TokenRequest(GrantType grantType) {
        super();
        this.grantType = grantType;

        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder umaBuilder() {
        return new Builder().grantType(GrantType.CLIENT_CREDENTIALS);
    }

    public String getSubjectToken() {
        return subjectToken;
    }

    public void setSubjectToken(String subjectToken) {
        this.subjectToken = subjectToken;
    }

    public String getSubjectTokenType() {
        return subjectTokenType;
    }

    public void setSubjectTokenType(String subjectTokenType) {
        this.subjectTokenType = subjectTokenType;
    }

    public String getActorToken() {
        return actorToken;
    }

    public void setActorToken(String actorToken) {
        this.actorToken = actorToken;
    }

    public String getActorTokenType() {
        return actorTokenType;
    }

    public void setActorTokenType(String actorTokenType) {
        this.actorTokenType = actorTokenType;
    }

    public String getRequestedTokenType() {
        return requestedTokenType;
    }

    public void setRequestedTokenType(String requestedTokenType) {
        this.requestedTokenType = requestedTokenType;
    }

    /**
     * Returns the grant type.
     *
     * @return The grant type.
     */
    public GrantType getGrantType() {
        return grantType;
    }

    /**
     * Sets the grant type.
     *
     * @param grantType The grant type.
     */
    public void setGrantType(GrantType grantType) {
        this.grantType = grantType;
    }

    /**
     * Returns the authorization code.
     *
     * @return The authorization code.
     */
    public String getCode() {
        return code;
    }

    /**
     * Sets the authorization code.
     *
     * @param code The authorization code.
     */
    public void setCode(String code) {
        this.code = code;
    }

    /**
     * Gets PKCE code verifier.
     *
     * @return code verifier
     */
    public String getCodeVerifier() {
        return codeVerifier;
    }

    /**
     * Sets PKCE code verifier.
     *
     * @param codeVerifier code verifier
     */
    public void setCodeVerifier(String codeVerifier) {
        this.codeVerifier = codeVerifier;
    }

    /**
     * Returns the redirect URI.
     *
     * @return The redirect URI.
     */
    public String getRedirectUri() {
        return redirectUri;
    }

    /**
     * Sets the redirect URI.
     *
     * @param redirectUri The redirect URI.
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * Returns the username.
     *
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     *
     * @param username The username.
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Returns the password.
     *
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Sets the password.
     *
     * @param password The password.
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Returns the scope.
     *
     * @return The scope.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope.
     *
     * @param scope The scope.
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Returns the assertion.
     *
     * @return The assertion.
     */
    public String getAssertion() {
        return assertion;
    }

    /**
     * Sets the assertion.
     *
     * @param assertion The assertion.
     */
    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    /**
     * Returns the refresh token.
     *
     * @return The refresh token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token.
     *
     * @param refreshToken The refresh token.
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public String getDeviceCode() {
        return deviceCode;
    }

    public void setDeviceCode(String deviceCode) {
        this.deviceCode = deviceCode;
    }

    public DPoP getDpop() {
        return dpop;
    }

    public void setDpop(DPoP dpop) {
        this.dpop = dpop;
    }

    /**
     * Returns a query string with the parameters of the authorization request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        QueryBuilder builder = QueryBuilder.instance();

        builder.appendIfNotNull("grant_type", grantType);
        builder.append("code", code);
        builder.append("redirect_uri", redirectUri);
        builder.append("scope", scope);
        builder.append("username", username);
        builder.append("password", password);
        builder.append("assertion", assertion);
        builder.append("refresh_token", refreshToken);
        builder.append("auth_req_id", authReqId);
        builder.append("device_code", deviceCode);
        builder.append("audience", getAudience());
        builder.append("subject_token", subjectToken);
        builder.append("subject_token_type", subjectTokenType);
        builder.append("actor_token", actorToken);
        builder.append("actor_token_type", actorTokenType);
        builder.append("requested_token_type", requestedTokenType);
        appendClientAuthnToQuery(builder);
        for (String key : getCustomParameters().keySet()) {
            builder.append(key, getCustomParameters().get(key));
        }

        return builder.toString();
    }

    /**
     * Returns a collection of parameters of the token request. Any
     * <code>null</code> or empty parameter will be omitted.
     *
     * @return A collection of parameters.
     */
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();

        if (grantType != null) {
            parameters.put("grant_type", grantType.toString());
        }
        if (code != null && !code.isEmpty()) {
            parameters.put("code", code);
        }
        if (redirectUri != null && !redirectUri.isEmpty()) {
            parameters.put("redirect_uri", redirectUri);
        }
        if (username != null && !username.isEmpty()) {
            parameters.put("username", username);
        }
        if (StringUtils.isNotBlank(getAudience())) {
            parameters.put("audience", getAudience());
        }
        if (StringUtils.isNotBlank(subjectToken)) {
            parameters.put("subject_token", subjectToken);
        }
        if (StringUtils.isNotBlank(subjectTokenType)) {
            parameters.put("subject_token_type", subjectTokenType);
        }
        if (StringUtils.isNotBlank(actorToken)) {
            parameters.put("actor_token", actorToken);
        }
        if (StringUtils.isNotBlank(actorTokenType)) {
            parameters.put("actor_token_type", actorTokenType);
        }
        if (StringUtils.isNotBlank(requestedTokenType)) {
            parameters.put("requested_token_type", requestedTokenType);
        }
        if (password != null && !password.isEmpty()) {
            parameters.put("password", password);
        }
        if (scope != null && !scope.isEmpty()) {
            parameters.put("scope", scope);
        }
        if (assertion != null && !assertion.isEmpty()) {
            parameters.put("assertion", assertion);
        }
        if (refreshToken != null && !refreshToken.isEmpty()) {
            parameters.put("refresh_token", refreshToken);
        }
        if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_POST) {
            if (getAuthUsername() != null && !getAuthUsername().isEmpty()) {
                parameters.put("client_id", getAuthUsername());
            }
            if (getAuthPassword() != null && !getAuthPassword().isEmpty()) {
                parameters.put("client_secret", getAuthPassword());
            }
        } else if (getAuthenticationMethod() == AuthenticationMethod.CLIENT_SECRET_JWT ||
                getAuthenticationMethod() == AuthenticationMethod.PRIVATE_KEY_JWT) {
            parameters.put("client_assertion_type", ClientAssertionType.JWT_BEARER.toString());
            parameters.put("client_assertion", getClientAssertion());
        }
        for (String key : getCustomParameters().keySet()) {
            parameters.put(key, getCustomParameters().get(key));
        }

        return parameters;
    }

    public static class Builder {

        private GrantType grantType;
        private String scope;

        public Builder grantType(GrantType grantType) {
            this.grantType = grantType;
            return this;
        }

        public Builder scope(String scope) {
            this.scope = scope;
            return this;
        }

        public Builder pat(String... scopeArray) {
            StringBuilder scope = new StringBuilder(UmaScopeType.PROTECTION.getValue());
            if (scopeArray != null && scopeArray.length > 0) {
                for (String s : scopeArray) {
                    scope.append(" ").append(s);
                }
            }
            return scope(scope.toString());
        }

        public TokenRequest build() {
            final TokenRequest request = new TokenRequest(grantType);
            request.setScope(scope);
            return request;
        }
    }
}
