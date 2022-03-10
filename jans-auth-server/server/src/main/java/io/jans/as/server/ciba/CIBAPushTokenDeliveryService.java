/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.client.ciba.push.PushTokenDeliveryClient;
import io.jans.as.client.ciba.push.PushTokenDeliveryRequest;
import io.jans.as.client.ciba.push.PushTokenDeliveryResponse;
import io.jans.as.model.common.TokenType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
@Stateless
@Named
public class CIBAPushTokenDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(CIBAPushTokenDeliveryService.class);

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

        log.debug("CIBA: push token delivery result status {}", pushTokenDeliveryResponse.getStatus());
    }
}
