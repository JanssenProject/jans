/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.bcauthorize.ws.rs;

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
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public interface BackchannelAuthorizeRestWebService {

    @POST
    @Path("/bc-authorize")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestBackchannelAuthorizationPost(
            @FormParam("client_id") String clientId,
            @FormParam("scope") String scope,
            @FormParam("client_notification_token") String clientNotificationToken,
            @FormParam("acr_values") String acrValues,
            @FormParam("login_hint_token") String loginHintToken,
            @FormParam("id_token_hint") String idTokenHint,
            @FormParam("login_hint") String loginHint,
            @FormParam("binding_message") String bindingMessage,
            @FormParam("user_code") String userCode,
            @FormParam("requested_expiry") Integer requestedExpiry,
            @FormParam("request") String request,
            @FormParam("request_uri") String requestUri,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext
    );
}