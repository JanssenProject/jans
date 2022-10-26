/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.fcm;

import io.jans.as.client.BaseClient;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class FirebaseCloudMessagingClient extends BaseClient<FirebaseCloudMessagingRequest, FirebaseCloudMessagingResponse> {

    private static final Logger LOG = Logger.getLogger(FirebaseCloudMessagingClient.class);

    public FirebaseCloudMessagingClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public FirebaseCloudMessagingResponse execFirebaseCloudMessaging(String key, String to, String title, String body, String clickAction) {
        setRequest(new FirebaseCloudMessagingRequest(key, to, title, body, clickAction));

        return exec();
    }

    public FirebaseCloudMessagingResponse exec() {
        initClient();
        return _exec();
    }

    private FirebaseCloudMessagingResponse _exec() {
        try {
            // Prepare request parameters

            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", getRequest().getContentType());
            clientRequest.accept(getRequest().getMediaType());

            if (StringUtils.isNotBlank(getRequest().getKey())) {
                clientRequest.header("Authorization", "key=" + getRequest().getKey());
            }

            JSONObject requestBody = getRequest().getJSONParameters();

            // Call REST Service and handle response
            clientResponse = clientRequest.buildPost(Entity.json(requestBody.toString(4))).invoke();
            setResponse(new FirebaseCloudMessagingResponse(clientResponse));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}