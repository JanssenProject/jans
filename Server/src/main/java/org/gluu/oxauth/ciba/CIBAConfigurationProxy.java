/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.gluu.oxauth.interception.CIBAConfigurationInterception;
import org.gluu.oxauth.interception.CIBAConfigurationInterceptionInterface;
import org.json.JSONObject;

import javax.ejb.Stateless;
import javax.inject.Named;

/**
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
@Stateless
@Named
public class CIBAConfigurationProxy implements CIBAConfigurationInterceptionInterface {

    @Override
    @CIBAConfigurationInterception
    public void processConfiguration(JSONObject jsonConfiguration) {
    }
}