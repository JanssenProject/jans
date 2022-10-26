/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.client.ciba.ping;

import io.jans.as.client.BaseClient;
import io.jans.as.client.util.ClientUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.jaxrs.engines.ApacheHttpClient43Engine;
import org.json.JSONObject;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
public class PingCallbackClient extends BaseClient<PingCallbackRequest, PingCallbackResponse> {

    private static final Logger LOG = Logger.getLogger(PingCallbackClient.class);

    private final boolean fapiCompatibility;

    public PingCallbackClient(String url, boolean fapiCompatibility) {
        super(url);
        this.fapiCompatibility = fapiCompatibility;
    }

    @Override
    public String getHttpMethod() {
        return HttpMethod.POST;
    }

    public PingCallbackResponse exec() {
        if (this.fapiCompatibility) {
            setExecutor(getApacheHttpClient4ExecutorForMTLS());
        }
        initClient();
        return _exec();
    }

    private PingCallbackResponse _exec() {
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
            setResponse(new PingCallbackResponse(clientResponse));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            closeConnection();
        }

        return getResponse();
    }

    /**
     * Creates an executor responsible to process rest calls using special SSL context defined in FAPI-CIBA specs.
     */
    private ApacheHttpClient43Engine getApacheHttpClient4ExecutorForMTLS() {
        // Ciphers accepted by FAPI-CIBA specs and OpenJDK.
        String[] ciphers = new String[]{"TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384"};
        return new ApacheHttpClient43Engine(ClientUtil.createHttpClient("TLSv1.2", ciphers));
    }

}
