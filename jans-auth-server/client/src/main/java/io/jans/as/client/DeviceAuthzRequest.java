/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.util.Util;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import jakarta.ws.rs.core.MediaType;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a device authorization request to send to the authorization server.
 * @version April 25, 2022
 */
public class DeviceAuthzRequest extends ClientAuthnRequest {

    private static final Logger LOG = Logger.getLogger(DeviceAuthzRequest.class);

    private String clientId;
    private List<String> scopes;

    public DeviceAuthzRequest(String clientId, List<String> scopes) {
        setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        setAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC);
        this.clientId = clientId;
        this.scopes = scopes;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public String getScopesAsString() {
        return Util.listAsString(scopes);
    }

    /**
     * Returns a collection of parameters of the authorization request. Any
     * <code>null</code> or empty parameter will be omitted.
     *
     * @return A collection of parameters.
     */
    public Map<String, String> getParameters() {
        Map<String, String> parameters = new HashMap<>();

        try {
            // OAuth 2.0 request parameters
            final String scopesAsString = getScopesAsString();

            if (StringUtils.isNotBlank(clientId)) {
                parameters.put(AuthorizeRequestParam.CLIENT_ID, clientId);
            }
            if (StringUtils.isNotBlank(scopesAsString)) {
                parameters.put(AuthorizeRequestParam.SCOPE, scopesAsString);
            }

            for (String key : getCustomParameters().keySet()) {
                parameters.put(key, getCustomParameters().get(key));
            }
        } catch (JSONException e) {
            LOG.error(e.getMessage(), e);
        }

        return parameters;
    }

    @Override
    public String getQueryString() {
        StringBuilder queryStringBuilder = new StringBuilder();
        try {
            final String scopesAsString = getScopesAsString();

            if (StringUtils.isNotBlank(clientId)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.CLIENT_ID)
                        .append("=").append(URLEncoder.encode(clientId, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(scopesAsString)) {
                queryStringBuilder.append("&").append(AuthorizeRequestParam.SCOPE)
                        .append("=").append(URLEncoder.encode(scopesAsString, Util.UTF8_STRING_ENCODING));
            }

            for (String key : getCustomParameters().keySet()) {
                queryStringBuilder.append("&");
                queryStringBuilder.append(key).append("=").append(getCustomParameters().get(key));
            }
        } catch (UnsupportedEncodingException | JSONException e) {
            LOG.error(e.getMessage(), e);
        }

        return queryStringBuilder.toString();
    }
}