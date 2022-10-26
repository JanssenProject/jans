/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.push;

import io.jans.as.client.BaseClient;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

/**
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
public class PushTokenDeliveryClient extends BaseClient<PushTokenDeliveryRequest, PushTokenDeliveryResponse> {

    private static final Logger LOG = Logger.getLogger(PushTokenDeliveryClient.class);

    public PushTokenDeliveryClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public PushTokenDeliveryResponse exec() {
        initClient();
        return _exec();
    }

    private PushTokenDeliveryResponse _exec() {
        try {
            // Prepare request parameters

            Builder clientRequest = webTarget.request();
            applyCookies(clientRequest);

            clientRequest.header("Content-Type", getRequest().getContentType());

            if (StringUtils.isNotBlank(getRequest().getClientNotificationToken())) {
                clientRequest.header("Authorization", "Bearer " + getRequest().getClientNotificationToken());
            }

            JSONObject requestBody = getRequest().getJSONParameters();

            // Call REST Service and handle response
            clientResponse = clientRequest.buildPost(Entity.json(requestBody.toString(4))).invoke();
            setResponse(new PushTokenDeliveryResponse(clientResponse));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}