/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.auth;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.security.Identity;
import org.xdi.oxauth.model.authorize.AuthorizeRequestParam;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.exception.InvalidJwtException;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.token.ClientAssertion;
import org.xdi.oxauth.model.token.ClientAssertionType;
import org.xdi.oxauth.model.token.TokenErrorResponseType;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientFilterService;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.SessionIdService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

import javax.inject.Inject;
import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.List;

/**
 * @author Javier Rojas Blum
 * @version January 16, 2019
 */
@WebFilter(asyncSupported = true, urlPatterns = {"/restv1/authorize", "/restv1/token", "/restv1/userinfo", "/restv1/revoke"}, displayName = "oxAuth")
public class AuthenticationFilter implements Filter {

    public static final String ACCESS_TOKEN_PREFIX = "AccessToken ";

    @Inject
    private Logger log;

    @Inject
    private Authenticator authenticator;

    @Inject
    private SessionIdService sessionIdService;

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

    private String realm;
    public static final String REALM = "oxAuth";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, final FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        try {
            final String requestUrl = httpRequest.getRequestURL().toString();
            log.trace("Get request to: '{}'", requestUrl);

            boolean tokenEndpoint = ServerUtil.isSameRequestPath(requestUrl, appConfiguration.getTokenEndpoint());
            boolean tokenRevocationEndpoint = ServerUtil.isSameRequestPath(requestUrl, appConfiguration.getTokenRevocationEndpoint());
            boolean umaTokenEndpoint = requestUrl.endsWith("/uma/token");
            String authorizationHeader = httpRequest.getHeader("Authorization");

            if (tokenEndpoint || umaTokenEndpoint) {
                log.debug("Starting token endpoint authentication");

                // #686 : allow authenticated client via user access_token
                if (StringUtils.isNotBlank(authorizationHeader) && authorizationHeader.startsWith(ACCESS_TOKEN_PREFIX)) {
                    processAuthByAccessToken(httpRequest, httpResponse, filterChain);
                    return;
                }

                if (httpRequest.getParameter("client_assertion") != null
                        && httpRequest.getParameter("client_assertion_type") != null) {
                    log.debug("Starting JWT token endpoint authentication");
                    processJwtAuth(httpRequest, httpResponse, filterChain);
                } else if (authorizationHeader != null && authorizationHeader.startsWith("Basic ")) {
                    log.debug("Starting Basic Auth token endpoint authentication");
                    processBasicAuth(clientService, errorResponseFactory, httpRequest, httpResponse, filterChain);
                } else {
                    log.debug("Starting POST Auth token endpoint authentication");
                    processPostAuth(clientService, clientFilterService, errorResponseFactory, httpRequest, httpResponse,
                            filterChain, tokenEndpoint);
                }
            } else if (tokenRevocationEndpoint) {
                if (authorizationHeader.startsWith("Basic ")) {
                    processBasicAuth(clientService, errorResponseFactory, httpRequest, httpResponse, filterChain);
                } else {
                    httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealm() + "\"");

                    httpResponse.sendError(401, "Not authorized");
                }
            } else if (authorizationHeader != null) {
                if (authorizationHeader.startsWith("Bearer ")) {
                    processBearerAuth(httpRequest, httpResponse, filterChain);
                } else if (authorizationHeader.startsWith("Basic ")) {
                    processBasicAuth(clientService, errorResponseFactory, httpRequest, httpResponse, filterChain);
                } else {
                    httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealm() + "\"");

                    httpResponse.sendError(401, "Not authorized");
                }
            } else {
                String sessionId = httpRequest.getParameter(AuthorizeRequestParam.SESSION_ID);
                List<Prompt> prompts = Prompt.fromString(httpRequest.getParameter(AuthorizeRequestParam.PROMPT), " ");

                if (StringUtils.isBlank(sessionId)) {
                    // OXAUTH-297 : check whether session_id is present in cookie
                    sessionId = sessionIdService.getSessionIdFromCookie(httpRequest);
                }

                SessionId sessionIdObject = null;
                if (StringUtils.isNotBlank(sessionId)) {
                    sessionIdObject = sessionIdService.getSessionId(sessionId);
                }
                if (sessionIdObject != null && SessionIdState.AUTHENTICATED == sessionIdObject.getState()
                        && !prompts.contains(Prompt.LOGIN)) {
                    processSessionAuth(errorResponseFactory, sessionId, httpRequest, httpResponse, filterChain);
                } else {
                    filterChain.doFilter(httpRequest, httpResponse);
                }
            }
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void processAuthByAccessToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, FilterChain filterChain) {
        try {
            String accessToken = httpRequest.getHeader("Authorization").substring(ACCESS_TOKEN_PREFIX.length());
            if (StringUtils.isNotBlank(accessToken)) {
                AuthorizationGrant grant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);
                if (grant != null && grant.getAccessToken(accessToken).isValid()) {
                    Client client = grant.getClient();

                    authenticator.configureSessionClient(client);

                    filterChain.doFilter(httpRequest, httpResponse);
                    return;
                }
            }
        } catch (Exception ex) {
            log.error("Failed to authenticate client by access_token", ex);
        }

        sendError(httpResponse);
    }

    private void processSessionAuth(ErrorResponseFactory errorResponseFactory, String p_sessionId,
                                    HttpServletRequest p_httpRequest, HttpServletResponse p_httpResponse, FilterChain p_filterChain)
            throws IOException, ServletException {
        boolean requireAuth;

        requireAuth = !authenticator.authenticateBySessionId(p_sessionId);
        log.trace("Process Session Auth, sessionId = {}, requireAuth = {}", p_sessionId, requireAuth);

        if (!requireAuth) {
            try {
                p_filterChain.doFilter(p_httpRequest, p_httpResponse);
            } catch (Exception ex) {
                log.error("Failed to process session authentication", ex);
                requireAuth = true;
            }
        }

        if (requireAuth) {
            sendError(p_httpResponse);
        }
    }

    private void processBasicAuth(ClientService clientService, ErrorResponseFactory errorResponseFactory,
                                  HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain) {
        boolean requireAuth = true;

        try {
            String header = servletRequest.getHeader("Authorization");
            if (header != null && header.startsWith("Basic ")) {
                String base64Token = header.substring(6);
                String token = new String(Base64.decodeBase64(base64Token), Util.UTF8_STRING_ENCODING);

                String username = "";
                String password = "";
                int delim = token.indexOf(":");

                if (delim != -1) {
                    // oxAuth #677 URL decode the username and password
                    username = URLDecoder.decode(token.substring(0, delim), Util.UTF8_STRING_ENCODING);
                    password = URLDecoder.decode(token.substring(delim + 1), Util.UTF8_STRING_ENCODING);
                }

                requireAuth = !StringHelper.equals(username, identity.getCredentials().getUsername())
                        || !identity.isLoggedIn();

                // Only authenticate if username doesn't match Identity.username
                // and user isn't authenticated
                if (requireAuth) {
                    if (!username.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                        if (servletRequest.getRequestURI().endsWith("/token")
                                || servletRequest.getRequestURI().endsWith("/revoke")) {
                            Client client = clientService.getClient(username);
                            if (client == null
                                    || AuthenticationMethod.CLIENT_SECRET_BASIC != client.getAuthenticationMethod()) {
                                throw new Exception("The Token Authentication Method is not valid.");
                            }
                        }

                        identity.getCredentials().setUsername(username);
                        identity.getCredentials().setPassword(password);

                        requireAuth = !authenticator.authenticateWebService(servletRequest);
                    }
                }
            }

            if (!requireAuth) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }
        } catch (UnsupportedEncodingException ex) {
            log.info("Basic authentication failed", ex);
        } catch (ServletException ex) {
            log.info("Basic authentication failed", ex);
        } catch (IOException ex) {
            log.info("Basic authentication failed", ex);
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
            String header = servletRequest.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                // Immutable object
                // servletRequest.getParameterMap().put("access_token", new
                // String[]{accessToken});
                filterChain.doFilter(servletRequest, servletResponse);
            }
        } catch (ServletException ex) {
            log.info("Bearer authorization failed: {}", ex);
        } catch (IOException ex) {
            log.info("Bearer authorization failed: {}", ex);
        } catch (Exception ex) {
            log.info("Bearer authorization failed: {}", ex);
        }
    }

    private void processPostAuth(ClientService clientService, ClientFilterService clientFilterService,
                                 ErrorResponseFactory errorResponseFactory, HttpServletRequest servletRequest,
                                 HttpServletResponse servletResponse, FilterChain filterChain, boolean tokenEndpoint) {
        try {
            String clientId = "";
            String clientSecret = "";
            boolean isExistUserPassword = false;
            if (StringHelper.isNotEmpty(servletRequest.getParameter("client_id"))
                    && StringHelper.isNotEmpty(servletRequest.getParameter("client_secret"))) {
                clientId = servletRequest.getParameter("client_id");
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
                    if (client != null && AuthenticationMethod.CLIENT_SECRET_POST == client.getAuthenticationMethod()) {
                        // Only authenticate if username doesn't match
                        // Identity.username and user isn't authenticated
                        if (!clientId.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                            identity.logout();

                            identity.getCredentials().setUsername(clientId);
                            identity.getCredentials().setPassword(clientSecret);

                            requireAuth = !authenticator.authenticateWebService(servletRequest);
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

                        requireAuth = !authenticator.authenticateWebService(servletRequest, true);
                    }
                } else if (tokenEndpoint) {
                    Client client = clientService.getClient(servletRequest.getParameter("client_id"));
                    if (client != null && client.getAuthenticationMethod() == AuthenticationMethod.NONE) {
                        identity.logout();

                        identity.getCredentials().setUsername(client.getClientId());
                        identity.getCredentials().setPassword(null);

                        requireAuth = !authenticator.authenticateWebService(servletRequest, true);
                    }
                }
            }

            if (!requireAuth) {
                filterChain.doFilter(servletRequest, servletResponse);
                return;
            }

            if (requireAuth && !identity.isLoggedIn()) {
                sendError(servletResponse);
            }
        } catch (ServletException ex) {
            log.error("Post authentication failed: {}", ex);
        } catch (IOException ex) {
            log.error("Post authentication failed: {}", ex);
        } catch (Exception ex) {
            log.error("Post authentication failed: {}", ex);
        }
    }

    private void processJwtAuth(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                FilterChain filterChain) {
        boolean authorized = false;

        try {
            if (servletRequest.getParameter("client_assertion") != null
                    && servletRequest.getParameter("client_assertion_type") != null) {
                String clientId = servletRequest.getParameter("client_id");
                ClientAssertionType clientAssertionType = ClientAssertionType
                        .fromString(servletRequest.getParameter("client_assertion_type"));
                String encodedAssertion = servletRequest.getParameter("client_assertion");

                if (clientAssertionType == ClientAssertionType.JWT_BEARER) {
                    ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, clientId,
                            clientAssertionType, encodedAssertion);

                    String username = clientAssertion.getSubjectIdentifier();
                    String password = clientAssertion.getClientSecret();

                    // Only authenticate if username doesn't match
                    // Identity.username and user isn't authenticated
                    if (!username.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                        identity.getCredentials().setUsername(username);
                        identity.getCredentials().setPassword(password);

                        authenticator.authenticateWebService(servletRequest, true);
                        authorized = true;
                    }
                }
            }

            filterChain.doFilter(servletRequest, servletResponse);
        } catch (ServletException ex) {
            log.info("JWT authentication failed: {}", ex);
        } catch (IOException ex) {
            log.info("JWT authentication failed: {}", ex);
        } catch (InvalidJwtException ex) {
            log.info("JWT authentication failed: {}", ex);
        }

        if (!authorized) {
            sendError(servletResponse);
        }
    }

    private void sendError(HttpServletResponse servletResponse) {
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();

            servletResponse.setStatus(401);
            servletResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealm() + "\"");
            servletResponse.setContentType("application/json;charset=UTF-8");
            out.write(errorResponseFactory.getErrorAsJson(TokenErrorResponseType.INVALID_CLIENT));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    public String getRealm() {
        if (realm != null) {
            return realm;
        } else {
            return REALM;
        }
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    @Override
    public void destroy() {
    }

}