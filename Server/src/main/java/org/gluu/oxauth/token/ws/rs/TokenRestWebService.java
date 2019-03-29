/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.token.ws.rs;

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

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * Provides interface for token REST web services
 *
 * @author Javier Rojas Blum
 * @author Yuriy Zabrovarnyy
 */
@Api(value = "/", description = "Token Endpoint is used to obtain an Access Token, an ID Token, and optionally a Refresh Token. The RP (Client) sends a Token Request to the Token Endpoint to obtain a Token Response")
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

    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "invalid_request\n" +
                    " The request is missing a required parameter, includes an unsupported parameter value (other than grant type), repeats a parameter, includes multiple credentials,\n" +
                    " utilizes more than one mechanism for authenticating the client, or is otherwise malformed."),
            @ApiResponse(code = 400, message = "invalid_client\n" +
                    "Client authentication failed (e.g., unknown client, no client authentication included, or unsupported\n" +
                    "authentication method).  The authorization server MAY return an HTTP 401 (Unauthorized) status code to indicate\n" +
                    "which HTTP authentication schemes are supported.  If the client attempted to authenticate via the \"Authorization\"\n" +
                    "request header field, the authorization server MUST respond with an HTTP 401 (Unauthorized) status code and\n" +
                    "include the \"WWW-Authenticate\" response header field matching the authentication scheme used by the client."),
            @ApiResponse(code = 400, message = "invalid_grant\n" +
                    " The provided authorization grant (e.g., authorization code, resource owner credentials) or refresh token is\n" +
                    " invalid, expired, revoked, does not match the redirection URI used in the authorization request, or was issued to another client."),
            @ApiResponse(code = 400, message = "unauthorized_client\n" +
                    "The authenticated client is not authorized to use this authorization grant type."),
            @ApiResponse(code = 400, message = "unsupported_grant_type\n" +
                    "The authorization grant type is not supported by the authorization server."),
            @ApiResponse(code = 400, message = " invalid_scope\n" +
                    "The requested scope is invalid, unknown, malformed, or exceeds the scope granted by the resource owner.")
    })
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
            @FormParam("client_id")
            @ApiParam(value = "OAuth 2.0 Client Identifier valid at the Authorization Server.", required = false)
            String clientId,
            @FormParam("client_secret")
            @ApiParam(value = "The client secret.  The client MAY omit the parameter if the client secret is an empty string.", required = false)
            String clientSecret,
            @FormParam("code_verifier")
            @ApiParam(value = "The client's PKCE code verifier.", required = false)
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
            @Context HttpServletRequest request,
            @Context HttpServletResponse response,
            @Context SecurityContext sec);
}