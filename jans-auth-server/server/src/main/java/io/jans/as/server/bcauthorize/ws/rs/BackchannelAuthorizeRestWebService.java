/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.bcauthorize.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

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