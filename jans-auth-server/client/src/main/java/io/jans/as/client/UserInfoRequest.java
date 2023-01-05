/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthorizationMethod;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a User Info request to send to the authorization server.
 *
 * @author Javier Rojas Blum
 * @version April 25, 2022
 */
public class UserInfoRequest extends BaseRequest {

    private String accessToken;

    /**
     * Constructs a User Info Request.
     *
     * @param accessToken The access token obtained from the Jans Auth authorization request.
     */
    public UserInfoRequest(String accessToken) {
        this.accessToken = accessToken;
        setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
    }

    /**
     * Returns the access token obtained from Jans Auth authorization request.
     *
     * @return The access token obtained from Jans Auth authorization request.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token obtained from Jans Auth authorization request.
     *
     * @param accessToken The access token obtained from Jans Auth authorization request.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Returns a query string with the parameters of the User Info request.
     * Any <code>null</code> or empty parameter will be omitted.
     *
     * @return A query string of parameters.
     */
    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();

        boolean isMethodOk = getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER ||
                getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER;
        if (StringUtils.isNotBlank(accessToken) && isMethodOk) {
            queryStringBuilder.append("access_token=").append(accessToken);
        }

        return queryStringBuilder.toString();
    }

    /**
     * Returns a collection of parameters of the user info request. Any
     * <code>null</code> or empty parameter will be omitted.
     *
     * @return A collection of parameters.
     */
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();
        if (StringUtils.isBlank(accessToken)) {
            return parameters;
        }

        if (getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER ||
                getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
            parameters.put("access_token", accessToken);
        }

        return parameters;
    }
}