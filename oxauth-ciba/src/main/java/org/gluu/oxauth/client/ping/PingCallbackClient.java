/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.client.ping;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.gluu.oxauth.client.BaseClient;
import org.json.JSONObject;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
public class PingCallbackClient extends BaseClient<PingCallbackRequest, PingCallbackResponse> {

    private static final Logger LOG = Logger.getLogger(PingCallbackClient.class);

    public PingCallbackClient(String url) {
        super(url);
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public PingCallbackResponse exec() {
        initClientRequest();
        return _exec();
    }

    private PingCallbackResponse _exec() {
        try {
            // Prepare request parameters
            clientRequest.setHttpMethod(getHttpMethod());

            clientRequest.header("Content-Type", getRequest().getContentType());

            if (StringUtils.isNotBlank(getRequest().getClientNotificationToken())) {
                clientRequest.header("Authorization", "Bearer " + getRequest().getClientNotificationToken());
            }

            JSONObject requestBody = getRequest().getJSONParameters();
            clientRequest.body(MediaType.APPLICATION_JSON, requestBody.toString(4));

            // Call REST Service and handle response
            clientResponse = clientRequest.post(String.class);
            setResponse(new PingCallbackResponse(clientResponse));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }
}
