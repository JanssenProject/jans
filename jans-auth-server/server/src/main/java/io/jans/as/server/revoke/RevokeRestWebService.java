/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.revoke;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

/**
 * Provides interface for token revocation REST web services.
 * <p>
 * The Jans Auth authorization server's revocation policy acts as follows:
 * The revocation of a particular token cause the revocation of related
 * tokens and the underlying authorization grant.  If the particular
 * token is a refresh token, then the authorization server will also
 * invalidate all access tokens based on the same authorization grant.
 * If the token passed to the request is an access token, the server will
 * revoke the respective refresh token as well.
 *
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
public interface RevokeRestWebService {

    @POST
    @Path("/revoke")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestAccessToken(
            @FormParam("token") String token,
            @FormParam("token_type_hint") String tokenTypeHint,
            @FormParam("client_id") String clientId,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SecurityContext sec);
}
