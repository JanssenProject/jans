/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.AuthorizationMethod;
import io.jans.as.model.util.Util;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

import static io.jans.as.model.ciba.PushTokenDeliveryRequestParam.AUTHORIZATION_REQUEST_ID;

/**
 * @author Javier Rojas Blum
 * @version April 25. 2022
 */
public abstract class BaseRequest {

    private static final Map<String, String> EMPTY_MAP = new HashMap<>();
    private static final JSONObject EMPTY_JSON_OBJECT = new JSONObject();

    private String contentType;
    private String mediaType;
    private String authUsername;
    private String authPassword;
    private AuthenticationMethod authenticationMethod;
    private AuthorizationMethod authorizationMethod;
    private final Map<String, String> customParameters;

    protected BaseRequest() {
        customParameters = new HashMap<>();
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getAuthUsername() {
        return authUsername;
    }

    public void setAuthUsername(String authUsername) {
        this.authUsername = authUsername;
    }

    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    public AuthenticationMethod getAuthenticationMethod() {
        return authenticationMethod;
    }

    public void setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
        this.authenticationMethod = authenticationMethod;
    }

    public AuthorizationMethod getAuthorizationMethod() {
        return authorizationMethod;
    }

    public void setAuthorizationMethod(AuthorizationMethod authorizationMethod) {
        this.authorizationMethod = authorizationMethod;
    }

    public Map<String, String> getCustomParameters() {
        return customParameters;
    }

    public void addCustomParameter(String paramName, String paramValue) {
        customParameters.put(paramName, paramValue);
    }

    public boolean hasCredentials() {
        return authUsername != null && authPassword != null
                && !authUsername.isEmpty()
                && !authPassword.isEmpty();
    }

    /**
     * Returns the client credentials (URL encoded).
     *
     * @return The client credentials.
     */
    public String getCredentials() throws UnsupportedEncodingException {
        return URLEncoder.encode(authUsername, Util.UTF8_STRING_ENCODING)
                + ":"
                + URLEncoder.encode(authPassword, Util.UTF8_STRING_ENCODING);
    }

    /**
     * Returns the client credentials encoded using base64.
     *
     * @return The encoded client credentials.
     */
    public String getEncodedCredentials() {
        try {
            if (hasCredentials()) {
                return Base64.encodeBase64String(Util.getBytes(getCredentials()));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getEncodedCredentials(String clientId, String clientSecret) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(Util.getBytes(URLEncoder.encode(clientId, Util.UTF8_STRING_ENCODING) + ":" + URLEncoder.encode(clientSecret, Util.UTF8_STRING_ENCODING)));
    }

    public Map<String, String> getParameters() {
        return EMPTY_MAP;
    }

    public JSONObject getJSONParameters() throws JSONException {
        return EMPTY_JSON_OBJECT;
    }

    public abstract String getQueryString();
}
