/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.gluu.oxauth.client.fcm.FirebaseCloudMessagingClient;
import org.gluu.oxauth.client.fcm.FirebaseCloudMessagingRequest;
import org.gluu.oxauth.client.fcm.FirebaseCloudMessagingResponse;
import org.gluu.oxauth.interception.CIBAEndUserNotificationInterception;
import org.gluu.oxauth.interception.CIBAEndUserNotificationInterceptionInterface;
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
@CIBAEndUserNotificationInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBAEndUserNotificationInterceptor implements CIBAEndUserNotificationInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBAEndUserNotificationInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBAEndUserNotificationInterceptor() {
        log.info("CIBA End-User Notification Interceptor loaded.");
    }

    @AroundInvoke
    public Object notifyEndUser(InvocationContext ctx) {
        log.debug("CIBA: notifying end-user...");

        try {
            String authReqId = (String) ctx.getParameters()[0];
            String deviceRegistrationToken = (String) ctx.getParameters()[1];
            notifyEndUser(authReqId, deviceRegistrationToken);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to process CIBA support.", e);
        }

        return true;
    }

    @Override
    public void notifyEndUser(String authReqId, String deviceRegistrationToken) {
        String url = appConfiguration.getCibaEndUserNotificationConfig().getNotificationUrl();
        String key = appConfiguration.getCibaEndUserNotificationConfig().getNotificationKey();
        String to = deviceRegistrationToken;
        String title = "oxAuth Authentication Request";
        String body = "Client Initiated Backchannel Authentication (CIBA)";
        String clickAction = "https://ce.gluu.info:8443/ciba/authorize.htm?auth_req_id=" + authReqId;

        FirebaseCloudMessagingRequest request = new FirebaseCloudMessagingRequest(key, to, title, body, clickAction);
        FirebaseCloudMessagingClient client = new FirebaseCloudMessagingClient(url);
        client.setRequest(request);
        FirebaseCloudMessagingResponse response = client.exec();
    }
}