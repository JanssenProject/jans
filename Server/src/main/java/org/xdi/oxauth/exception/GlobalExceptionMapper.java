package org.xdi.oxauth.exception;

import org.slf4j.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * @author Yuriy Zabrovarnyy
 */
@ApplicationScoped
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<WebApplicationException> {

    @Inject
    private Logger log;

    @Override
    public Response toResponse(WebApplicationException ex) {
        log.trace("Handle WebApplicationException", ex);

        return ex.getResponse();
    }

}

