/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.interception.CIBARegisterClientResponseInterception;
import org.gluu.oxauth.interception.CIBARegisterClientResponseInterceptionInterface;
import org.gluu.oxauth.model.registration.Client;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Stateless
@Named
public class CIBARegisterClientResponseProxy implements CIBARegisterClientResponseInterceptionInterface {

    @Override
    @CIBARegisterClientResponseInterception
    public void updateResponse(JSONObject responseJsonObject, Client client) {
    }
}