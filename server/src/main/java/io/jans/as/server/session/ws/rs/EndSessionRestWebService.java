/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.session.ws.rs;

import io.jans.as.model.session.EndSessionRequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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