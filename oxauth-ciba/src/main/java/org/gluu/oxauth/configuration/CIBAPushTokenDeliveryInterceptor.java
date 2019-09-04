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
 * @version September 4, 2019
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
            String clientNotificationEndpoint = (String) ctx.getParameters()[1];
            String clientNotificationToken = (String) ctx.getParameters()[2];
            String accessToken = (String) ctx.getParameters()[3];
            String refreshToken = (String) ctx.getParameters()[4];
            String idToken = (String) ctx.getParameters()[5];
            Integer expiresIn = (Integer) ctx.getParameters()[6];
            pushTokenDelivery(authReqId, clientNotificationEndpoint, clientNotificationToken, accessToken, refreshToken, idToken, expiresIn);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to process CIBA support.", e);
        }

        return true;
    }

    @Override
    public void pushTokenDelivery(String authReqId, String clientNotificationEndpoint, String clientNotificationToken,
                                  String accessToken, String refreshToken, String idToken, Integer expiresIn) {
        PushTokenDeliveryRequest pushTokenDeliveryRequest = new PushTokenDeliveryRequest();

        pushTokenDeliveryRequest.setClientNotificationToken(clientNotificationToken);
        pushTokenDeliveryRequest.setAuthReqId(authReqId);
        pushTokenDeliveryRequest.setAccessToken(accessToken);
        pushTokenDeliveryRequest.setTokenType(TokenType.BEARER);
        pushTokenDeliveryRequest.setRefreshToken(refreshToken);
        pushTokenDeliveryRequest.setExpiresIn(expiresIn);
        pushTokenDeliveryRequest.setIdToken(idToken);

        PushTokenDeliveryClient pushTokenDeliveryClient = new PushTokenDeliveryClient(clientNotificationEndpoint);
        pushTokenDeliveryClient.setRequest(pushTokenDeliveryRequest);
        PushTokenDeliveryResponse pushTokenDeliveryResponse = pushTokenDeliveryClient.exec();
    }
}
