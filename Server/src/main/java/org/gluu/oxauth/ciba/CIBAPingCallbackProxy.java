/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.interception.CIBAPingCallbackInterception;
import org.gluu.oxauth.interception.CIBAPingCallbackInterceptionInterface;

import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version December 21, 2019
 */
@Stateless
@Named
public class CIBAPingCallbackProxy implements CIBAPingCallbackInterceptionInterface {

    @Override
    @CIBAPingCallbackInterception
    public void pingCallback(String authReqId, String clientNotificationEndpoint, String clientNotificationToken) {
    }
}
