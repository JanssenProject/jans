/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.push;

import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.gluu.oxauth.client.BaseRequest;
import org.gluu.oxauth.model.common.TokenType;
import org.json.JSONException;
import org.json.JSONObject;

import static org.gluu.oxauth.model.ciba.PushTokenDeliveryRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
public class PushTokenDeliveryRequest extends BaseRequest {

    private String clientNotificationToken;
    private String authReqId;
    private String accessToken;
    private TokenType tokenType;
    private String refreshToken;
    private Integer expiresIn;
    private String idToken;

    public PushTokenDeliveryRequest() {
        setContentType(ContentType.APPLICATION_JSON.toString());
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public String getAuthReqId() {
        return authReqId;
    }

    public void setAuthReqId(String authReqId) {
        this.authReqId = authReqId;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    @Override
    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();

        if (StringUtils.isNotBlank(authReqId)) {
            parameters.put(AUTHORIZATION_REQUEST_ID, authReqId);
        }

        if (StringUtils.isNotBlank(accessToken)) {
            parameters.put(ACCESS_TOKEN, accessToken);
        }

        if (tokenType != null) {
            parameters.put(TOKEN_TYPE, tokenType.getName());
        }

        if (StringUtils.isNotBlank(refreshToken)) {
            parameters.put(REFRESH_TOKEN, refreshToken);
        }

        if (expiresIn != null) {
            parameters.put(EXPIRES_IN, expiresIn);
        }

        if (StringUtils.isNotBlank(idToken)) {
            parameters.put(ID_TOKEN, idToken);
        }

        return parameters;
    }

    @Override
    public String getQueryString() {
        String jsonQueryString = null;

        try {
            jsonQueryString = getJSONParameters().toString(4).replace("\\/", "/");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return jsonQueryString;
    }
}