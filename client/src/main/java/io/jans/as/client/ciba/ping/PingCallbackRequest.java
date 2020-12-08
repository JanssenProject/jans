/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.ping;

import io.jans.as.client.BaseRequest;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;

import static io.jans.as.model.ciba.PushTokenDeliveryRequestParam.AUTHORIZATION_REQUEST_ID;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
public class PingCallbackRequest extends BaseRequest {

    private String clientNotificationToken;
    private String authReqId;

    public PingCallbackRequest() {
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

    @Override
    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();

        if (StringUtils.isNotBlank(authReqId)) {
            parameters.put(AUTHORIZATION_REQUEST_ID, authReqId);
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
