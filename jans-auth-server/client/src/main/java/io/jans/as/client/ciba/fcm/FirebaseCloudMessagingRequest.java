/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.fcm;

import io.jans.as.client.BaseRequest;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;

import jakarta.ws.rs.core.MediaType;

import static io.jans.as.model.ciba.FirebaseCloudMessagingRequestParam.BODY;
import static io.jans.as.model.ciba.FirebaseCloudMessagingRequestParam.CLICK_ACTION;
import static io.jans.as.model.ciba.FirebaseCloudMessagingRequestParam.NOTIFICATION;
import static io.jans.as.model.ciba.FirebaseCloudMessagingRequestParam.TITLE;
import static io.jans.as.model.ciba.FirebaseCloudMessagingRequestParam.TO;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class FirebaseCloudMessagingRequest extends BaseRequest {

    private final String key;
    private final String to;
    private final Notification notification;

    public FirebaseCloudMessagingRequest(String key, String to, String title, String body, String clickAction) {
        this.key = key;
        this.to = to;
        this.notification = new Notification(title, body, clickAction);

        setContentType(MediaType.APPLICATION_JSON);
        setMediaType(MediaType.APPLICATION_JSON);
    }

    public String getKey() {
        return key;
    }

    @Override
    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();

        if (StringUtils.isNotBlank(to)) {
            parameters.put(TO, to);
        }

        parameters.put(NOTIFICATION, notification.getJSONParameters());

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

class Notification {
    private final String title;
    private final String body;
    private final String clickAction;

    public Notification(String title, String body, String clickAction) {
        this.title = title;
        this.body = body;
        this.clickAction = clickAction;
    }

    public JSONObject getJSONParameters() throws JSONException {
        JSONObject parameters = new JSONObject();

        if (StringUtils.isNotBlank(title)) {
            parameters.put(TITLE, title);
        }

        if (StringUtils.isNotBlank(body)) {
            parameters.put(BODY, body);
        }

        if (StringUtils.isNotBlank(clickAction)) {
            parameters.put(CLICK_ACTION, clickAction);
        }

        return parameters;
    }
} 