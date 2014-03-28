package org.xdi.oxauth.authorize.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientRequest;
import org.jboss.resteasy.client.ClientResponse;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.xdi.oxauth.auth.Authenticator;
import org.xdi.oxauth.model.authorize.*;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.config.ClaimMappingConfiguration;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.InvalidClaimException;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.jwt.JwtClaimName;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.JwtUtil;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.*;
import org.xdi.oxauth.util.QueryStringDecoder;
import org.xdi.oxauth.util.RedirectUtil;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;
import org.xdi.util.security.StringEncrypter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.SecurityContext;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.security.SignatureException;
import java.util.*;

import static org.xdi.oxauth.model.util.StringUtils.implode;

/**
 * Implementation for request authorization through REST web services.
 *
 * @author Javier Rojas Blum Date: 09.20.2011
 */
@Name("requestAuthorizationRestWebService")
public class AuthorizeRestWebServiceImpl implements AuthorizeRestWebService {

    @Logger
    private Log log;

    @In
    private AuthenticationService authenticationService;

    @In
    private ErrorResponseFactory errorResponseFactory;

    @In
    private RedirectionUriService redirectionUriService;

    @In
    private AuthorizationGrantList authorizationGrantList;

    @In
    private ClientService clientService;

    @In
    private UserService userService;

    @In
    private UserGroupService userGroupService;

    @In
    private FederationDataService federationDataService;

    @In
    private ScopeService scopeService;

    @In
    private AttributeService attributeService;

    @In
    private Identity identity;

    @In
    private AuthenticationFilterService authenticationFilterService;

    @In
    private SessionIdService sessionIdService;

    @In
    private SessionId sessionUser;

    @Override
    public Response requestAuthorizationGet(
            String scope, String responseType, String clientId, String redirectUri, String state, String nonce,
            String display, String prompt, Integer maxAge, String uiLocales, String idTokenHint, String loginHint,
            String acrValues, String request, String requestUri, String requestSessionId, String sessionId,
            String accessToken, String authLevel, String authMode, HttpServletRequest httpRequest, SecurityContext securityContext) {
        return requestAuthorization(scope, responseType, clientId, redirectUri, state, nonce, display, prompt, maxAge,
                uiLocales, idTokenHint, loginHint, acrValues,
                request, requestUri, requestSessionId, sessionId, accessToken, authLevel, authMode, HttpMethod.GET,
                httpRequest, securityContext);
    }

    @Override
    public Response requestAuthorizationPost(
            String scope, String responseType, String clientId, String redirectUri, String state, String nonce,
            String display, String prompt, Integer maxAge, String uiLocales, String idTokenHint, String loginHint,
            String acrValues, String request, String requestUri, String requestSessionId, String sessionId,
            String accessToken, String authLevel, String authMode, HttpServletRequest httpRequest, SecurityContext securityContext) {
        return requestAuthorization(scope, responseType, clientId, redirectUri, state, nonce, display, prompt, maxAge,
                uiLocales, idTokenHint, loginHint, acrValues, request, requestUri, requestSessionId, sessionId,
                accessToken, authLevel, authMode, HttpMethod.POST, httpRequest, securityContext);
    }

    public Response requestAuthorization(
            String scope, String responseType, String clientId, String redirectUri, String state, String nonce,
            String display, String prompt, Integer maxAge, String uiLocalesStr, String idTokenHint, String loginHint,
            String acrValuesStr, String request, String requestUri, String requestSessionId, String sessionId,
            String accessToken, String authLevel, String authMode, String method, HttpServletRequest httpRequest,
            SecurityContext securityContext) {
        scope = ServerUtil.urlDecode(scope); // it may be encoded in uma case

        // ATTENTION : please do not add more parameter in this debug method because it will not work with Seam 2.2.2.Final ,
        // there is limit of 10 parameters (hardcoded), see: org.jboss.seam.core.Interpolator#interpolate
        log.debug("Attempting to request authorization: "
                + "responseType = {0}, clientId = {1}, scope = {2}, redirectUri = {3}, nonce = {4}, "
                + "state = {5}, request = {6}, isSecure = {7}, requestSessionId = {8}, sessionId = {9}",
                responseType, clientId, scope, redirectUri, nonce,
                state, request, securityContext.isSecure(), requestSessionId, sessionId);

        log.debug("Attempting to request authorization: "
                + "authLevel = {0}, authMode = {1}", authLevel, authMode);

        ResponseBuilder builder = Response.ok();

        List<String> uiLocales = null;
        if (StringUtils.isNotBlank(uiLocalesStr)) {
            uiLocales = Util.splittedStringAsList(uiLocalesStr, " ");
        }

        List<ResponseType> responseTypes = ResponseType.fromString(responseType, " ");
        List<Prompt> prompts = Prompt.fromString(prompt, " ");
        List<String> scopes = Util.splittedStringAsList(scope, " ");
        List<String> acrValues = Util.splittedStringAsList(acrValuesStr, " ");

        User user = sessionUser != null && StringUtils.isNotBlank(sessionUser.getUserDn()) ?
                userService.getUserByDn(sessionUser.getUserDn()) : null;

        try {
            if (!AuthorizeParamsValidator.validateParams(responseType, clientId, prompts, nonce, request, requestUri)) {
                if (clientId != null && redirectUri != null && redirectionUriService.validateRedirectionUri(clientId, redirectUri) != null) {
                    // In this case the error response must be sent in a fragment component
                    StringBuilder sb = new StringBuilder(redirectUri);

                    if (responseTypes.contains(ResponseType.TOKEN) || responseTypes.contains(ResponseType.ID_TOKEN)) {
                        if (!sb.toString().contains("#")) {
                            sb.append("#");
                        } else {
                            sb.append("&");
                        }
                    } else {
                        if (!sb.toString().contains("?")) {
                            sb.append("?");
                        } else {
                            sb.append("&");
                        }
                    }
                    sb.append(errorResponseFactory.getErrorAsQueryString(
                            AuthorizeErrorResponseType.INVALID_REQUEST, state));

                    builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                } else {
                    builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                    builder.entity(errorResponseFactory.getErrorAsJson(
                            AuthorizeErrorResponseType.INVALID_REQUEST, state));
                }
            } else {
                Client client = clientService.getClient(clientId);
                JwtAuthorizationRequest jwtAuthorizationRequest = null;

                if (client != null) {
                    // Validate redirectUri
                    redirectUri = redirectionUriService.validateRedirectionUri(clientId, redirectUri);
                    boolean validRedirectUri = redirectUri != null;

                    if (AuthorizeParamsValidator.validateResponseTypes(responseTypes, client)) {
                        if (validRedirectUri) {

                            if (ConfigurationFactory.getConfiguration().getFederationEnabled()) {
                                if (!federationDataService.hasAnyActiveTrust(client)) {
                                    log.debug("Forbid authorization. Client is not in any trust relationship however federation is enabled for server. Client id: {0}, client redirectUris: {1}",
                                            client.getClientId(), client.getRedirectUris());
                                    return error(Response.Status.UNAUTHORIZED, AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state).build();
                                }
                            }

                            if (StringUtils.isNotBlank(accessToken)) {
                                AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

                                if (authorizationGrant == null) {
                                    StringBuilder sb = new StringBuilder(redirectUri);

                                    sb.append("#").append(errorResponseFactory.getErrorAsQueryString(
                                            AuthorizeErrorResponseType.ACCESS_DENIED, state));

                                    builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                    return builder.build();
                                } else {
                                    user = userService.getUser(authorizationGrant.getUserId());
                                    sessionUser = sessionIdService.generateSessionId(user.getDn());
                                    sessionUser.setAuthenticationTime(new Date());
                                    sessionIdService.updateSessionLastUsedDate(sessionUser);
                                }
                            }

                            if (StringUtils.isNotBlank(requestUri)) {
                                boolean validRequestUri = false;
                                try {
                                    URI reqUri = new URI(requestUri);
                                    String reqUriHash = reqUri.getFragment();
                                    String reqUriWithoutFragment = reqUri.getScheme() + ":" + reqUri.getSchemeSpecificPart();

                                    ClientRequest clientRequest = new ClientRequest(reqUriWithoutFragment);
                                    clientRequest.setHttpMethod(HttpMethod.GET);

                                    ClientResponse<String> clientResponse = clientRequest.get(String.class);
                                    int status = clientResponse.getStatus();

                                    if (status == 200) {
                                        request = clientResponse.getEntity(String.class);

                                        if (StringUtils.isBlank(reqUriHash)) {
                                            validRequestUri = true;
                                        } else {
                                            String hash = JwtUtil.base64urlencode(JwtUtil.getMessageDigestSHA256(request));
                                            validRequestUri = StringUtils.equals(reqUriHash, hash);
                                        }
                                    }

                                    if (validRequestUri) {
                                        requestUri = null;
                                    } else {
                                        StringBuilder sb = new StringBuilder(redirectUri);

                                        sb.append("#").append(errorResponseFactory.getErrorAsQueryString(
                                                AuthorizeErrorResponseType.INVALID_REQUEST_URI, state));

                                        builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                        return builder.build();
                                    }
                                } catch (URISyntaxException e) {
                                    log.error(e.getMessage(), e);
                                } catch (UnknownHostException e) {
                                    log.error(e.getMessage(), e);
                                } catch (ConnectException e) {
                                    log.error(e.getMessage(), e);
                                } catch (Exception e) {
                                    log.error(e.getMessage(), e);
                                }
                            }

                            boolean invalidOpenidRequestObject = false;
                            if (StringUtils.isNotBlank(request)) {
                                try {
                                    jwtAuthorizationRequest = new JwtAuthorizationRequest(request, client);

                                    if (!jwtAuthorizationRequest.getResponseTypes().containsAll(responseTypes)
                                            || !responseTypes.containsAll(jwtAuthorizationRequest.getResponseTypes())) {
                                        throw new InvalidJwtException("The responseType parameter is not the same in the JWT");
                                    } else if (jwtAuthorizationRequest.getClientId() != null
                                            && !jwtAuthorizationRequest.getClientId().equals(clientId)) {
                                        throw new InvalidJwtException("The clientId parameter is not the same in the JWT");
                                    } else if (!jwtAuthorizationRequest.getScopes().containsAll(scopes)
                                            || !scopes.containsAll(jwtAuthorizationRequest.getScopes())) {
                                        throw new InvalidJwtException("The scope parameter is not the same in the JWT");
                                    } else if (jwtAuthorizationRequest.getRedirectUri() != null
                                            && !jwtAuthorizationRequest.getRedirectUri().equals(redirectUri)) {
                                        throw new InvalidJwtException("The redirectUri parameter is not the same in the JWT");
                                    } else if (jwtAuthorizationRequest.getState() != null && StringUtils.isNotBlank(state)
                                            && !jwtAuthorizationRequest.getState().equals(state)) {
                                        throw new InvalidJwtException("The state parameter is not the same in the JWT");
                                    } else if (jwtAuthorizationRequest.getNonce() != null && StringUtils.isNotBlank(nonce)
                                            && !jwtAuthorizationRequest.getNonce().equals(nonce)) {
                                        throw new InvalidJwtException("The nonce parameter is not the same in the JWT");
                                    } else if (jwtAuthorizationRequest.getDisplay() != null && StringUtils.isNotBlank(display)
                                            && !jwtAuthorizationRequest.getDisplay().getParamName().equals(display)) {
                                        throw new InvalidJwtException("The display parameter is not the same in the JWT");
                                    } else if (!jwtAuthorizationRequest.getPrompts().isEmpty() && !prompts.isEmpty()
                                            && !jwtAuthorizationRequest.getPrompts().containsAll(prompts)) {
                                        throw new InvalidJwtException("The prompt parameter is not the same in the JWT");
                                    } else if (jwtAuthorizationRequest.getIdTokenMember().getMaxAge() != null && maxAge != null
                                            && !jwtAuthorizationRequest.getIdTokenMember().getMaxAge().equals(maxAge)) {
                                        throw new InvalidJwtException("The maxAge parameter is not the same in the JWT");
                                    }
                                } catch (InvalidJwtException e) {
                                    invalidOpenidRequestObject = true;
                                    log.debug("Invalid JWT authorization request. Exception = {0}, Message = {1}", e,
                                            e.getClass().getName(), e.getMessage());
                                } catch (Exception e) {
                                    invalidOpenidRequestObject = true;
                                    log.debug("Invalid JWT authorization request. Exception = {0}, Message = {1}", e,
                                            e.getClass().getName(), e.getMessage());
                                }
                            }
                            if (invalidOpenidRequestObject) {
                                StringBuilder sb = new StringBuilder(redirectUri);

                                sb.append("#").append(errorResponseFactory.getErrorAsQueryString(
                                        AuthorizeErrorResponseType.INVALID_OPENID_REQUEST_OBJECT, state));

                                builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                            } else {
                                StringBuilder sb = new StringBuilder(redirectUri);

                                AuthorizationGrant authorizationGrant = null;

                                if (responseTypes.contains(ResponseType.TOKEN) || responseTypes.contains(ResponseType.ID_TOKEN)) {
                                    if (!sb.toString().contains("#")) {
                                        sb.append("#");
                                    } else {
                                        sb.append("&");
                                    }
                                } else {
                                    if (!sb.toString().contains("?")) {
                                        sb.append("?");
                                    } else {
                                        sb.append("&");
                                    }
                                }

                                if (jwtAuthorizationRequest != null && jwtAuthorizationRequest.getIdTokenMember() != null) {
                                    Claim userIdClaim = jwtAuthorizationRequest.getIdTokenMember().getClaim(JwtClaimName.SUBJECT_IDENTIFIER);
                                    if (userIdClaim != null && userIdClaim.getClaimValue() != null
                                            && userIdClaim.getClaimValue().getValue() != null) {
                                        String userIdClaimValue = userIdClaim.getClaimValue().getValue();

                                        if (user != null) {
                                            String userId = user.getUserId();

                                            if (!userId.equalsIgnoreCase(userIdClaimValue)) {
                                                sb.append(errorResponseFactory.getErrorAsQueryString(
                                                        AuthorizeErrorResponseType.USER_MISMATCHED, state));

                                                builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                                return builder.build();
                                            }
                                        }
                                    }
                                }

                                if (user == null) {
                                    identity.logout();
                                    if (prompts.contains(Prompt.NONE)) {
                                        if (authenticationFilterService.isEnabled()) {
                                            Map<String, String> params = new HashMap<String, String>();
                                            if (method.equals(HttpMethod.GET)) {
                                                params = QueryStringDecoder.decode(httpRequest.getQueryString());
                                            } else {
                                                params = httpRequest.getParameterMap();
                                            }

                                            String userDn = authenticationFilterService.processAuthenticationFilters(params);
                                            if (userDn != null) {
                                                sessionUser = sessionIdService.generateSessionId(userDn);
                                                sessionUser.setAuthenticationTime(new Date());
                                                sessionIdService.updateSessionLastUsedDate(sessionUser);
                                                user = userService.getUserByDn(sessionUser.getUserDn());

                                                Authenticator authenticator = (Authenticator) Component.getInstance(Authenticator.class, true);
                                                authenticator.authenticateExternallyWebService(user.getUserId());
                                                identity.addRole("user");
                                            } else {
                                                sb.append(errorResponseFactory.getErrorAsQueryString(
                                                        AuthorizeErrorResponseType.LOGIN_REQUIRED, state));

                                                builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                                return builder.build();
                                            }
                                        } else {
                                            sb.append(errorResponseFactory.getErrorAsQueryString(
                                                    AuthorizeErrorResponseType.LOGIN_REQUIRED, state));

                                            builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                            return builder.build();
                                        }
                                    } else {
                                        if (prompts.contains(Prompt.LOGIN)) {
                                            identity.logout();
                                            user = null;
                                            sessionUser.setUserDn(null);
                                            sessionUser.setAuthenticationTime(null);
                                            //sessionUser.setPermissionGranted(false);
                                            prompts.remove(Prompt.LOGIN);
                                        }

                                        sb = redirectToAuthorizationPage(responseTypes, scope, clientId, redirectUri,
                                                state, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint,
                                                acrValues, request, requestUri);
                                        builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                        return builder.build();
                                    }
                                }

                                if (prompts.contains(Prompt.NONE) && Boolean.parseBoolean(client.getTrustedClient())) {
                                    sessionUser.setPermissionGranted(true);
                                }

                                if (prompts.contains(Prompt.LOGIN)) {
                                    identity.logout();
                                    user = null;
                                    sessionUser.setUserDn(null);
                                    sessionUser.setAuthenticationTime(null);
                                    //sessionUser.setPermissionGranted(false);
                                    prompts.remove(Prompt.LOGIN);

                                    sb = redirectToAuthorizationPage(responseTypes, scope, clientId, redirectUri,
                                            state, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint,
                                            acrValues, request, requestUri);
                                    builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                    return builder.build();
                                }

                                if (prompts.contains(Prompt.CONSENT) && !sessionUser.isPermissionGranted()) {
                                    prompts.remove(Prompt.CONSENT);

                                    sb = redirectToAuthorizationPage(responseTypes, scope, clientId, redirectUri,
                                            state, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint,
                                            acrValues, request, requestUri);
                                    builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                    return builder.build();
                                }

                                // OXAUTH-37 : Validate authentication max age
                                boolean validAuthenticationMaxAge = true;
                                Integer authenticationMaxAge = null;
                                if (maxAge != null) {
                                    authenticationMaxAge = maxAge;
                                } else if (!invalidOpenidRequestObject && jwtAuthorizationRequest != null
                                        && jwtAuthorizationRequest.getIdTokenMember().getMaxAge() != null) {
                                    authenticationMaxAge = jwtAuthorizationRequest.getIdTokenMember().getMaxAge();
                                }
                                GregorianCalendar now = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                                GregorianCalendar userAuthenticationTime = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
                                userAuthenticationTime.setTime(sessionUser.getAuthenticationTime());
                                if (authenticationMaxAge != null) {
                                    userAuthenticationTime.add(Calendar.SECOND, authenticationMaxAge);
                                    validAuthenticationMaxAge = userAuthenticationTime.after(now);
                                } else if (client.getDefaultMaxAge() != null) {
                                    userAuthenticationTime.add(Calendar.SECOND, client.getDefaultMaxAge());
                                    validAuthenticationMaxAge = userAuthenticationTime.after(now);
                                }
                                if (!validAuthenticationMaxAge) {
                                    identity.logout();
                                    user = null;
                                    sessionUser.setUserDn(null);
                                    sessionUser.setAuthenticationTime(null);
                                    //sessionUser.setPermissionGranted(false);

                                    sb = redirectToAuthorizationPage(responseTypes, scope, clientId, redirectUri,
                                            state, nonce, display, prompts, maxAge, uiLocales, idTokenHint, loginHint,
                                            acrValues, request, requestUri);
                                    builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                    return builder.build();
                                }

                                // OXAUTH-87 : Checks whether client has groups. If yes then user must be in one of these groups otherwise forbid authorization.
                                if (checkUserGroups(user, client)) {
                                    AuthorizationCode authorizationCode = null;
                                    if (responseTypes.contains(ResponseType.CODE)) {
                                        authorizationGrant = authorizationGrantList.createAuthorizationCodeGrant(user, client,
                                                sessionUser.getAuthenticationTime());
                                        authorizationGrant.setJwtAuthorizationRequest(jwtAuthorizationRequest);

                                        // Store authentication level and mode
                                        authorizationGrant.setAuthLevel(authLevel);
                                        authorizationGrant.setAuthMode(authMode);
                                        authorizationGrant.save(); // call save after object modification!!!

                                        authorizationCode = authorizationGrant.getAuthorizationCode();

                                        if (sb.toString().contains("?") && !sb.toString().endsWith("?")
                                                && !sb.toString().endsWith("&")) {
                                            sb.append("&");
                                        }

                                        sb.append("code=").append(authorizationCode.getCode());
                                    }

                                    AccessToken newAccessToken = null;
                                    if (responseTypes.contains(ResponseType.TOKEN)) {
                                        if (authorizationGrant == null) {
                                            authorizationGrant = authorizationGrantList.createImplicitGrant(user, client,
                                                    sessionUser.getAuthenticationTime());
                                            authorizationGrant.setJwtAuthorizationRequest(jwtAuthorizationRequest);

                                            // Store authentication level and mode
                                            authorizationGrant.setAuthLevel(authLevel);
                                            authorizationGrant.setAuthMode(authMode);
                                            authorizationGrant.save(); // call save after object modification!!!
                                        }
                                        newAccessToken = authorizationGrant.createAccessToken();

                                        if (sb.toString().contains("#") && !sb.toString().endsWith("#")) {
                                            sb.append("&");
                                        }

                                        sb.append("access_token=").append(newAccessToken.getCode());
                                        sb.append("&token_type=").append(newAccessToken.getTokenType());
                                        sb.append("&expires_in=").append(newAccessToken.getExpiresIn());
                                    }

                                    if (responseTypes.contains(ResponseType.ID_TOKEN)) {
                                        if (authorizationGrant == null) {
                                            authorizationGrant = authorizationGrantList.createAuthorizationGrant(user, client,
                                                    sessionUser.getAuthenticationTime());
                                            authorizationGrant.setJwtAuthorizationRequest(jwtAuthorizationRequest);

                                            // Store authentication level and mode
                                            authorizationGrant.setAuthLevel(authLevel);
                                            authorizationGrant.setAuthMode(authMode);
                                            authorizationGrant.save(); // call save after object modification!!!
                                        }
                                        Map<String, String> idTokenClaims = getClaims(user, authorizationGrant, scopes);
                                        IdToken idToken = authorizationGrant.createIdToken(nonce, authorizationCode, newAccessToken, idTokenClaims);

                                        if (sb.toString().contains("#") && !sb.toString().endsWith("#")) {
                                            sb.append("&");
                                        }

                                        sb.append("id_token=").append(idToken.getCode());
                                    }

                                    if ((authorizationGrant != null) && StringHelper.isNotEmpty(authLevel) && StringHelper.isNotEmpty(authMode)) {
                                        sb.append("&auth_level=").append(authLevel);
                                        sb.append("&auth_mode=").append(authMode);
                                    }

                                    //if (Boolean.valueOf(requestSessionId) && StringUtils.isBlank(sessionId) &&
                                    if (sessionUser.getId() == null) {
                                        final String newSessionId = sessionIdService.generateId(sessionUser.getUserDn());
                                        sessionUser.setId(newSessionId);
                                        log.trace("newSessionId = {0}", newSessionId);
                                    }
                                    sb.append(Parameters.SESSION_ID.nameToAppend()).append(sessionUser.getId());

                                    if (state != null && !state.isEmpty()) {
                                        sb.append("&state=").append(state);
                                    }
                                    if (scope != null && !scope.isEmpty()) {
                                        scope = authorizationGrant.checkScopesPolicy(scope);

                                        try {
                                            sb.append("&scope=").append(URLEncoder.encode(scope, "UTF-8"));
                                        } catch (UnsupportedEncodingException e) {
                                            log.trace(e.getMessage(), e);
                                        }
                                    }

                                    builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                } else {
                                    sb.append(errorResponseFactory.getErrorAsQueryString(
                                            AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state));
                                    builder = RedirectUtil.getRedirectResponseBuilder(sb.toString(), httpRequest);
                                }
                            }
                        } else { // Invalid redirectUri
                            builder = error(Response.Status.BAD_REQUEST,
                                    AuthorizeErrorResponseType.INVALID_REQUEST_REDIRECT_URI, state); // 400
                        }
                    } else { // Invalid responseTypes
                        builder = Response.status(Response.Status.BAD_REQUEST.getStatusCode()); // 400
                        builder.entity(errorResponseFactory.getErrorAsJson(
                                AuthorizeErrorResponseType.UNSUPPORTED_RESPONSE_TYPE, state));
                    }
                } else { // Invalid clientId
                    builder = error(Response.Status.UNAUTHORIZED, AuthorizeErrorResponseType.UNAUTHORIZED_CLIENT, state);
                }
            }
        } catch (SignatureException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        } catch (StringEncrypter.EncryptionException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        } catch (InvalidJwtException e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            builder = Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()); // 500
            log.error(e.getMessage(), e);
        }

        return builder.build();
    }

    private ResponseBuilder error(Response.Status p_status, AuthorizeErrorResponseType p_type, String p_state) {
        return Response.status(p_status.getStatusCode()).entity(errorResponseFactory.getErrorAsJson(p_type, p_state));
    }

    private StringBuilder redirectToAuthorizationPage(
            List<ResponseType> responseTypes, String scope, String clientId, String redirectUri, String state,
            String nonce, String display, List<Prompt> prompts, Integer maxAge, List<String> uiLocales,
            String idTokenHint, String loginHint, List<String> acrValues, String request, String requestUri) {
        StringBuilder sb = new StringBuilder(ConfigurationFactory.getConfiguration().getAuthorizationPage());

        try {
            // oAuth parameters
            String responseType = implode(responseTypes, " ");
            if (StringUtils.isNotBlank(responseType)) {
                sb.append("?").append(AuthorizeRequestParam.RESPONSE_TYPE).append("=")
                        .append(URLEncoder.encode(responseType, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(scope)) {
                sb.append("&").append(AuthorizeRequestParam.SCOPE).append("=")
                        .append(URLEncoder.encode(scope, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(clientId)) {
                sb.append("&").append(AuthorizeRequestParam.CLIENT_ID).append("=")
                        .append(URLEncoder.encode(clientId, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(redirectUri)) {
                sb.append("&").append(AuthorizeRequestParam.REDIRECT_URI).append("=")
                        .append(URLEncoder.encode(redirectUri, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(state)) {
                sb.append("&").append(AuthorizeRequestParam.STATE).append("=")
                        .append(URLEncoder.encode(state, Util.UTF8_STRING_ENCODING));
            }

            // OIC parameters
            if (StringUtils.isNotBlank(nonce)) {
                sb.append("&").append(AuthorizeRequestParam.NONCE).append("=")
                        .append(URLEncoder.encode(nonce, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(display)) {
                sb.append("&").append(AuthorizeRequestParam.DISPLAY).append("=")
                        .append(URLEncoder.encode(display, Util.UTF8_STRING_ENCODING));
            }
            String prompt = implode(prompts, " ");
            if (StringUtils.isNotBlank(prompt)) {
                sb.append("&").append(AuthorizeRequestParam.PROMPT).append("=")
                        .append(URLEncoder.encode(prompt, Util.UTF8_STRING_ENCODING));
            }
            if (maxAge != null) {
                sb.append("&").append(AuthorizeRequestParam.MAX_AGE).append("=").append(maxAge);
            }
            String uiLocalesStr = implode(uiLocales, " ");
            if (StringUtils.isNotBlank(uiLocalesStr)) {
                sb.append("&").append(AuthorizeRequestParam.UI_LOCALES).append("=")
                        .append(URLEncoder.encode(uiLocalesStr, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(idTokenHint)) {
                sb.append("&").append(AuthorizeRequestParam.ID_TOKEN_HINT).append("=")
                        .append(URLEncoder.encode(idTokenHint, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(loginHint)) {
                sb.append("&").append(AuthorizeRequestParam.LOGIN_HINT).append("=")
                        .append(URLEncoder.encode(loginHint, Util.UTF8_STRING_ENCODING));
            }
            String acrValuesStr = implode(acrValues, " ");
            if (StringUtils.isNotBlank(acrValuesStr)) {
                sb.append("&").append(AuthorizeRequestParam.ACR_VALUES).append("=")
                        .append(URLEncoder.encode(acrValuesStr, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(request)) {
                sb.append("&").append(AuthorizeRequestParam.REQUEST).append("=")
                        .append(URLEncoder.encode(request, Util.UTF8_STRING_ENCODING));
            }
            if (StringUtils.isNotBlank(requestUri)) {
                sb.append("&").append(AuthorizeRequestParam.REQUEST_URI).append("=")
                        .append(URLEncoder.encode(requestUri, Util.UTF8_STRING_ENCODING));
            }
        } catch (UnsupportedEncodingException ex) {
            log.error(ex.getMessage(), ex);
        }

        return sb;
    }

    /**
     * Checks whether client has groups. If yes then user must be in one of these groups otherwise forbid authorization.
     * (OXAUTH-87)
     *
     * @param p_user   user
     * @param p_client client
     * @return whether client has groups. If yes then user must be in one of these groups otherwise forbid authorization
     */
    private boolean checkUserGroups(User p_user, Client p_client) {
        if (p_client != null && p_client.hasUserGroups()) {
            final String[] userGroups = p_client.getUserGroups();
            return userGroupService.isInAnyGroup(userGroups, p_user.getDn());
        }
        return true;
    }

    public Map<String, String> getClaims(User user, AuthorizationGrant authorizationGrant, Collection<String> scopes) throws InvalidClaimException {
        Map<String, String> claims = new HashMap<String, String>();

        for (String scopeName : scopes) {
            Scope scope = scopeService.getScopeByDisplayName(scopeName);

            if (scope != null && scope.getOxAuthClaims() != null) {
                for (String claimDn : scope.getOxAuthClaims()) {
                    GluuAttribute attribute = attributeService.getScopeByDn(claimDn);

                    String attributeName = attribute.getName();
                    Object attributeValue = null;
                    if (attributeName.equals("uid")) {
                        attributeValue = user.getUserId();
                    } else {
                        attributeValue = user.getAttribute(attribute.getName(), true);
                    }

                    final ClaimMappingConfiguration mapping = ClaimMappingConfiguration.getMappingByLdap(attributeName);
                    if (mapping != null) {
                        attributeName = mapping.getClaim();
                    }

                    if (attributeName != null && attributeValue != null) {
                        claims.put(attributeName, attributeValue.toString());
                    }
                }
            }
        }

        if (authorizationGrant.getAuthMode() != null) {
            claims.put(JwtClaimName.AUTHENTICATION_METHOD_REFERENCES, authorizationGrant.getAuthMode());
        }

        if (authorizationGrant.getJwtAuthorizationRequest() != null
                && authorizationGrant.getJwtAuthorizationRequest().getUserInfoMember() != null) {
            for (Claim claim : authorizationGrant.getJwtAuthorizationRequest().getIdTokenMember().getClaims()) {
                boolean optional = true; // ClaimValueType.OPTIONAL.equals(claim.getClaimValue().getClaimValueType());

                String claimName = claim.getName();

                final ClaimMappingConfiguration mapping = ClaimMappingConfiguration.getMappingByClaim(claimName);
                String ldapClaimName = mapping != null ? mapping.getLdap() : null;
                if (ldapClaimName == null) {
                    ldapClaimName = claimName;
                }
                Object attribute = user.getAttribute(ldapClaimName, optional);
                if (claim != null && attribute != null) {
                    claims.put(claim.getName(), attribute.toString());
                }
            }
        }


        return claims;
    }
}