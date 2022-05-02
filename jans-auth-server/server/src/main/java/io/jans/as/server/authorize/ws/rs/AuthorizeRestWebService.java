/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.model.authorize.AuthorizeRequestParam;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;

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
 * @version October 7, 2019
 */
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
     * @param sessionId           session id
     * @param originHeaders
     * @param codeChallenge       PKCE code challenge
     * @param codeChallengeMethod PKCE code challenge method
     * @param authReqId           A unique identifier to identify the CIBA authentication request made by the Client.
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
    Response requestAuthorizationGet(
            @QueryParam("scope") String scope,
            @QueryParam("response_type") String responseType,
            @QueryParam("client_id") String clientId,
            @QueryParam("redirect_uri") String redirectUri,
            @QueryParam("state") String state,
            @QueryParam("response_mode") String responseMode,
            @QueryParam("nonce") String nonce,
            @QueryParam("display") String display,
            @QueryParam("prompt") String prompt,
            @QueryParam("max_age") Integer maxAge,
            @QueryParam("ui_locales") String uiLocales,
            @QueryParam("id_token_hint") String idTokenHint,
            @QueryParam("login_hint") String loginHint,
            @QueryParam("acr_values") String acrValues,
            @QueryParam("amr_values") String amrValues,
            @QueryParam("request") String request,
            @QueryParam("request_uri") String requestUri,
            @QueryParam("session_id") String sessionId,
            @QueryParam("origin_headers") String originHeaders,
            @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") String codeChallengeMethod,
            @QueryParam(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS) String customResponseHeaders,
            @QueryParam("claims") String claims,
            @QueryParam("auth_req_id") String authReqId,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext);

    @POST
    @Path("/authorize")
    @Produces({MediaType.TEXT_PLAIN})
    Response requestAuthorizationPost(
            @FormParam("scope") String scope,
            @FormParam("response_type") String responseType,
            @FormParam("client_id") String clientId,
            @FormParam("redirect_uri") String redirectUri,
            @FormParam("state") String state,
            @QueryParam("response_mode") String responseMode,
            @FormParam("nonce") String nonce,
            @FormParam("display") String display,
            @FormParam("prompt") String prompt,
            @FormParam("max_age") Integer maxAge,
            @FormParam("ui_locales") String uiLocales,
            @FormParam("id_token_hint") String idTokenHint,
            @FormParam("login_hint") String loginHint,
            @FormParam("acr_values") String acrValues,
            @FormParam("amr_values") String amrValues,
            @FormParam("request") String request,
            @FormParam("request_uri") String requestUri,
            @FormParam("session_id") String sessionId,
            @FormParam("origin_headers") String originHeaders,
            @QueryParam("code_challenge") String codeChallenge,
            @QueryParam("code_challenge_method") String codeChallengeMethod,
            @QueryParam(AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS) String customResponseHeaders,
            @QueryParam("claims") String claims,
            @QueryParam("auth_req_id") String authReqId,
            @Context HttpServletRequest httpRequest,
            @Context HttpServletResponse httpResponse,
            @Context SecurityContext securityContext);
}