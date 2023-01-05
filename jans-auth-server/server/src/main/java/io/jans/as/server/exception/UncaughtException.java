/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.enterprise.inject.Vetoed;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.net.URI;

/**
 * Created by eugeniuparvan on 8/29/17.
 */
@Provider
@Vetoed
public class UncaughtException extends Throwable implements ExceptionMapper<Throwable> {

    private static final long serialVersionUID = 1L;

    private static final String ERROR_PAGE = "/error_service.htm";

    private final Logger log = LoggerFactory.getLogger(UncaughtException.class);

    @Context
    private HttpServletRequest httpRequest;

    @Context
    private UriInfo uriInfo;

    public UncaughtException() {
    }


    @Override
    public Response toResponse(Throwable exception) {
        try {
            if (exception instanceof WebApplicationException) {
                final Response response = ((WebApplicationException) exception).getResponse();
                if (response != null && response.getStatus() > 0) {
                    return response;
                }
            }
            log.error("Jersey error.", exception);
            return Response.temporaryRedirect(new URI(getRedirectURI())).build();
        } catch (Exception e) {
            log.error("Jersey error.", e);
            return Response.status(500).entity("Something bad happened. Please try again later!").type("text/plain").build();
        }
    }

    private String getRedirectURI() throws Exception {
        String baseUri = uriInfo.getBaseUri().toString();
        String contextPath = httpRequest.getContextPath();

        int startIndex = baseUri.indexOf(contextPath);
        if (startIndex == -1)
            throw new Exception("Can't build redirect URI");

        return baseUri.substring(0, startIndex + contextPath.length()) + ERROR_PAGE;
    }
}