/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.ciba;

import org.apache.logging.log4j.util.Strings;
import org.gluu.oxauth.model.error.DefaultErrorResponse;

import javax.ejb.Stateless;
import javax.inject.Named;
import javax.ws.rs.core.Response;

import static org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static org.gluu.oxauth.model.ciba.BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID;

/**
 * @author Javier Rojas Blum
 * @version October 7, 2019
 */
@Stateless
@Named
public class CIBADeviceRegistrationValidatorService {

    public DefaultErrorResponse validateParams(String idTokenHint, String deviceRegistrationToken) {
        if (Strings.isBlank(deviceRegistrationToken)) {
            DefaultErrorResponse errorResponse = new DefaultErrorResponse();
            errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            errorResponse.setType(INVALID_REQUEST);
            errorResponse.setReason("The device registration token cannot be blank.");

            return errorResponse;
        }

        if (Strings.isBlank(idTokenHint)) {
            DefaultErrorResponse errorResponse = new DefaultErrorResponse();
            errorResponse.setStatus(Response.Status.BAD_REQUEST.getStatusCode()); // 400
            errorResponse.setType(UNKNOWN_USER_ID);
            errorResponse.setReason("The id token hint cannot be blank.");

            return errorResponse;
        }

        return null;
    }
}