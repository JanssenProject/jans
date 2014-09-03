package org.xdi.oxauth.token.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;

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
    @ApiOperation(
            value = "To obtain an Access Token, an ID Token, and optionally a Refresh Token, the RP (Client) sends a Token Request to the Token Endpoint to obtain a Token Response",
            notes = "To obtain an Access Token, an ID Token, and optionally a Refresh Token, the RP (Client) sends a Token Request to the Token Endpoint to obtain a Token Response",
            response = Response.class,
            responseContainer = "JSON"
    )
    Response requestAccessToken(
            @FormParam("grant_type")
            @ApiParam(value = "Grant type value, one of these: authorization_code, implicit, password, client_credentials, refresh_token as described in OAuth 2.0 [RFC6749]", required = true)
            String grantType,
            @FormParam("code")
            @ApiParam(value = "Code which is returned by authorization endpoint. (For grant_type=authorization_code)", required = false)
            String code,
            @FormParam("redirect_uri")
            @ApiParam(value = "Redirection URI to which the response will be sent. This URI MUST exactly match one of the Redirection URI values for the Client pre-registered at the OpenID Provider", required = false)
            String redirectUri,
            @FormParam("username")
            @ApiParam(value = "End-User username.", required = false)
            String username,
            @FormParam("password")
            @ApiParam(value = "End-User password.", required = false)
            String password,
            @FormParam("scope")
            @ApiParam(value = "OpenID Connect requests MUST contain the openid scope value. If the openid scope value is not present, the behavior is entirely unspecified. Other scope values MAY be present. Scope values used that are not understood by an implementation SHOULD be ignored.", required = false)
            String scope,
            @FormParam("assertion")
            @ApiParam(value = "Assertion", required = false)
            String assertion,
            @FormParam("refresh_token")
            @ApiParam(value = "Refresh token", required = false)
            String refreshToken,
            @FormParam("oxauth_exchange_token")
            @ApiParam(value = "oxauth_exchange_token", required = false)
            String oxAuthExchangeToken,
            @FormParam("client_id")
            @ApiParam(value = "OAuth 2.0 Client Identifier valid at the Authorization Server.", required = false)
            String clientId,
            @FormParam("client_secret")
            @ApiParam(value = "The client secret.  The client MAY omit the parameter if the client secret is an empty string.", required = false)
            String clientSecret,
            @Context HttpServletRequest request,
            @Context SecurityContext sec);
}