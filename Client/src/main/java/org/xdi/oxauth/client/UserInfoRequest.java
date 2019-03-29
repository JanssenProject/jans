/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.model.common.AuthorizationMethod;

/**
 * Represents a User Info request to send to the authorization server.
 *
 * @author Javier Rojas Blum Date: 11.28.2011
 */
public class UserInfoRequest extends BaseRequest {

    private String accessToken;

    /**
     * Constructs a User Info Request.
     *
     * @param accessToken The access token obtained from the oxAuth authorization request.
     */
    public UserInfoRequest(String accessToken) {
        this.accessToken = accessToken;
        setAuthorizationMethod(AuthorizationMethod.AUTHORIZATION_REQUEST_HEADER_FIELD);
    }

    /**
     * Returns the access token obtained from oxAuth authorization request.
     *
     * @return The access token obtained from oxAuth authorization request.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token obtained from oxAuth authorization request.
     *
     * @param accessToken The access token obtained from oxAuth authorization request.
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

        if (StringUtils.isNotBlank(accessToken)) {
            if (getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER
                    || getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
                queryStringBuilder.append("access_token=").append(accessToken);
            }
        }

        return queryStringBuilder.toString();
    }

    /**
     * Returns a collection of parameters of the user info request. Any
     * <code>null</code> or empty parameter will be omitted.
     *
     * @return A collection of parameters.
     */
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<String, String>();

        if (accessToken != null && !accessToken.isEmpty()) {
            if (getAuthorizationMethod() == AuthorizationMethod.FORM_ENCODED_BODY_PARAMETER
                    || getAuthorizationMethod() == AuthorizationMethod.URL_QUERY_PARAMETER) {
                parameters.put("access_token", accessToken);
            }
        }

        return parameters;
    }
}