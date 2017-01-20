/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.auth;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Install;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Identity;
import org.jboss.seam.security.NotLoggedInException;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.jboss.seam.util.Base64;
import org.jboss.seam.web.AbstractFilter;
import org.xdi.oxauth.model.authorize.AuthorizeRequestParam;
import org.xdi.oxauth.model.common.AuthenticationMethod;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.config.StaticConf;
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
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.util.ServerUtil;
import org.xdi.util.StringHelper;

/**
 * @author Javier Rojas Blum
 * @version March 4, 2016
 */
@Scope(ScopeType.APPLICATION)
@Name("org.jboss.seam.web.authenticationFilter")
@Install(value = false, precedence = Install.BUILT_IN)
@BypassInterceptors
@Filter(within = "org.jboss.seam.web.exceptionFilter")
public class AuthenticationFilter extends AbstractFilter {

    @Logger
    private Log log;

	private AppConfiguration appConfiguration;

	@Observer( ConfigurationFactory.CONFIGURATION_UPDATE_EVENT )
	public void updateConfiguration(AppConfiguration appConfiguration, StaticConf staticConfiguration) {
		this.appConfiguration = appConfiguration;
	}

    private String realm;
    public static final String REALM = "oxAuth";
    private static long counter = 0L;

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                         final FilterChain filterChain) throws IOException, ServletException {
        final HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        final HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        new ContextualHttpServletRequest(httpRequest) {

            @Override
            public void process() {
                final Identity identity = Identity.instance();
                final SessionStateService sessionStateService = SessionStateService.instance();
                final Authenticator authenticator = (Authenticator) Component.getInstance(Authenticator.class, true);
                final ClientService clientService = (ClientService) Component.getInstance(ClientService.class, true);
                final ClientFilterService clientFilterService = ClientFilterService.instance();
                final ErrorResponseFactory errorResponseFactory = (ErrorResponseFactory) Component.getInstance(ErrorResponseFactory.class, true);
                
                // Workaround for tomcat
                if (appConfiguration == null) {
                	appConfiguration = (AppConfiguration) Component.getInstance("appConfiguration", true);
                }

                try {
                    final String requestUrl = httpRequest.getRequestURL().toString();
					log.trace("Get request to: '{0}'", requestUrl);
                    if (requestUrl.endsWith("/token") && ServerUtil.isSameRequestPath(requestUrl, appConfiguration.getTokenEndpoint())) {
                    	log.debug("Starting token endpoint authentication");
                        if (httpRequest.getParameter("client_assertion") != null
                                && httpRequest.getParameter("client_assertion_type") != null) {
							log.debug("Starting JWT token endpoint authentication");
                            processJwtAuth(authenticator, errorResponseFactory, httpRequest, httpResponse, filterChain, identity);
                        } else if (httpRequest.getHeader("Authorization") != null && httpRequest.getHeader("Authorization").startsWith("Basic ")) {
							log.debug("Starting Basic Auth token endpoint authentication");
                            processBasicAuth(authenticator, clientService, errorResponseFactory, httpRequest, httpResponse, filterChain, identity);
                        } else {
							log.debug("Starting POST Auth token endpoint authentication");
                            processPostAuth(authenticator, clientService, clientFilterService, errorResponseFactory, httpRequest, httpResponse, filterChain, identity);
                        }
                    } else if (httpRequest.getHeader("Authorization") != null) {
                        String header = httpRequest.getHeader("Authorization");
                        if (header.startsWith("Bearer ")) {
                            processBearerAuth(httpRequest, httpResponse, filterChain);
                        } else if (header.startsWith("Basic ")) {
                            processBasicAuth(authenticator, clientService, errorResponseFactory, httpRequest, httpResponse, filterChain, identity);
                        } else {
                            httpResponse.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealm() + "\"");

                            httpResponse.sendError(401, "Not authorized");
                        }
                    } else {
                        String sessionState = httpRequest.getParameter(AuthorizeRequestParam.SESSION_STATE);
                        List<Prompt> prompts = Prompt.fromString(httpRequest.getParameter(AuthorizeRequestParam.PROMPT), " ");

                        if (StringUtils.isBlank(sessionState)) {
                            // OXAUTH-297 : check whether session_state is present in cookie
                            sessionState = sessionStateService.getSessionStateFromCookie(httpRequest);
                        }

                        SessionState sessionStateObject = null;
                        if (StringUtils.isNotBlank(sessionState)) {
                            sessionStateObject = sessionStateService.getSessionState(sessionState);
                        }
                        if (sessionStateObject != null && SessionIdState.AUTHENTICATED == sessionStateObject.getState() && !prompts.contains(Prompt.LOGIN)) {
                            processSessionAuth(authenticator, errorResponseFactory, sessionState, httpRequest, httpResponse, filterChain);
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
        }.run();
    }

    private void processSessionAuth(Authenticator authenticator, ErrorResponseFactory errorResponseFactory, String p_sessionState, HttpServletRequest p_httpRequest, HttpServletResponse p_httpResponse, FilterChain p_filterChain) throws IOException, ServletException {
        boolean requireAuth;

        requireAuth = !authenticator.authenticateBySessionState(p_sessionState);
        log.trace("Process Session Auth, sessionState = {0}, requireAuth = {1}", p_sessionState, requireAuth);

        if (!requireAuth) {
            try {
                p_filterChain.doFilter(p_httpRequest, p_httpResponse);
            } catch (Exception ex) {
                log.error("Failed to process session authentication", ex);
                requireAuth = true;
            }
        }

        if (requireAuth) {
            sendError(errorResponseFactory, p_httpResponse);
        }
    }

    private void processBasicAuth(Authenticator authenticator, ClientService clientService, ErrorResponseFactory errorResponseFactory, HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                  FilterChain filterChain, Identity identity) {
        boolean requireAuth = true;

        try {
            String header = servletRequest.getHeader("Authorization");
            if (header != null && header.startsWith("Basic ")) {
                String base64Token = header.substring(6);
                String token = new String(Base64.decode(base64Token), Util.UTF8_STRING_ENCODING);

                String username = "";
                String password = "";
                int delim = token.indexOf(":");

                if (delim != -1) {
                    username = token.substring(0, delim);
                    password = token.substring(delim + 1);
                }

                requireAuth = !StringHelper.equals(username, identity.getCredentials().getUsername()) || !identity.isLoggedIn();

                // Only authenticate if username doesn't match Identity.username and user isn't authenticated
                if (requireAuth) {
                    if (!username.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                        if (servletRequest.getRequestURI().endsWith("/token")) {
                            Client client = clientService.getClient(username);
                            if (client == null || AuthenticationMethod.CLIENT_SECRET_BASIC != client.getAuthenticationMethod()) {
                                throw new Exception("The Token Authentication Method is not valid.");
                            }
                        }

                        identity.getCredentials().setUsername(username);
                        identity.getCredentials().setPassword(password);

                        requireAuth = !authenticator.authenticateWebService();
                    }
                }
            }

            try {
                if (!requireAuth) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            } catch (NotLoggedInException ex) {
                requireAuth = true;
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

        try {
            if (requireAuth && !identity.isLoggedIn()) {
                sendError(errorResponseFactory, servletResponse);
            }
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private void processBearerAuth(HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                   FilterChain filterChain) {
        try {
            String header = servletRequest.getHeader("Authorization");
            if (header != null && header.startsWith("Bearer ")) {
                // Immutable object
                //servletRequest.getParameterMap().put("access_token", new String[]{accessToken});
                filterChain.doFilter(servletRequest, servletResponse);
            }
        } catch (ServletException ex) {
            log.info("Bearer authorization failed: {0}", ex, ex.getMessage());
        } catch (IOException ex) {
            log.info("Bearer authorization failed: {0}", ex, ex.getMessage());
        } catch (Exception ex) {
            log.info("Bearer authorization failed: {0}", ex, ex.getMessage());
        }
    }

    private void processPostAuth(Authenticator authenticator, ClientService clientService, ClientFilterService clientFilterService, ErrorResponseFactory errorResponseFactory, HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                 FilterChain filterChain, Identity identity) {
        try {
            String clientId = "";
            String clientSecret = "";
            boolean isExistUserPassword = false;
            if (StringHelper.isNotEmpty(servletRequest.getParameter("client_id")) && StringHelper.isNotEmpty(servletRequest.getParameter("client_secret"))) {
                clientId = servletRequest.getParameter("client_id");
                clientSecret = servletRequest.getParameter("client_secret");
                isExistUserPassword = true;
            }
        	log.trace("isExistUserPassword: {0}", isExistUserPassword);

            boolean requireAuth = !StringHelper.equals(clientId, identity.getCredentials().getUsername()) || !identity.isLoggedIn();
        	log.debug("requireAuth: '{0}'", requireAuth);

            if (requireAuth) {
                if (isExistUserPassword) {
                    Client client = clientService.getClient(clientId);
                    if (client != null && AuthenticationMethod.CLIENT_SECRET_POST == client.getAuthenticationMethod()) {
                        // Only authenticate if username doesn't match Identity.username and user isn't authenticated
                        if (!clientId.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                            identity.logout();

                            identity.getCredentials().setUsername(clientId);
                            identity.getCredentials().setPassword(clientSecret);

                            requireAuth = !authenticator.authenticateWebService();
                        } else {
                        	authenticator.configureSessionClient(client);
                        }
                    }
                } else if (Boolean.TRUE.equals(appConfiguration.getClientAuthenticationFiltersEnabled())) {
                    String clientDn = clientFilterService.processAuthenticationFilters(servletRequest.getParameterMap());
                    if (clientDn != null) {
                        Client client = clientService.getClientByDn(clientDn);

                        identity.logout();

                        identity.getCredentials().setUsername(client.getClientId());
                        identity.getCredentials().setPassword(null);

                        requireAuth = !authenticator.authenticateWebService(true);
                    }
                }
            }

            try {
                if (!requireAuth) {
                    filterChain.doFilter(servletRequest, servletResponse);
                    return;
                }
            } catch (NotLoggedInException ex) {
                requireAuth = true;
            }

            if (requireAuth && !identity.isLoggedIn()) {
                sendError(errorResponseFactory, servletResponse);
            }
        } catch (ServletException ex) {
            log.error("Post authentication failed: {0}", ex, ex.getMessage());
        } catch (IOException ex) {
            log.error("Post authentication failed: {0}", ex, ex.getMessage());
        } catch (Exception ex) {
            log.error("Post authentication failed: {0}", ex, ex.getMessage());
        }
    }

    private void processJwtAuth(Authenticator authenticator, ErrorResponseFactory errorResponseFactory, HttpServletRequest servletRequest, HttpServletResponse servletResponse,
                                FilterChain filterChain, Identity identity) {
        boolean authorized = false;

        try {
            if (servletRequest.getParameter("client_assertion") != null
                    && servletRequest.getParameter("client_assertion_type") != null) {
                String clientId = servletRequest.getParameter("client_id");
                ClientAssertionType clientAssertionType = ClientAssertionType.fromString(
                        servletRequest.getParameter("client_assertion_type"));
                String encodedAssertion = servletRequest.getParameter("client_assertion");

                if (clientAssertionType == ClientAssertionType.JWT_BEARER) {
                    ClientAssertion clientAssertion = new ClientAssertion(appConfiguration, clientId, clientAssertionType, encodedAssertion);

                    String username = clientAssertion.getSubjectIdentifier();
                    String password = clientAssertion.getClientSecret();

                    // Only authenticate if username doesn't match Identity.username and user isn't authenticated
                    if (!username.equals(identity.getCredentials().getUsername()) || !identity.isLoggedIn()) {
                        identity.getCredentials().setUsername(username);
                        identity.getCredentials().setPassword(password);

                        authenticator.authenticateWebService(true);
                        authorized = true;
                    }
                }
            }

            filterChain.doFilter(servletRequest, servletResponse);
        } catch (NotLoggedInException ex) {
            log.info("JWT authentication failed: {0}", ex, ex.getMessage());
        } catch (ServletException ex) {
            log.info("JWT authentication failed: {0}", ex, ex.getMessage());
        } catch (IOException ex) {
            log.info("JWT authentication failed: {0}", ex, ex.getMessage());
        } catch (InvalidJwtException ex) {
            log.info("JWT authentication failed: {0}", ex, ex.getMessage());
        }

        try {
            if (!authorized) {
                sendError(errorResponseFactory, servletResponse);
            }
        } catch (IOException ex) {
        }
    }

    private void sendError(ErrorResponseFactory errorResponseFactory, HttpServletResponse servletResponse) throws IOException {
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

}