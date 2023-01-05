/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.client.ciba.push.PushErrorClient;
import io.jans.as.client.ciba.push.PushErrorRequest;
import io.jans.as.client.ciba.push.PushErrorResponse;
import io.jans.as.model.ciba.PushErrorResponseType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
@Stateless
@Named
public class CIBAPushErrorService {

    private final static Logger log = LoggerFactory.getLogger(CIBAPushErrorService.class);

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
