/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.interception.CIBAEndUserNotificationInterception;
import org.gluu.oxauth.interception.CIBAEndUserNotificationInterceptionInterface;

import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Stateless
@Named
public class CIBAEndUserNotificationProxy implements CIBAEndUserNotificationInterceptionInterface {

    @Override
    @CIBAEndUserNotificationInterception
    public void notifyEndUser(String scope, String acrValues, String authReqId, String deviceRegistrationToken) {
    }
}