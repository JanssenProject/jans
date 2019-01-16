/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2018, Gluu
 */

package org.gluu.oxauth.fido2.service;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.gluu.oxauth.fido2.exception.Fido2RPRuntimeException;
import org.slf4j.Logger;

/**
 * Fido2 RP resteasy exception handler
 *
 * @author Yuriy Movchan Date: 01/03/2019
 */
@ApplicationScoped
@Provider
public class Fido2RpExceptionHandler implements ExceptionMapper<Fido2RPRuntimeException> {

    @Inject
    private Logger log;

    @Override
    public Response toResponse(Fido2RPRuntimeException ex) {
        log.error("Handled Fido2 RP exception", ex);

        return Response.status(Status.BAD_REQUEST).entity(ex.getMessage()).build();
    }

}
