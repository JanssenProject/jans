/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.exception;

import java.net.URI;

import javax.enterprise.inject.Vetoed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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