/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.configuration;

import org.gluu.oxauth.client.push.PushTokenDeliveryClient;
import org.gluu.oxauth.client.push.PushTokenDeliveryRequest;
import org.gluu.oxauth.client.push.PushTokenDeliveryResponse;
import org.gluu.oxauth.model.ciba.PushTokenDeliveryRequestParam;
import org.gluu.oxauth.model.common.TokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.fcm.FirebaseCloudMessagingClient;
import org.gluu.oxauth.client.fcm.FirebaseCloudMessagingRequest;
import org.gluu.oxauth.client.fcm.FirebaseCloudMessagingResponse;
import org.gluu.oxauth.interception.CIBAPushTokenDeliveryInterception;
import org.gluu.oxauth.interception.CIBAPushTokenDeliveryInterceptionInterface;
import org.gluu.oxauth.model.configuration.AppConfiguration;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.io.Serializable;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Interceptor
@CIBAPushTokenDeliveryInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBAPushTokenDeliveryInterceptor implements CIBAPushTokenDeliveryInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBAPushTokenDeliveryInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBAPushTokenDeliveryInterceptor() {
        log.info("CIBA Push Token Delivery Interceptor loaded.");
    }

    @AroundInvoke
    public Object pushTokenDelivery(InvocationContext ctx) {
        log.debug("CIBA: notifying end-user...");

        try {
            String authReqId = (String) ctx.getParameters()[0];
            pushTokenDelivery(authReqId);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to process CIBA support.", e);
        }

        return true;
    }

    @Override
    public void pushTokenDelivery(String authReqId) {
        PushTokenDeliveryRequest pushTokenDeliveryRequest = new PushTokenDeliveryRequest();

        //pushTokenDeliveryRequest.setClientNotificationToken(authorizationGrant.getClientNotificationToken());
        pushTokenDeliveryRequest.setAuthReqId(authReqId);
        //pushTokenDeliveryRequest.setAccessToken(authorizationGrant.getAccessToken());
        pushTokenDeliveryRequest.setTokenType(TokenType.BEARER);
        pushTokenDeliveryRequest.setRefreshToken(null);
        pushTokenDeliveryRequest.setExpiresIn(3600);
        pushTokenDeliveryRequest.setIdToken(null);

        String clientNotificationEndpoint = "https://ce.gluu.info/oxauth-ciba-client-test/client-notification-endpoint"; //authorizationGrant.getClientNotificationEndpoint();

        PushTokenDeliveryClient pushTokenDeliveryClient = new PushTokenDeliveryClient(clientNotificationEndpoint);
        pushTokenDeliveryClient.setRequest(pushTokenDeliveryRequest);
        PushTokenDeliveryResponse pushTokenDeliveryResponse = pushTokenDeliveryClient.exec();
    }
}
