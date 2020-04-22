/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.client.ping.PingCallbackClient;
import org.gluu.oxauth.client.ping.PingCallbackRequest;
import org.gluu.oxauth.client.ping.PingCallbackResponse;
import org.gluu.oxauth.interception.CIBAPingCallbackInterception;
import org.gluu.oxauth.interception.CIBAPingCallbackInterceptionInterface;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

/**
 * @author Javier Rojas Blum
 * @version February 25, 2020
 */
@Interceptor
@CIBAPingCallbackInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBAPingCallbackInterceptor implements CIBAPingCallbackInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBAPingCallbackInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBAPingCallbackInterceptor() {
        log.info("CIBA Ping Callback Interceptor loaded.");
    }

    @AroundInvoke
    public Object pingCallback(InvocationContext ctx) {
        log.debug("CIBA: ping callback...");

        try {
            String authReqId = (String) ctx.getParameters()[0];
            String clientNotificationEndpoint = (String) ctx.getParameters()[1];
            String clientNotificationToken = (String) ctx.getParameters()[2];
            pingCallback(authReqId, clientNotificationEndpoint, clientNotificationToken);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to process CIBA support.", e);
        }

        return true;
    }

    @Override
    public void pingCallback(String authReqId, String clientNotificationEndpoint, String clientNotificationToken) {
        PingCallbackRequest pingCallbackRequest = new PingCallbackRequest();

        pingCallbackRequest.setClientNotificationToken(clientNotificationToken);
        pingCallbackRequest.setAuthReqId(authReqId);

        PingCallbackClient pingCallbackClient = new PingCallbackClient(clientNotificationEndpoint);
        pingCallbackClient.setRequest(pingCallbackRequest);
        PingCallbackResponse pingCallbackResponse = pingCallbackClient.exec();

        log.debug("CIBA: ping callback result status " + pingCallbackResponse.getStatus());
    }
}
