/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.session.ws.rs;

import io.jans.as.model.session.EndSessionRequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * @author Javier Rojas Blum
 * @version August 9, 2017
 */
public interface EndSessionRestWebService {

    @GET
    @Path("/end_session")
    @Produces({MediaType.TEXT_PLAIN})
    Response requestEndSession(@QueryParam(EndSessionRequestParam.ID_TOKEN_HINT) String idTokenHint,
                               @QueryParam(EndSessionRequestParam.POST_LOGOUT_REDIRECT_URI) String postLogoutRedirectUri,
                               @QueryParam(EndSessionRequestParam.STATE) String state,
                               @QueryParam("sid") String sid,
                               @Context HttpServletRequest httpRequest,
                               @Context HttpServletResponse httpResponse,
                               @Context SecurityContext securityContext);

}