/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.fcm;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.client.BaseRequest;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;

import static org.gluu.oxauth.model.ciba.FirebaseCloudMessagingRequestParam.*;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class FirebaseCloudMessagingRequest extends BaseRequest {

    private String key;
    private String to;
    private Notification notification;

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
    private String title;
    private String body;
    private String clickAction;

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