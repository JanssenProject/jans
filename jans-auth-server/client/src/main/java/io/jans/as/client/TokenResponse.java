/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client;

import io.jans.as.model.common.TokenType;
import io.jans.as.model.token.TokenErrorResponseType;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.core.Response;

/**
 * Represents a token response received from the authorization server.
 *
 * @author Javier Rojas Blum Date: 10.19.2011
 */
public class TokenResponse extends BaseResponseWithErrors<TokenErrorResponseType> {

    private static final Logger LOG = Logger.getLogger(TokenResponse.class);

    private String accessToken;
    private TokenType tokenType;
    private Integer expiresIn;
    private String refreshToken;
    private String scope;
    private String idToken;
    private String deviceToken;

    public TokenResponse() {
    }

    /**
     * Constructs a token response.
     *
     * @param clientResponse The response
     */
    public TokenResponse(Response clientResponse) {
        super(clientResponse);
    }

    @Override
    public TokenErrorResponseType fromString(String p_str) {
        return TokenErrorResponseType.fromString(p_str);
    }

    public void injectDataFromJson() {
        injectDataFromJson(entity);
    }

    @Override
    public void injectDataFromJson(String json) {
        if (StringUtils.isNotBlank(entity)) {
            try {
                JSONObject jsonObj = new JSONObject(entity);
                if (jsonObj.has("access_token")) {
                    setAccessToken(jsonObj.getString("access_token"));
                }
                if (jsonObj.has("token_type")) {
                    setTokenType(TokenType.fromString(jsonObj.getString("token_type")));
                }
                if (jsonObj.has("expires_in")) {
                    setExpiresIn(jsonObj.getInt("expires_in"));
                }
                if (jsonObj.has("refresh_token")) {
                    setRefreshToken(jsonObj.getString("refresh_token"));
                }
                if (jsonObj.has("scope")) {
                    setScope(jsonObj.getString("scope"));
                }
                if (jsonObj.has("id_token")) {
                    setIdToken(jsonObj.getString("id_token"));
                }
                setDeviceToken(jsonObj.optString("device_token"));
            } catch (JSONException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    /**
     * Returns the access token issued by the authorization server.
     *
     * @return The access token issued by the authorization server.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Sets the access token issued by the authorization server.
     *
     * @param accessToken The access token issued by the authorization server.
     */
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     * Returns the type of the token issued. Value is case insensitive.
     *
     * @return The type of the token issued.
     */
    public TokenType getTokenType() {
        return tokenType;
    }

    /**
     * Sets the type of the token issued. Value is case insensitive.
     *
     * @param tokenType The type of the token issued.
     */
    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    /**
     * Returns the lifetime in seconds of the access token.
     *
     * @return The lifetime in seconds of the access token.
     */
    public Integer getExpiresIn() {
        return expiresIn;
    }

    /**
     * Sets the lifetime in seconds of the access token.
     *
     * @param expiresIn The lifetime in seconds of the access token.
     */
    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    /**
     * Returns the refresh token which can be used to obtain new access tokens
     * using the same authorization grant.
     *
     * @return The refresh token.
     */
    public String getRefreshToken() {
        return refreshToken;
    }

    /**
     * Sets the refresh token which can be used to obtain new access tokens
     * using the same authorization grant.
     *
     * @param refreshToken The refresh token.
     */
    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    /**
     * Returns the scope of the access token.
     *
     * @return The scope of the access token.
     */
    public String getScope() {
        return scope;
    }

    /**
     * Sets the scope of the access token.
     *
     * @param scope The scope of the access token.
     */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
     * Gets the value of the id token.
     *
     * @return The id token.
     */
    public String getIdToken() {
        return idToken;
    }

    /**
     * Sets the value of the id token.
     *
     * @param idToken The id token.
     */
    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }
}