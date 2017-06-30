/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.client;

import org.apache.commons.codec.binary.Base64;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.AuthorizationMethod;
import org.xdi.oxauth.model.util.Util;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Javier Rojas Blum Date: 01.31.2012
 */
public abstract class BaseRequest {

    private static final Map<String, String> EMPTY_MAP = new HashMap<String, String>();
    private static final JSONObject EMPTY_JSON_OBJECT = new JSONObject();

    private String contentType;
    private String mediaType;
    private String authUsername;
    private String authPassword;
    private AuthenticationMethod authenticationMethod;
    private AuthorizationMethod authorizationMethod;
    private Map<String, String> customParameters;

    protected BaseRequest() {
        customParameters = new HashMap<String, String>();
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

    public static String encodeCredentials(String username, String password) throws UnsupportedEncodingException {
        return Base64.encodeBase64String(Util.getBytes(username + ":" + password));
    }

    /**
     * Returns the client credentials encoded using base64.
     *
     * @return The encoded client credentials.
     */
    public String getEncodedCredentials() {
        try {
            if (hasCredentials()) {
                return encodeCredentials(authUsername, authPassword);
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Map<String, String> getParameters() {
        return EMPTY_MAP;
    }

    public JSONObject getJSONParameters() throws JSONException {
        return EMPTY_JSON_OBJECT;
    }

    public abstract String getQueryString();
}