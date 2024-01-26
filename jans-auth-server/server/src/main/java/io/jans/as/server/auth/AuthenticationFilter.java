/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.auth;

import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.model.authorize.AuthorizeRequestParam;
import io.jans.as.model.common.AuthenticationMethod;
import io.jans.as.model.common.ExchangeTokenType;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.common.SubjectTokenType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.token.ClientAssertionType;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.as.model.token.TokenRequestParam;
import io.jans.as.model.util.Util;
import io.jans.as.server.model.audit.Action;
import io.jans.as.server.model.audit.OAuth2AuditLog;
import io.jans.as.server.model.common.AbstractToken;
import io.jans.as.server.model.common.AuthorizationCodeGrant;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.model.common.AuthorizationGrantList;
import io.jans.as.server.model.token.ClientAssertion;
import io.jans.as.server.model.token.HttpAuthTokenType;
import io.jans.as.server.service.*;
import io.jans.as.server.service.token.TokenService;
import io.jans.as.server.token.ws.rs.TxTokenValidator;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TokenHashUtil;
import io.jans.model.security.Identity;
import io.jans.model.token.TokenEntity;
import io.jans.service.CacheService;
import io.jans.util.StringHelper;
import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.WebApplicationException;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static io.jans.as.model.ciba.BackchannelAuthenticationErrorResponseType.INVALID_REQUEST;
import static io.jans.as.model.util.Util.escapeLog;
import static org.apache.commons.lang3.BooleanUtils.isTrue;

/**
 * @author Javier Rojas Blum
 * @version September 30, 2021
 */
@WebFilter(
        filterName = "AuthenticationFilter",
        asyncSupported = true,
        urlPatterns = {
                "/restv1/authorize",
                "/restv1/token",
                "/restv1/userinfo",
                "/restv1/revoke",
                "/restv1/revoke_session",
                "/restv1/bc-authorize",
                "/restv1/par",
                "/restv1/device_authorization",
                "/restv1/register",
                "/restv1/ssa",
                "/restv1/ssa/jwt",
        },
        displayName = "oxAuth")
public class AuthenticationFilter implements Filter {

    private static final String REALM_CONSTANT = "jans-auth";

    @Inject
    private Logger log;

    @Inject
    private Authenticator authenticator;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private CookieService cookieService;

    @Inject
    private ClientService clientService;

    @Inject
    private ClientFilterService clientFilterService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private Identity identity;

    @Inject
    private AuthorizationGrantList authorizationGrantList;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private MTLSService mtlsService;

    @Inject
    private TokenService tokenService;

    @Inject
    private CacheService cacheService;

    @Inject
    private GrantService grantService;

    @Inject
    private DpopService dPoPService;

    @Inject
    private TxTokenValidator txTokenValidator;

    private String realm;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        try {
            final String requestUrl = httpRequest.getRequestURL().toString();
            log.trace("Get request to: '{}'", requestUrl);

            final String method = httpRequest.getMethod();
            if (appConfiguration.isSkipAuthenticationFilterOptionsMethod() && "OPTIONS".equals(method)) {
                log.trace("Ignoring '{}' request to to: '{}'", method, requestUrl);
                filterChain.doFilter(httpRequest, httpResponse);
                return;
            }

            boolean tokenEndpoint = requestUrl.endsWith("/token");
            boolean tokenRevocationEndpoint = requestUrl.endsWith("/revoke");
            boolean backchannelAuthenticationEnpoint = requestUrl.endsWith("/bc-authorize");
            boolean deviceAuthorizationEndpoint = requestUrl.endsWith("/device_authorization");
            boolean revokeSessionEndpoint = requestUrl.endsWith("/revoke_session");
            boolean isParEndpoint = requestUrl.endsWith("/par");
            boolean ssaEndpoint = requestUrl.endsWith("/ssa") &&
                    (Arrays.asList(HttpMethod.POST, HttpMethod.GET, HttpMethod.DELETE).contains(httpRequest.getMethod()));
            boolean ssaJwtEndpoint = requestUrl.endsWith("/ssa/jwt") && httpRequest.getMethod().equals(HttpMethod.GET);
            String authorizationHeader = httpRequest.getHeader(Constants.AUTHORIZATION);
            String dpopHeader = httpRequest.getHeader("DPoP");

            if (processMTLS(httpRequest, httpResponse, filterChain)) {
                return;
            }

            if ((tokenRevocationEndpoint || deviceAuthorizationEndpoint) && clientService.isPublic(httpRequest.getParameter(Constants.CLIENT_ID))) {
                log.trace("Skipped authentication for {} for public client.", tokenRevocationEndpoint ? "Token Revocation" : "Device Authorization");
                filterChain.doFilter(httpRequest, httpResponse);
                return;
            }

            if (tokenEndpoint || revokeSessionEndpoint || tokenRevocationEndpoint || deviceAuthorizationEndpoint || isParEndpoint || ssaEndpoint || ssaJwtEndpoint) {
                log.debug("Starting endpoint authentication {}", requestUrl);

                // #686 : allow authenticated client via user access_token
                final String accessToken = tokenService.getToken(authorizationHeader,
                        HttpAuthTokenType.Bearer, HttpAuthTokenType.AccessToken);
                if (StringUtils.isNotBlank(accessToken)) {
                    processAuthByAccessToken(accessToken, httpRequest, httpResponse, filterChain);
                    return;
                }

                // Transaction Tokens
                final String requestedTokenType = httpRequest.getParameter("requested_token_type");
                if (ExchangeTokenType.fromString(requestedTokenType) == ExchangeTokenType.TX_TOKEN) {
                    processTxToken(httpRequest, httpResponse, filterChain);
                    return;
                }

                if (httpRequest.getParameter(Constants.CLIENT_ASSERTION) != null
                        && httpRequest.getParameter(Constants.CLIENT_ASSERTION_TYPE) != null) {
                    log.debug("Starting JWT token endpoint authentication");
                    processJwtAuth(httpRequest, httpResponse, filterChain);
                } else if (tokenService.isBasicAuthToken(authorizationHeader)) {
                    log.debug("Starting Basic Auth token endpoint authentication");
                    processBasicAuth(httpRequest, httpResponse, filterChain);
                } else if (tokenEndpoint && StringUtils.isNotBlank(dpopHeader)) {
                    processDPoP(httpRequest, httpResponse, filterChain);
                } else {
                    log.debug("Starting POST Auth token endpoint authentication");
                    processPostAuth(clientFilterService, httpRequest, httpResponse, filterChain, tokenEndpoint);
                }
            } else if (backchannelAuthenticationEnpoint) {
                if (httpRequest.getParameter(Constants.CLIENT_ASSERTION) != null
                        && httpRequest.getParameter(Constants.CLIENT_ASSERTION_TYPE) != null) {
                    log.debug("Starting JWT token endpoint authentication");
                    processJwtAuth(httpRequest, httpResponse, filterChain);
                } else if (tokenService.isBasicAuthToken(authorizationHeader)) {
                    processBasicAuth(httpRequest, httpResponse, filterChain);
                } else {
                    String entity = errorResponseFactory.getErrorAsJson(INVALID_REQUEST);
                    httpResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
                    httpResponse.addHeader(Constants.WWW_AUTHENTICATE, getRealmHeaderValue());
                    httpResponse.setContentType(ContentType.APPLICATION_JSON.toString());
                    httpResponse.setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(entity.length()));
                    PrintWriter out = httpResponse.getWriter();
                    out.print(entity);
                    out.flush();
                }
            } else if (authorizationHeader != null && !tokenService.isNegotiateAuthToken(authorizationHeader)) {
                if (tokenService.isBearerAuthToken(authorizationHeader)) {
                    processBearerAuth(httpRequest, httpResponse, filterChain);
                } else if (tokenService.isBasicAuthToken(authorizationHeader)) {
                    processBasicAuth(httpRequest, httpResponse, filterChain);
                } else {
                    httpResponse.addHeader(Constants.WWW_AUTHENTICATE, getRealmHeaderValue());
                    httpResponse.sendError(401, "Not authorized");
                }
            } else {
                String sessionId = cookieService.getSessionIdFromCookie(httpRequest);
                List<io.jans.as.model.common.Prompt> prompts = io.jans.as.model.common.Prompt.fromString(httpRequest.getParameter(AuthorizeRequestParam.PROMPT), " ");

                if (StringUtils.isBlank(sessionId) && isTrue(appConfiguration.getSessionIdRequestParameterEnabled())) {
                    sessionId = httpRequest.getParameter(AuthorizeRequestParam.SESSION_ID);
                }

                SessionId sessionIdObject = null;
                if (StringUtils.isNotBlank(sessionId)) {
                    sessionIdObject = sessionIdService.getSessionId(sessionId);
                }
                if (sessionIdObject != null && SessionIdState.AUTHENTICATED == sessionIdObject.getState()
                        && !prompts.contains(io.jans.as.model.common.Prompt.LOGIN)) {
                    processSessionAuth(sessionId, httpRequest, httpResponse, filterChain);
                } else {
                    filterChain.doFilter(httpRequest, httpResponse);
                }
            }
        } catch (WebApplicationException ex) {
            log.trace(ex.getMessage(), ex);
            if (ex.getResponse() != null) {
                sendResponse(httpResponse, ex);
                return;
            }
            log.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void processTxToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) {
        try {
            final OAuth2AuditLog auditLog = new OAuth2AuditLog(ServerUtil.getIpAddress(httpRequest), Action.TOKEN_REQUEST);
            auditLog.setUsername("tx_token");

            final String subjectToken = httpRequest.getParameter("subject_token");
            final String subjectTokenType = httpRequest.getParameter("subject_token_type");

            final SubjectTokenType subjectTokenTypeEnum = txTokenValidator.validateSubjectTokenType(subjectTokenType, auditLog);
            final AuthorizationGrant subjectGrant = txTokenValidator.validateSubjectToken(subjectToken, subjectTokenTypeEnum, auditLog);

            final Client client = subjectGrant.getClient();
            if (client != null) {
                authenticator.configureSessionClient(client);
                filterChain.doFilter(httpRequest, httpResponse);
                return;
            } else {
                log.error("Client is not set for grant {}", subjectGrant.getGrantId());
            }
        } catch (Exception ex) {
            log.error("Failed to authenticate client by subject_token (Transaction Tokens)", ex);
        }

        sendError(httpResponse);
    }

    @NotNull
    public String getRealmHeaderValue() {
        return String.format("Basic realm=\"%s\"", getRealm());
    }

    /**
     * @return whether successful or not
     */
    private boolean processMTLS(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) throws Exception {
        if (cryptoProvider == null) {
            log.debug("Unable to create cryptoProvider.");
            return false;
        }

        final String clientId = httpRequest.getParameter(Constants.CLIENT_ID);
        if (StringUtils.isNotBlank(clientId)) {
            final Client client = clientService.getClient(clientId);
            if (client != null &&
                    (client.hasAuthenticationMethod(AuthenticationMethod.TLS_CLIENT_AUTH) ||
                            client.hasAuthenticationMethod(AuthenticationMethod.SELF_SIGNED_TLS_CLIENT_AUTH))) {
                return mtlsService.processMTLS(httpRequest, httpResponse, filterChain, client);
            }
        } else {
            final String requestUrl = httpRequest.getRequestURL().toString();
            boolean isRegisterEndpoint = requestUrl.endsWith("/register");
            boolean isRegistration = "POST".equalsIgnoreCase(httpRequest.getMethod());
            if (appConfiguration.getDcrAuthorizationWithMTLS() && isRegistration && isRegisterEndpoint) {
                return mtlsService.processRegisterMTLS(httpRequest);
            }
        }
        return false;
    }

    private void processAuthByAccessToken(String accessToken, HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) {
        try {
            log.trace("Authenticating client by access token {} ...", accessToken);
            if (StringUtils.isBlank(accessToken)) {
                sendError(httpResponse);
                return;
            }

            AuthorizationGrant grant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);
            if (grant == null) {
                sendError(httpResponse);
                return;
            }
            final AbstractToken accessTokenObj = grant.getAccessToken(accessToken);
            if (accessTokenObj == null || !accessTokenObj.isValid()) {
                sendError(httpResponse);
                return;
            }

            Client client = grant.getClient();
            authenticator.configureSessionClient(client);
            filterChain.doFilter(httpRequest, httpResponse);
            return;
        } catch (Exception ex) {
            log.error("Failed to authenticate client by access_token", ex);
        }

        sendError(httpResponse);
    }

    private void processSessionAuth(String sessionId, HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) {
        boolean requireAuth;

        requireAuth = !authenticator.authenticateBySessionId(sessionId);
        if (log.isTraceEnabled())
            log.trace("Process Session Auth, sessionId = {}, requireAuth = {}", escapeLog(sessionId), requireAuth);

        if (!requireAuth) {
            try {
                filterChain.doFilter(httpRequest, httpResponse);
            } catch (Exception ex) {
                log.error("Failed to process session authentication", ex);
                requireAuth = true;
            }
        }

        if (requireAuth) {
            sendError(httpResponse);
        }
    }

    private void processBasicAuth(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) {
        boolean requireAuth = true;

        try {
            String header = servletRequest.getHeader(Constants.AUTHORIZATION);
            if (tokenService.isBasicAuthToken(header)) {
                String base64Token = tokenService.getBasicToken(header);
                String token = new String(Base64.decodeBase64(base64Token), StandardCharsets.UTF_8);

                String username = "";
                String password = "";
                int delim = token.indexOf(":");

                if (delim != -1) {
                    // Jans Auth #677 URL decode the username and password
                    username = URLDecoder.decode(token.substring(0, delim), Util.UTF8_STRING_ENCODING);
                    password = URLDecoder.decode(token.substring(delim + 1), Util.UTF8_STRING_ENCODING);
                }

                requireAuth = !StringHelper.equals(username, identity.getCredentials().getUsername())
                        || !identity.isLoggedIn();

                // Only authenticate if username doesn't match Identity.username
                // and user isn't authenticated
                if (requireAuth && !username.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                    identity.getCredentials().setUsername(username);
                    identity.getCredentials().setPassword(password);

                    if (servletRequest.getRequestURI().endsWith("/token")
                            || servletRequest.getRequestURI().endsWith("/revoke")
                            || servletRequest.getRequestURI().endsWith("/revoke_session")
                            || servletRequest.getRequestURI().endsWith("/userinfo")
                            || servletRequest.getRequestURI().endsWith("/bc-authorize")
                            || servletRequest.getRequestURI().endsWith("/par")
                            || servletRequest.getRequestURI().endsWith("/device_authorization")) {
                        Client client = clientService.getClient(username);
                        if (client == null
                                || !client.hasAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_BASIC)) {
                            throw new Exception("The Token Authentication Method is not valid.");
                        }
                        requireAuth = !authenticator.authenticateClient(servletRequest);
                    } else {
                        requireAuth = !authenticator.authenticateUser(servletRequest);
                    }
                }
            }

            if (!requireAuth) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        } catch (Exception ex) {
            log.info("Basic authentication failed", ex);
        }

        if (requireAuth && !identity.isLoggedIn()) {
            sendError(servletResponse);
        }
    }

    private void processBearerAuth(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                   FilterChain filterChain) {
        try {
            String header = servletRequest.getHeader(Constants.AUTHORIZATION);
            if (tokenService.isBearerAuthToken(header)) {
                filterChain.doFilter(servletRequest, servletResponse);
            }
        } catch (Exception ex) {
            log.info("Bearer authorization failed.", ex);
        }
    }

    private void processPostAuth(ClientFilterService clientFilterService, HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse, FilterChain filterChain, boolean tokenEndpoint) {
        try {
            String clientId = "";
            String clientSecret = "";
            boolean isExistUserPassword = false;
            if (StringHelper.isNotEmpty(servletRequest.getParameter(Constants.CLIENT_ID))
                    && StringHelper.isNotEmpty(servletRequest.getParameter("client_secret"))) {
                clientId = servletRequest.getParameter(Constants.CLIENT_ID);
                clientSecret = servletRequest.getParameter("client_secret");
                isExistUserPassword = true;
            }
            log.trace("isExistUserPassword: {}", isExistUserPassword);

            boolean requireAuth = !StringHelper.equals(clientId, identity.getCredentials().getUsername())
                    || !identity.isLoggedIn();
            log.debug("requireAuth: '{}'", requireAuth);

            if (requireAuth) {
                if (isExistUserPassword) {
                    Client client = clientService.getClient(clientId);
                    if (client != null && client.hasAuthenticationMethod(AuthenticationMethod.CLIENT_SECRET_POST)) {
                        // Only authenticate if username doesn't match
                        // Identity.username and user isn't authenticated
                        if (!clientId.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                            identity.logout();

                            identity.getCredentials().setUsername(clientId);
                            identity.getCredentials().setPassword(clientSecret);

                            requireAuth = !authenticator.authenticateClient(servletRequest);
                        } else {
                            authenticator.configureSessionClient(client);
                        }
                    }
                } else if (Boolean.TRUE.equals(appConfiguration.getClientAuthenticationFiltersEnabled())) {
                    String clientDn = clientFilterService
                            .processAuthenticationFilters(servletRequest.getParameterMap());
                    if (clientDn != null) {
                        Client client = clientService.getClientByDn(clientDn);

                        identity.logout();

                        identity.getCredentials().setUsername(client.getClientId());
                        identity.getCredentials().setPassword(null);

                        requireAuth = !authenticator.authenticateClient(servletRequest, true);
                    }
                } else if (tokenEndpoint) {
                    Client client = clientService.getClient(servletRequest.getParameter(Constants.CLIENT_ID));
                    if (client != null && client.hasAuthenticationMethod(AuthenticationMethod.NONE)) {
                        identity.logout();

                        identity.getCredentials().setUsername(client.getClientId());
                        identity.getCredentials().setPassword(null);

                        requireAuth = !authenticator.authenticateClient(servletRequest, true);
                    }
                }
            }

            if (!requireAuth) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            if (!identity.isLoggedIn()) {
                sendError(servletResponse);
            }
        } catch (Exception ex) {
            log.error("Post authentication failed.", ex);
        }
    }

    private void processJwtAuth(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                FilterChain filterChain) {
        boolean authorized = false;

        try {
            if (servletRequest.getParameter(Constants.CLIENT_ASSERTION) != null
                    && servletRequest.getParameter(Constants.CLIENT_ASSERTION_TYPE) != null) {
                String clientId = servletRequest.getParameter(Constants.CLIENT_ID);
                ClientAssertionType clientAssertionType = ClientAssertionType
                        .fromString(servletRequest.getParameter(Constants.CLIENT_ASSERTION_TYPE));
                String encodedAssertion = servletRequest.getParameter(Constants.CLIENT_ASSERTION);

                if (clientAssertionType == ClientAssertionType.JWT_BEARER) {
                    ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, cryptoProvider, clientId,
                            clientAssertionType, encodedAssertion);

                    String username = clientAssertion.getSubjectIdentifier();
                    String password = clientAssertion.getClientSecret();

                    // Only authenticate if username doesn't match
                    // Identity.username and user isn't authenticated
                    if (!username.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                        identity.getCredentials().setUsername(username);
                        identity.getCredentials().setPassword(password);

                        authenticator.authenticateClient(servletRequest, true);
                        authorized = true;
                    }
                }
            }

            filterChain.doFilter(servletRequest, servletResponse);
        } catch (ServletException | IOException | InvalidJwtException ex) {
            log.info("JWT authentication failed.", ex);
        }

        if (!authorized) {
            sendError(servletResponse);
        }
    }

    private void processDPoP(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                             FilterChain filterChain) {
        boolean validDPoPProof = false;
        boolean authorized = false;
        String errorReason = null;

        try {
            dPoPService.validateDpopValuesCount(servletRequest);

            String dpopStr = servletRequest.getHeader(TokenRequestParam.DPOP);
            validDPoPProof = dPoPService.validateDpop(dpopStr);
            GrantType grantType = GrantType.fromString(servletRequest.getParameter("grant_type"));

            if (grantType == GrantType.AUTHORIZATION_CODE) {
                final String code = servletRequest.getParameter("code");
                final AuthorizationCodeGrant authorizationCodeGrant = authorizationGrantList.getAuthorizationCodeGrant(code);

                identity.logout();

                identity.getCredentials().setUsername(authorizationCodeGrant.getClient().getClientId());
                identity.getCredentials().setPassword(null);
                authorized = authenticator.authenticateClient(servletRequest, true);

                filterChain.doFilter(servletRequest, servletResponse);
            } else if (grantType == GrantType.REFRESH_TOKEN) {
                final String refreshTokenCode = servletRequest.getParameter("refresh_token");
                TokenEntity tokenEntity;
                if (!isTrue(appConfiguration.getPersistRefreshToken())) {
                    tokenEntity = (TokenEntity) cacheService.get(TokenHashUtil.hash(refreshTokenCode));
                } else {
                    tokenEntity = grantService.getGrantByCode(refreshTokenCode);
                }

                String dpopJwkThumbprint = dPoPService.getDpopJwkThumbprint(dpopStr);
                if (!dpopJwkThumbprint.equals(tokenEntity.getDpop())) {
                    throw new InvalidJwtException("Invalid DPoP Proof Header. The jwk header is not valid.");
                }

                AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByRefreshToken(tokenEntity.getClientId(), refreshTokenCode);

                identity.logout();

                identity.getCredentials().setUsername(authorizationGrant.getClient().getClientId());
                identity.getCredentials().setPassword(null);
                authorized = authenticator.authenticateClient(servletRequest, true);

                filterChain.doFilter(servletRequest, servletResponse);
            }

        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception ex) {
            log.error("Invalid DPoP.", ex);
            errorReason = ex.getMessage();
        }

        if (!validDPoPProof) {
            sendInvalidDPoPError(servletResponse, errorReason);
        }

        if (!authorized) {
            sendError(servletResponse);
        }
    }

    private void sendInvalidDPoPError(HttpServletResponse servletResponse, String reason) {
        try (PrintWriter out = servletResponse.getWriter()) {
            servletResponse.setStatus(400);
            servletResponse.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON_UTF_8);
            out.write(errorResponseFactory.errorAsJson(TokenErrorResponseType.INVALID_DPOP_PROOF, reason));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void sendError(HttpServletResponse servletResponse) {
        try (PrintWriter out = servletResponse.getWriter()) {
            servletResponse.setStatus(401);
            servletResponse.addHeader(Constants.WWW_AUTHENTICATE, "Basic realm=\"" + getRealm() + "\"");
            servletResponse.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON_UTF_8);
            out.write(errorResponseFactory.errorAsJson(TokenErrorResponseType.INVALID_CLIENT, "Unable to authenticate client."));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void sendResponse(HttpServletResponse servletResponse, WebApplicationException e) {
        try (PrintWriter out = servletResponse.getWriter()) {
            servletResponse.setStatus(e.getResponse().getStatus());
            servletResponse.addHeader(Constants.WWW_AUTHENTICATE, "Basic realm=\"" + getRealm() + "\"");
            servletResponse.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON_UTF_8);
            out.write(e.getResponse().getEntity().toString());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public String getRealm() {
        if (realm != null) {
            return realm;
        } else {
            return REALM_CONSTANT;
        }
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public void destroy() {
        // nothing
    }

}