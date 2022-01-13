/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.push;

import io.jans.as.client.BaseRequest;
import io.jans.as.model.ciba.PushErrorResponseType;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.json.JSONException;
import org.json.JSONObject;

import static io.jans.as.model.ciba.PushErrorRequestParam.AUTHORIZATION_REQUEST_ID;
import static io.jans.as.model.ciba.PushErrorRequestParam.ERROR;
import static io.jans.as.model.ciba.PushErrorRequestParam.ERROR_DESCRIPTION;
import static io.jans.as.model.ciba.PushErrorRequestParam.ERROR_URI;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
public class PushErrorRequest extends BaseRequest {

    private String clientNotificationToken;
    private String authReqId;
    private PushErrorResponseType errorType;
    private String errorDescription;
    private String errorUri;

    public PushErrorRequest() {
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

    public PushErrorResponseType getErrorType() {
        return errorType;
    }

    public void setErrorType(PushErrorResponseType errorType) {
        this.errorType = errorType;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public String getErrorUri() {
        return errorUri;
    }

    public void setErrorUri(String errorUri) {
        this.errorUri = errorUri;
    }

    @Override
    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();

        if (StringUtils.isNotBlank(authReqId)) {
            parameters.put(AUTHORIZATION_REQUEST_ID, authReqId);
        }

        if (errorType != null) {
            parameters.put(ERROR, errorType.toString());
        }

        if (StringUtils.isNotBlank(errorDescription)) {
            parameters.put(ERROR_DESCRIPTION, errorDescription);
        }

        if (StringUtils.isNotBlank(errorUri)) {
            parameters.put(ERROR_URI, errorUri);
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
