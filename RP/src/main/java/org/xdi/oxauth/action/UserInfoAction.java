/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.action;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.client.UserInfoClient;
import org.xdi.oxauth.client.UserInfoRequest;
import org.xdi.oxauth.model.common.AuthorizationMethod;

/**
 * @author Javier Rojas Blum Date: 02.22.2012
 */
@Name("userInfoAction")
@Scope(ScopeType.SESSION)
@AutoCreate
public class UserInfoAction {

    @Logger
    private Log log;

    private String userInfoEndpoint;
    private String accessToken;

    private boolean showResults;
    private String requestString;
    private String responseString;

    private AuthorizationMethod authorizationMethod;

    public void exec() {
        try {
            UserInfoRequest request = new UserInfoRequest(accessToken);
            request.setAuthorizationMethod(authorizationMethod);

            UserInfoClient client = new UserInfoClient(userInfoEndpoint);
            client.setRequest(request);
            client.exec();

            showResults = true;
            requestString = client.getRequestAsString();
            responseString = client.getResponseAsString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Factory(value = "authorizationMethods")
    public AuthorizationMethod[] authorizationMethods() {
        return AuthorizationMethod.values();
    }

    public String getUserInfoEndpoint() {
        return userInfoEndpoint;
    }

    public void setUserInfoEndpoint(String userInfoEndpoint) {
        this.userInfoEndpoint = userInfoEndpoint;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
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

    public AuthorizationMethod getAuthorizationMethod() {
        return authorizationMethod;
    }

    public void setAuthorizationMethod(AuthorizationMethod authorizationMethod) {
        this.authorizationMethod = authorizationMethod;
    }
}