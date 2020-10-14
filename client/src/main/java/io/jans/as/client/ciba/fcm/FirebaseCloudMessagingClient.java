/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.fcm;

import io.jans.as.client.BaseClient;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

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
        initClientRequest();
        return _exec();
    }

    private FirebaseCloudMessagingResponse _exec() {
        try {
            // Prepare request parameters
            clientRequest.setHttpMethod(getHttpMethod());

            clientRequest.header("Content-Type", getRequest().getContentType());
            clientRequest.accept(getRequest().getMediaType());

            if (StringUtils.isNotBlank(getRequest().getKey())) {
                clientRequest.header("Authorization", "key=" + getRequest().getKey());
            }

            JSONObject requestBody = getRequest().getJSONParameters();
            clientRequest.body(MediaType.APPLICATION_JSON, requestBody.toString(4));

            // Call REST Service and handle response
            clientResponse = clientRequest.post(String.class);
            setResponse(new FirebaseCloudMessagingResponse(clientResponse));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}