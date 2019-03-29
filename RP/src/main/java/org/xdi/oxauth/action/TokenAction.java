/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.action;

import java.io.Serializable;

import javax.enterprise.context.SessionScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.model.common.AuthenticationMethod;
import org.gluu.oxauth.model.common.GrantType;
import org.slf4j.Logger;
import org.xdi.oxauth.client.TokenClient;
import org.xdi.oxauth.client.TokenRequest;
import org.xdi.oxauth.client.TokenResponse;

/**
 * @author Javier Rojas Blum Date: 02.21.2012
 */
@Named
@SessionScoped
public class TokenAction implements Serializable {

	private static final long serialVersionUID = -1049039555549738261L;

	@Inject
    private Logger log;

    @Inject
    private UserInfoAction userInfoAction;

    private String tokenEndpoint;
    private GrantType grantType;
    private String clientId;
    private String clientSecret;
    private String code;
    private String redirectUri;
    private String username;
    private String password;
    private String scope;
    private String assertion;
    private String refreshToken;

    private boolean showResults;
    private String requestString;
    private String responseString;

    private AuthenticationMethod authenticationMethod;

    public void exec() {
        try {
            TokenRequest request = new TokenRequest(grantType);
            request.setAuthUsername(clientId);
            request.setAuthPassword(clientSecret);
            request.setCode(code);
            request.setRedirectUri(redirectUri);
            request.setUsername(username);
            request.setPassword(password);
            request.setScope(scope);
            request.setAssertion(assertion);
            request.setRefreshToken(refreshToken);
            request.setAuthenticationMethod(authenticationMethod);
            if (authenticationMethod.equals(AuthenticationMethod.CLIENT_SECRET_JWT)) {
                request.setAudience(tokenEndpoint);
            }

            TokenClient client = new TokenClient(tokenEndpoint);
            client.setRequest(request);
            TokenResponse response = client.exec();

            if (response.getStatus() == 200) {
                userInfoAction.setAccessToken(response.getAccessToken());
            }

            showResults = true;
            requestString = client.getRequestAsString();
            responseString = client.getResponseAsString();
        } catch (Exception e) {
        	log.error(e.getMessage(), e);
        }
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public GrantType getGrantType() {
        return grantType;
    }

    public void setGrantType(GrantType grantType) {
        this.grantType = grantType;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getAssertion() {
        return assertion;
    }

    public void setAssertion(String assertion) {
        this.assertion = assertion;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public boolean isShowResults() {
        return showResults;
    }

    public void setShowResults(boolean showResults) {
        this.showResults = showResults;
    }

    public String getRequestString() {
        return requestString;
    }

    public void setRequestString(String requestString) {
        this.requestString = requestString;
    }

    public String getResponseString() {
        return responseString;
    }

    public void setResponseString(String responseString) {
        this.responseString = responseString;
    }

    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }
}