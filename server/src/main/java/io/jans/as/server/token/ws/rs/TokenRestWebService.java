/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.token.ws.rs;

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
 * Provides interface for token REST web services
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 */
public interface TokenRestWebService {

    @POST
    @Path("/token")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestAccessToken(
            @FormParam("grant_type")
            String grantType,
            @FormParam("code")
            String code,
            @FormParam("redirect_uri")
            String redirectUri,
            @FormParam("username")
            String username,
            @FormParam("password")
            String password,
            @FormParam("scope")
            String scope,
            @FormParam("assertion")
            String assertion,
            @FormParam("refresh_token")
            String refreshToken,
            @FormParam("client_id")
            String clientId,
            @FormParam("client_secret")
            String clientSecret,
            @FormParam("code_verifier")
            String codeVerifier,
            @FormParam("ticket")
            String ticket,
            @FormParam("claim_token")
            String claimToken,
            @FormParam("claim_token_format")
            String claimTokenFormat,
            @FormParam("pct")
            String pctCode,
            @FormParam("rpt")
            String rptCode,
            @FormParam("auth_req_id")
            String authReqId,
            @FormParam("device_code")
            String deviceCode,
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SecurityContext sec);
}