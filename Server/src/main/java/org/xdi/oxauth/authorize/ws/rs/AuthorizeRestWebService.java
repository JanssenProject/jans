/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.authorize.ws.rs;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.gluu.oxauth.model.authorize.AuthorizeRequestParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

/**
 * <p>
 * Provides interface for request authorization through REST web services.
 * </p>
 * <p>
 * An authorization grant is a credential representing the resource owner's
 * authorization (to access its protected resources) used by the client to
 * obtain an access token.
 * </p>
 *
 * @author Javier Rojas Blum
 * @version September 6, 2017
 */
@Api(value = "/", description = "The Authorization Endpoint performs Authentication of the End-User. This is done by sending the User Agent to the Authorization Server's Authorization Endpoint for Authentication and Authorization, using request parameters defined by OAuth 2.0 and additional parameters and parameter values defined by OpenID Connect.")
public interface AuthorizeRestWebService {

    /**
     * Requests authorization.
     *
     * @param scope               The scope of the access request.
     * @param responseType        The response type informs the authorization server of the desired response type:
     *                            <strong>code</strong>, <strong>token</strong>, <strong>id_token</strong>
     *                            a combination of them. The response type parameter is mandatory.
     * @param clientId            The client identifier.
     * @param redirectUri         Redirection URI
     * @param state               An opaque value used by the client to maintain state between
     *                            the request and callback. The authorization server includes
     *                            this value when redirecting the user-agent back to the client.
     *                            The parameter should be used for preventing cross-site request
     *                            forgery.
     * @param responseMode        Informs the Authorization Server of the mechanism to be used for returning parameters
     *                            from the Authorization Endpoint. This use of this parameter is NOT RECOMMENDED when the
     *                            Response Mode that would be requested is the default mode specified for the Response Type.
     * @param nonce               A string value used to associate a user agent session with an ID Token,
     *                            and to mitigate replay attacks.
     * @param display             An ASCII string value that specifies how the Authorization Server displays the
     *                            authentication page to the End-User.
     * @param prompt              A space delimited list of ASCII strings that can contain the values login, consent,
     *                            select_account, and none.
     * @param maxAge              Maximum Authentication Age. Specifies the allowable elapsed time in seconds since the
     *                            last time the End-User was actively authenticated.
     * @param uiLocales           End-User's preferred languages and scripts for the user interface, represented as a
     *                            space-separated list of BCP47 [RFC5646] language tag values, ordered by preference.
     * @param idTokenHint         Previously issued ID Token passed to the Authorization Server as a hint about the
     *                            End-User's current or past authenticated session with the Client.
     * @param loginHint           Hint to the Authorization Server about the login identifier the End-User might use to
     *                            log in (if necessary).
     * @param acrValues           Requested Authentication Context Class Reference values. Space-separated string that
     *                            specifies the acr values that the Authorization Server is being requested to use for
     *                            processing this Authentication Request, with the values appearing in order of preference.
     * @param amrValues           Requested Authentication Methods References. JSON array of strings that are identifiers
     *                            for authentication methods used in the authentication. For instance, values might indicate
     *                            that both password and OTP authentication methods were used. The definition of particular
     *                            values to be used in the amr Claim is beyond the scope of this specification.The amr value
     *                            is an array of case sensitive strings.
     * @param request             A JWT  encoded OpenID Request Object.
     * @param requestUri          An URL that points to an OpenID Request Object.
     * @param codeChallenge       PKCE code challenge
     * @param codeChallengeMethod PKCE code challenge method
     * @param requestSessionId    request session id
     * @param sessionId           session id
     * @param accessToken         access token
     * @param httpRequest         http request
     * @param securityContext     An injectable interface that provides access to security
     *                            related information.
     * @return <p>
     * When the responseType parameter is set to <strong>code</strong>:
     * </p>
     * <p>
     * If the resource owner grants the access request, the
     * authorization server issues an authorization code and delivers it
     * to the client by adding the following parameters to the query
     * component of the redirection URI using the
     * application/x-www-form-urlencoded format:
     * </p>
     * <dl>
     * <dt>code</dt>
     * <dd>
     * The authorization code generated by the authorization server.</dd>
     * <dt>state</dt>
     * <dd>
     * If the state parameter was present in the client authorization
     * request. The exact value received from the client.</dd>
     * </dl>
     * <p/>
     * <p>
     * When the responseType parameter is set to <strong>token</strong>:
     * </p>
     * <p>
     * If the resource owner grants the access request, the
     * authorization server issues an access token and delivers it to
     * the client by adding the following parameters to the fragment
     * component of the redirection URI using the
     * application/x-www-form-urlencoded format.
     * </p>
     * <dl>
     * <dt>access_token</dt>
     * <dd>The access token issued by the authorization server.</dd>
     * <dt>token_type</dt>
     * <dd>The type of the token issued. Value is case insensitive.</dd>
     * <dt>expires_in</dt>
     * <dd>The lifetime in seconds of the access token. For example, the
     * value 3600 denotes that the access token will expire in one hour
     * from the time the response was generated.</dd>
     * <dt>scope</dt>
     * <dd>The scope of the access token.</dd>
     * <dt>state</dt>
     * <dd>If the state parameter was present in the client
     * authorization request. The exact value received from the client.</dd>
     * </dl>
     */
    @GET
    @Path("/authorize")
    @Produces({MediaType.TEXT_PLAIN})
    @ApiOperation(
            value = "Performs authorization.",
            notes = "The Authorization Endpoint performs Authentication of the End-User.",
            response = Response.class,
            responseContainer = "JSON"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 302, message = "interaction_required\n" +
                    "    The Authorization Server requires End-User interaction of some form to proceed. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User interaction. "),
            @ApiResponse(code = 302, message = "login_required\n" +
                    "    The Authorization Server requires End-User authentication. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User authentication. "),
            @ApiResponse(code = 302, message = "account_selection_required\n" +
                    "    The End-User is REQUIRED to select a session at the Authorization Server. The End-User MAY be authenticated at the Authorization Server with different associated accounts, but the End-User did not select a session. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface to prompt for a session to use. "),
            @ApiResponse(code = 302, message = "consent_required\n" +
                    "    The Authorization Server requires End-User consent. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User consent. "),
            @ApiResponse(code = 302, message = "invalid_request_uri\n" +
                    "    The request_uri in the Authorization Request returns an error or contains invalid data. "),
            @ApiResponse(code = 302, message = "invalid_request_object\n" +
                    "    The request parameter contains an invalid Request Object. "),
            @ApiResponse(code = 302, message = "request_not_supported\n" +
                    "    The OP does not support use of the request parameter"),
            @ApiResponse(code = 302, message = "request_uri_not_supported\n" +
                    "    The OP does not support use of the request_uri parameter"),
            @ApiResponse(code = 302, message = "registration_not_supported\n" +
                    "    The OP does not support use of the registration parameter")
    })
    Response requestAuthorizationGet(
            @QueryParam("scope")
            @ApiParam(value = "OpenID Connect requests MUST contain the openid scope value. If the openid scope value is not present, the behavior is entirely unspecified. Other scope values MAY be present. Scope values used that are not understood by an implementation SHOULD be ignored.", required = true)
                    String scope,
            @QueryParam("response_type")
            @ApiParam(value = "OAuth 2.0 Response Type value that determines the authorization processing flow to be used, including what parameters are returned from the endpoints used. When using the Authorization Code Flow, this value is code. ", required = true)
                    String responseType,
            @QueryParam("client_id")
            @ApiParam(value = "OAuth 2.0 Client Identifier valid at the Authorization Server.", required = true)
                    String clientId,
            @QueryParam("redirect_uri")
            @ApiParam(value = "Redirection URI to which the response will be sent. This URI MUST exactly match one of the Redirection URI values for the Client pre-registered at the OpenID Provider", required = true)
                    String redirectUri,
            @QueryParam("state")
            @ApiParam(value = "Opaque value used to maintain state between the request and the callback. Typically, Cross-Site Request Forgery (CSRF, XSRF) mitigation is done by cryptographically binding the value of this parameter with a browser cookie. ", required = false)
                    String state,
            @QueryParam("response_mode")
            @ApiParam(value = "Informs the Authorization Server of the mechanism to be used for returning parameters from the Authorization Endpoint. This use of this parameter is NOT RECOMMENDED when the Response Mode that would be requested is the default mode specified for the Response Type. ", required = false)
                    String responseMode,
            @QueryParam("nonce")
            @ApiParam(value = "String value used to associate a Client session with an ID Token, and to mitigate replay attacks. The value is passed through unmodified from the Authorization Request to the ID Token. Sufficient entropy MUST be present in the nonce values used to prevent attackers from guessing values.", required = false)
                    String nonce,
            @QueryParam("display")
            @ApiParam(value = "ASCII string value that specifies how the Authorization Server displays the authentication and consent user interface pages to the End-User. The defined values are: page, popup, touch, wap", required = false)
                    String display,
            @QueryParam("prompt")
            @ApiParam(value = "Space delimited, case sensitive list of ASCII string values that specifies whether the Authorization Server prompts the End-User for reauthentication and consent. The defined values are: none, login, consent, select_account", required = false)
                    String prompt,
            @QueryParam("max_age")
            @ApiParam(value = "Maximum Authentication Age. Specifies the allowable elapsed time in seconds since the last time the End-User was actively authenticated by the OP. If the elapsed time is greater than this value, the OP MUST attempt to actively re-authenticate the End-User. (The max_age request parameter corresponds to the OpenID 2.0 PAPE [OpenID.PAPE] max_auth_age request parameter.) When max_age is used, the ID Token returned MUST include an auth_time Claim Value. ", required = false)
                    Integer maxAge,
            @QueryParam("ui_locales")
            @ApiParam(value = "End-User's preferred languages and scripts for the user interface, represented as a space-separated list of BCP47 [RFC5646] language tag values, ordered by preference. For instance, the value \"fr-CA fr en\" represents a preference for French as spoken in Canada, then French (without a region designation), followed by English (without a region designation). An error SHOULD NOT result if some or all of the requested locales are not supported by the OpenID Provider. ", required = false)
                    String uiLocales,
            @QueryParam("id_token_hint")
            @ApiParam(value = "ID Token previously issued by the Authorization Server being passed as a hint about the End-User's current or past authenticated session with the Client. If the End-User identified by the ID Token is logged in or is logged in by the request, then the Authorization Server returns a positive response; otherwise, it SHOULD return an error, such as login_required. When possible, an id_token_hint SHOULD be present when prompt=none is used and an invalid_request error MAY be returned if it is not; however, the server SHOULD respond successfully when possible, even if it is not present. The Authorization Server need not be listed as an audience of the ID Token when it is used as an id_token_hint value. ", required = false)
                    String idTokenHint,
            @QueryParam("login_hint")
            @ApiParam(value = "Hint to the Authorization Server about the login identifier the End-User might use to log in (if necessary). This hint can be used by an RP if it first asks the End-User for their e-mail address (or other identifier) and then wants to pass that value as a hint to the discovered authorization service. It is RECOMMENDED that the hint value match the value used for discovery. This value MAY also be a phone number in the format specified for the phone_number Claim. The use of this parameter is left to the OP's discretion. ", required = false)
                    String loginHint,
            @QueryParam("acr_values")
            @ApiParam(value = "Requested Authentication Context Class Reference values. Space-separated string that specifies the acr values that the Authorization Server is being requested to use for processing this Authentication Request, with the values appearing in order of preference. The Authentication Context Class satisfied by the authentication performed is returned as the acr Claim Value, as specified in Section 2. The acr Claim is requested as a Voluntary Claim by this parameter. ", required = false)
                    String acrValues,
            @QueryParam("amr_values")
            @ApiParam(value = "AMR Values", required = false)
                    String amrValues,
            @QueryParam("request")
            @ApiParam(value = "This parameter enables OpenID Connect requests to be passed in a single, self-contained parameter and to be optionally signed and/or encrypted. The parameter value is a Request Object value, as specified in Section 6.1. It represents the request as a JWT whose Claims are the request parameters.", required = false)
                    String request,
            @QueryParam("request_uri")
            @ApiParam(value = "This parameter enables OpenID Connect requests to be passed by reference, rather than by value. The request_uri value is a URL using the https scheme referencing a resource containing a Request Object value, which is a JWT containing the request parameters. ", required = false)
                    String requestUri,
            @QueryParam("request_session_id")
            @ApiParam(value = "Request session id", required = false)
                    String requestSessionId,
            @QueryParam("session_id")
            @ApiParam(value = "Session id of this call", required = false)
                    String sessionId,
            @QueryParam("access_token")
            @ApiParam(value = "Access token", required = false)
                    String accessToken,
            @QueryParam("origin_headers")
            @ApiParam(value = "Origin headers. Used in custom workflows.", required = false)
                    String originHeaders,
            @QueryParam("code_challenge")
            @ApiParam(value = "PKCE code challenge.", required = false)
                    String codeChallenge,
            @QueryParam("code_challenge_method")
            @ApiParam(value = "PKCE code challenge method.", required = false)
                    String codeChallengeMethod,
            @QueryParam(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS)
            @ApiParam(value = "Custom Response Headers.", required = false)
                    String customResponseHeaders,
            @QueryParam("claims")
            @ApiParam(value = "Requested Claims.", required = false)
                    String claims,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext);

    @POST
    @Path("/authorize")
    @Produces({MediaType.TEXT_PLAIN})
    @ApiOperation(
            value = "Performs authorization.",
            notes = "The Authorization Endpoint performs Authentication of the End-User.",
            response = Response.class,
            responseContainer = "JSON"
    )
    @ApiResponses(value = {
            @ApiResponse(code = 302, message = "interaction_required\n" +
                    "    The Authorization Server requires End-User interaction of some form to proceed. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User interaction. "),
            @ApiResponse(code = 302, message = "login_required\n" +
                    "    The Authorization Server requires End-User authentication. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User authentication. "),
            @ApiResponse(code = 302, message = "account_selection_required\n" +
                    "    The End-User is REQUIRED to select a session at the Authorization Server. The End-User MAY be authenticated at the Authorization Server with different associated accounts, but the End-User did not select a session. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface to prompt for a session to use. "),
            @ApiResponse(code = 302, message = "consent_required\n" +
                    "    The Authorization Server requires End-User consent. This error MAY be returned when the prompt parameter value in the Authentication Request is none, but the Authentication Request cannot be completed without displaying a user interface for End-User consent. "),
            @ApiResponse(code = 302, message = "invalid_request_uri\n" +
                    "    The request_uri in the Authorization Request returns an error or contains invalid data. "),
            @ApiResponse(code = 302, message = "invalid_request_object\n" +
                    "    The request parameter contains an invalid Request Object. "),
            @ApiResponse(code = 302, message = "request_not_supported\n" +
                    "    The OP does not support use of the request parameter"),
            @ApiResponse(code = 302, message = "request_uri_not_supported\n" +
                    "    The OP does not support use of the request_uri parameter"),
            @ApiResponse(code = 302, message = "registration_not_supported\n" +
                    "    The OP does not support use of the registration parameter")
    })
    Response requestAuthorizationPost(
            @FormParam("scope")
            @ApiParam(value = "OpenID Connect requests MUST contain the openid scope value. If the openid scope value is not present, the behavior is entirely unspecified. Other scope values MAY be present. Scope values used that are not understood by an implementation SHOULD be ignored.", required = true)
                    String scope,
            @FormParam("response_type")
            @ApiParam(value = "OAuth 2.0 Response Type value that determines the authorization processing flow to be used, including what parameters are returned from the endpoints used. When using the Authorization Code Flow, this value is code. ", required = true)
                    String responseType,
            @FormParam("client_id")
            @ApiParam(value = "OAuth 2.0 Client Identifier valid at the Authorization Server. ", required = true)
                    String clientId,
            @FormParam("redirect_uri")
            @ApiParam(value = "Redirection URI to which the response will be sent. This URI MUST exactly match one of the Redirection URI values for the Client pre-registered at the OpenID Provider", required = true)
                    String redirectUri,
            @FormParam("state")
            @ApiParam(value = "Opaque value used to maintain state between the request and the callback. Typically, Cross-Site Request Forgery (CSRF, XSRF) mitigation is done by cryptographically binding the value of this parameter with a browser cookie. ", required = false)
                    String state,
            @QueryParam("response_mode")
            @ApiParam(value = "Informs the Authorization Server of the mechanism to be used for returning parameters from the Authorization Endpoint. This use of this parameter is NOT RECOMMENDED when the Response Mode that would be requested is the default mode specified for the Response Type. ", required = false)
                    String responseMode,
            @FormParam("nonce")
            @ApiParam(value = "String value used to associate a Client session with an ID Token, and to mitigate replay attacks. The value is passed through unmodified from the Authorization Request to the ID Token. Sufficient entropy MUST be present in the nonce values used to prevent attackers from guessing values.", required = false)
                    String nonce,
            @FormParam("display")
            @ApiParam(value = "ASCII string value that specifies how the Authorization Server displays the authentication and consent user interface pages to the End-User. The defined values are: page, popup, touch, wap", required = false)
                    String display,
            @FormParam("prompt")
            @ApiParam(value = "Space delimited, case sensitive list of ASCII string values that specifies whether the Authorization Server prompts the End-User for reauthentication and consent. The defined values are: none, login, consent, select_account", required = false)
                    String prompt,
            @FormParam("max_age")
            @ApiParam(value = "Maximum Authentication Age. Specifies the allowable elapsed time in seconds since the last time the End-User was actively authenticated by the OP. If the elapsed time is greater than this value, the OP MUST attempt to actively re-authenticate the End-User. (The max_age request parameter corresponds to the OpenID 2.0 PAPE [OpenID.PAPE] max_auth_age request parameter.) When max_age is used, the ID Token returned MUST include an auth_time Claim Value. ", required = false)
                    Integer maxAge,
            @FormParam("ui_locales")
            @ApiParam(value = "End-User's preferred languages and scripts for the user interface, represented as a space-separated list of BCP47 [RFC5646] language tag values, ordered by preference. For instance, the value \"fr-CA fr en\" represents a preference for French as spoken in Canada, then French (without a region designation), followed by English (without a region designation). An error SHOULD NOT result if some or all of the requested locales are not supported by the OpenID Provider. ", required = false)
                    String uiLocales,
            @FormParam("id_token_hint")
            @ApiParam(value = "ID Token previously issued by the Authorization Server being passed as a hint about the End-User's current or past authenticated session with the Client. If the End-User identified by the ID Token is logged in or is logged in by the request, then the Authorization Server returns a positive response; otherwise, it SHOULD return an error, such as login_required. When possible, an id_token_hint SHOULD be present when prompt=none is used and an invalid_request error MAY be returned if it is not; however, the server SHOULD respond successfully when possible, even if it is not present. The Authorization Server need not be listed as an audience of the ID Token when it is used as an id_token_hint value. ", required = false)
                    String idTokenHint,
            @FormParam("login_hint")
            @ApiParam(value = "Hint to the Authorization Server about the login identifier the End-User might use to log in (if necessary). This hint can be used by an RP if it first asks the End-User for their e-mail address (or other identifier) and then wants to pass that value as a hint to the discovered authorization service. It is RECOMMENDED that the hint value match the value used for discovery. This value MAY also be a phone number in the format specified for the phone_number Claim. The use of this parameter is left to the OP's discretion. ", required = false)
                    String loginHint,
            @FormParam("acr_values")
            @ApiParam(value = "Requested Authentication Context Class Reference values. Space-separated string that specifies the acr values that the Authorization Server is being requested to use for processing this Authentication Request, with the values appearing in order of preference. The Authentication Context Class satisfied by the authentication performed is returned as the acr Claim Value, as specified in Section 2. The acr Claim is requested as a Voluntary Claim by this parameter. ", required = false)
                    String acrValues,
            @FormParam("amr_values")
            @ApiParam(value = "AMR Values", required = false)
                    String amrValues,
            @FormParam("request")
            @ApiParam(value = "This parameter enables OpenID Connect requests to be passed in a single, self-contained parameter and to be optionally signed and/or encrypted. The parameter value is a Request Object value, as specified in Section 6.1. It represents the request as a JWT whose Claims are the request parameters.", required = false)
                    String request,
            @FormParam("request_uri")
            @ApiParam(value = "This parameter enables OpenID Connect requests to be passed by reference, rather than by value. The request_uri value is a URL using the https scheme referencing a resource containing a Request Object value, which is a JWT containing the request parameters. ", required = false)
                    String requestUri,
            @FormParam("request_session_id")
            @ApiParam(value = "Request session id", required = false)
                    String requestSessionId,
            @FormParam("session_id")
            @ApiParam(value = "Session id of this call", required = false)
                    String sessionId,
            @FormParam("access_token")
            @ApiParam(value = "Access token", required = false)
                    String accessToken,
            @FormParam("origin_headers")
            @ApiParam(value = "Origin headers. Used in custom workflows.", required = false)
                    String originHeaders,
            @QueryParam("code_challenge")
            @ApiParam(value = "PKCE code challenge.", required = false)
                    String codeChallenge,
            @QueryParam("code_challenge_method")
            @ApiParam(value = "PKCE code challenge method.", required = false)
                    String codeChallengeMethod,
            @QueryParam(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS)
            @ApiParam(value = "Custom Response Headers.", required = false)
                    String customResponseHeaders,
            @QueryParam("claims")
            @ApiParam(value = "Requested Claims.", required = false)
                    String claims,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext);
}