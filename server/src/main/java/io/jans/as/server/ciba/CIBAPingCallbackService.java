/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jans.as.client.ciba.ping.PingCallbackClient;
import io.jans.as.client.ciba.ping.PingCallbackRequest;
import io.jans.as.client.ciba.ping.PingCallbackResponse;
import io.jans.as.model.configuration.AppConfiguration;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
@Stateless
@Named
public class CIBAPingCallbackService {

    private final static Logger log = LoggerFactory.getLogger(CIBAPingCallbackService.class);

    @Inject
    private AppConfiguration appConfiguration;

    public void pingCallback(String authReqId, String clientNotificationEndpoint, String clientNotificationToken) {
        PingCallbackRequest pingCallbackRequest = new PingCallbackRequest();

        pingCallbackRequest.setClientNotificationToken(clientNotificationToken);
        pingCallbackRequest.setAuthReqId(authReqId);

        PingCallbackClient pingCallbackClient = new PingCallbackClient(clientNotificationEndpoint, appConfiguration.getFapiCompatibility());
        pingCallbackClient.setRequest(pingCallbackRequest);
        PingCallbackResponse pingCallbackResponse = pingCallbackClient.exec();

        log.debug("CIBA: ping callback result status " + pingCallbackResponse.getStatus());
    }
}
