/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.interception;

import org.gluu.oxauth.model.error.DefaultErrorResponse;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
public interface CIBADeviceRegistrationValidatorInterceptionInterface {

    DefaultErrorResponse validateParams(String idTokenHint, String deviceRegistrationToken);
}