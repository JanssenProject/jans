/*
 * oxAuth-CIBA is available under the Gluu Enterprise License (2019).
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.client.push.PushErrorClient;
import org.gluu.oxauth.client.push.PushErrorRequest;
import org.gluu.oxauth.client.push.PushErrorResponse;
import org.gluu.oxauth.interception.CIBAPushErrorInterception;
import org.gluu.oxauth.interception.CIBAPushErrorInterceptionInterface;
import org.gluu.oxauth.model.ciba.PushErrorResponseType;
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
 * @version May 9, 2020
 */
@Interceptor
@CIBAPushErrorInterception
@Priority(Interceptor.Priority.APPLICATION)
public class CIBAPushErrorInterceptor implements CIBAPushErrorInterceptionInterface, Serializable {

    private final static Logger log = LoggerFactory.getLogger(CIBAPushErrorInterceptor.class);

    @Inject
    private AppConfiguration appConfiguration;

    public CIBAPushErrorInterceptor() {
        log.info("CIBA Push Error Interceptor loaded.");
    }

    @AroundInvoke
    public Object pushError(InvocationContext ctx) {
        log.debug("CIBA: push error...");

        try {
            String authReqId = (String) ctx.getParameters()[0];
            String clientNotificationEndpoint = (String) ctx.getParameters()[1];
            String clientNotificationToken = (String) ctx.getParameters()[2];
            PushErrorResponseType error = (PushErrorResponseType) ctx.getParameters()[3];
            String errorDescription = (String) ctx.getParameters()[4];
            pushError(authReqId, clientNotificationEndpoint, clientNotificationToken, error, errorDescription);
            ctx.proceed();
        } catch (Exception e) {
            log.error("Failed to process CIBA support.", e);
        }

        return true;
    }

    @Override
    public void pushError(String authReqId, String clientNotificationEndpoint, String clientNotificationToken,
                          PushErrorResponseType error, String errorDescription) {
        PushErrorRequest pushErrorRequest = new PushErrorRequest();

        pushErrorRequest.setClientNotificationToken(clientNotificationToken);
        pushErrorRequest.setAuthReqId(authReqId);
        pushErrorRequest.setErrorType(error);
        pushErrorRequest.setErrorDescription(errorDescription);

        PushErrorClient pushErrorClient = new PushErrorClient(clientNotificationEndpoint);
        pushErrorClient.setRequest(pushErrorRequest);
        PushErrorResponse pushErrorResponse = pushErrorClient.exec();

        log.debug("CIBA: push error result status " + pushErrorResponse.getStatus());
    }
}
