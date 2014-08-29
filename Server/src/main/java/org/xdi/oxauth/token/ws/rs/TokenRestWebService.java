package org.xdi.oxauth.token.ws.rs;

import com.wordnik.swagger.annotations.Api;

import javax.servlet.http.HttpServletRequest;
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
 * @author Javier Rojas Blum Date: 09.21.2011
 */
@Path("/oxauth")
@Api(value = "/oxauth", description = "Token Endpoint is used to obtain an Access Token, an ID Token, and optionally a Refresh Token. The RP (Client) sends a Token Request to the Token Endpoint to obtain a Token Response")
public interface TokenRestWebService {

    @POST
    @Path("/token")
    @Produces({MediaType.APPLICATION_JSON})
    Response requestAccessToken(
            @FormParam("grant_type") String grantType,
            @FormParam("code") String code,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("username") String username,
            @FormParam("password") String password,
            @FormParam("scope") String scope,
            @FormParam("assertion") String assertion,
            @FormParam("refresh_token") String refreshToken,
            @FormParam("oxauth_exchange_token") String oxAuthExchangeToken,
            @FormParam("client_id") String clientId,
            @FormParam("client_secret") String clientSecret,
            @Context HttpServletRequest request,
            @Context SecurityContext sec);
}