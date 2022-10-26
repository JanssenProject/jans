/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.ciba;

import io.jans.as.model.error.DefaultErrorResponse;
import org.apache.logging.log4j.util.Strings;

import jakarta.ejb.Stateless;
import jakarta.inject.Named;
import jakarta.ws.rs.core.Response;

import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.UNKNOWN_USER_ID;

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