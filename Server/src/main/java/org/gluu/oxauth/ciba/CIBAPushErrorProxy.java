/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.interception.CIBAPushErrorInterception;
import org.gluu.oxauth.interception.CIBAPushErrorInterceptionInterface;
import org.gluu.oxauth.model.ciba.PushErrorResponseType;

import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version May 9, 2020
 */
@Stateless
@Named
public class CIBAPushErrorProxy implements CIBAPushErrorInterceptionInterface {

    @Override
    @CIBAPushErrorInterception
    public void pushError(String authReqId, String clientNotificationEndpoint, String clientNotificationToken,
                          PushErrorResponseType error, String errorDescription) {
    }
}
