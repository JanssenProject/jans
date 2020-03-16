/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.interception;

/**
 * @author Javier Rojas Blum
 * @version September 4, 2019
 */
public interface CIBAPushTokenDeliveryInterceptionInterface {

    void pushTokenDelivery(String authReqId, String clientNotificationEndpoint, String clientNotificationToken,
                           String accessToken, String refreshToken, String idToken, Integer expiresIn);
}
