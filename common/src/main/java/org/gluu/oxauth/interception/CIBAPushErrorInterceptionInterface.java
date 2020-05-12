/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.interception;

import org.gluu.oxauth.model.ciba.PushErrorResponseType;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
public interface CIBAPushErrorInterceptionInterface {

    void pushError(String authReqId, String clientNotificationEndpoint, String clientNotificationToken,
                   PushErrorResponseType error, String errorDescription);
}
