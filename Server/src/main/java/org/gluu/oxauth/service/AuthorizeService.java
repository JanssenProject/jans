/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gluu.jsf2.message.FacesMessages;
import org.gluu.jsf2.service.FacesService;
import org.gluu.model.security.Identity;
import org.gluu.oxauth.auth.Authenticator;
import org.gluu.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.gluu.oxauth.model.authorize.AuthorizeRequestParam;
import org.gluu.oxauth.model.common.*;
import org.gluu.oxauth.model.configuration.AppConfiguration;
import org.gluu.oxauth.model.error.ErrorResponseFactory;
import org.gluu.oxauth.model.registration.Client;
import org.gluu.oxauth.util.ServerUtil;
import org.oxauth.persistence.model.Scope;
import org.slf4j.Logger;
import org.gluu.oxauth.model.common.User;
import org.gluu.oxauth.service.UserService;

import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version December 8, 2018
 */
@Stateless
@Named
public class AuthorizeService {

    // use only "acr" instead of "acr_values" #334
    public static final List<String> ALLOWED_PARAMETER = Collections.unmodifiableList(Arrays.asList(
            AuthorizeRequestParam.SCOPE,
            AuthorizeRequestParam.RESPONSE_TYPE,
            AuthorizeRequestParam.CLIENT_ID,
            AuthorizeRequestParam.REDIRECT_URI,
            AuthorizeRequestParam.STATE,
            AuthorizeRequestParam.RESPONSE_MODE,
            AuthorizeRequestParam.NONCE,
            AuthorizeRequestParam.DISPLAY,
            AuthorizeRequestParam.PROMPT,
            AuthorizeRequestParam.MAX_AGE,
            AuthorizeRequestParam.UI_LOCALES,
            AuthorizeRequestParam.ID_TOKEN_HINT,
            AuthorizeRequestParam.LOGIN_HINT,
            AuthorizeRequestParam.ACR_VALUES,
            AuthorizeRequestParam.SESSION_ID,
            AuthorizeRequestParam.REQUEST,
            AuthorizeRequestParam.REQUEST_URI,
            AuthorizeRequestParam.ORIGIN_HEADERS,
            AuthorizeRequestParam.CODE_CHALLENGE,
            AuthorizeRequestParam.CODE_CHALLENGE_METHOD,
            AuthorizeRequestParam.CUSTOM_RESPONSE_HEADERS,
            AuthorizeRequestParam.CLAIMS));

    @Inject
    private Logger log;

    @Inject
    private ClientService clientService;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private UserService userService;

    @Inject
    private ClientAuthorizationsService clientAuthorizationsService;

    @Inject
    private Identity identity;

    @Inject
    private Authenticator authenticator;

    @Inject
    private FacesService facesService;

    @Inject
    private FacesMessages facesMessages;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private ScopeService scopeService;

    @Inject
    private RequestParameterService requestParameterService;

    public SessionId getSession() {
        return getSession(null);
    }

    public SessionId getSession(String sessionId) {
        if (StringUtils.isBlank(sessionId)) {
            sessionId = sessionIdService.getSessionIdFromCookie();
            if (StringUtils.isBlank(sessionId)) {
                return null;
            }
        }

        if (!identity.isLoggedIn()) {
            authenticator.authenticateBySessionId(sessionId);
        }

        SessionId ldapSessionId = sessionIdService.getSessionId(sessionId);
        if (ldapSessionId == null) {
            identity.logout();
        }

        return ldapSessionId;
    }

    public void permissionGranted(HttpServletRequest httpRequest, final SessionId session) {
        log.trace("permissionGranted");
        try {
            final User user = userService.getUserByDn(session.getUserDn());
            if (user == null) {
                log.error("Permission denied. Failed to find session user: userDn = " + session.getUserDn() + ".");
                permissionDenied(session);
                return;
            }

            String clientId = session.getSessionAttributes().get(AuthorizeRequestParam.CLIENT_ID);
            final Client client = clientService.getClient(clientId);

            String scope = session.getSessionAttributes().get(AuthorizeRequestParam.SCOPE);
            String responseType = session.getSessionAttributes().get(AuthorizeRequestParam.RESPONSE_TYPE);

            boolean persistDuringImplicitFlow = ServerUtil.isFalse(appConfiguration.getUseCacheForAllImplicitFlowObjects()) || !ResponseType.isImplicitFlow(responseType);
            if (!client.getTrustedClient() && persistDuringImplicitFlow) {
                final Set<String> scopes = Sets.newHashSet(org.gluu.oxauth.model.util.StringUtils.spaceSeparatedToList(scope));
                clientAuthorizationsService.add(user.getAttribute("inum"), client.getClientId(), scopes, client.getPersistClientAuthorizations());
            }
            session.addPermission(clientId, true);
            sessionIdService.updateSessionId(session);

            // OXAUTH-297 - set session_id cookie
            if (!appConfiguration.getInvalidateSessionCookiesAfterAuthorizationFlow()) {
                sessionIdService.createSessionIdCookie(session.getId(), session.getSessionState(), session.getOPBrowserState(), false);
            }
            Map<String, String> sessionAttribute = requestParameterService.getAllowedParameters(session.getSessionAttributes());

            if (sessionAttribute.containsKey(AuthorizeRequestParam.PROMPT)) {
                List<Prompt> prompts = Prompt.fromString(sessionAttribute.get(AuthorizeRequestParam.PROMPT), " ");
                prompts.remove(Prompt.CONSENT);
                sessionAttribute.put(AuthorizeRequestParam.PROMPT, org.gluu.oxauth.model.util.StringUtils.implodeEnum(prompts, " "));
            }

            final String parametersAsString = requestParameterService.parametersAsString(sessionAttribute);
            final String uri = httpRequest.getContextPath() + "/restv1/authorize?" + parametersAsString;
            log.trace("permissionGranted, redirectTo: {}", uri);

            invalidateSessionCookiesIfNeeded();
            facesService.redirectToExternalURL(uri);
        } catch (UnsupportedEncodingException e) {
            log.trace(e.getMessage(), e);
        }
    }

    public void permissionDenied(final SessionId session) {
        log.trace("permissionDenied");
        invalidateSessionCookiesIfNeeded();

        if (session == null) {
            authenticationFailedSessionInvalid();
            return;
        }

        StringBuilder sb = new StringBuilder();
        String redirectUri = session.getSessionAttributes().get(AuthorizeRequestParam.REDIRECT_URI);
        String state = session.getSessionAttributes().get(AuthorizeRequestParam.STATE);

        sb.append(redirectUri);
        if (redirectUri != null && redirectUri.contains("?")) {
            sb.append("&");
        } else {
            sb.append("?");
        }
        sb.append(errorResponseFactory.getErrorAsQueryString(AuthorizeErrorResponseType.ACCESS_DENIED, state));

        facesService.redirectToExternalURL(sb.toString());
    }

    private void authenticationFailedSessionInvalid() {
        facesMessages.add(FacesMessage.SEVERITY_ERROR, "login.errorSessionInvalidMessage");
        facesService.redirect("/error.xhtml");
    }

    public List<org.oxauth.persistence.model.Scope> getScopes() {
        SessionId session = getSession();
        String scope = session.getSessionAttributes().get("scope");

        return getScopes(scope);

    }

    public List<Scope> getScopes(String scopes) {
        List<org.oxauth.persistence.model.Scope> result = new ArrayList<org.oxauth.persistence.model.Scope>();

        if (scopes != null && !scopes.isEmpty()) {
            String[] scopesName = scopes.split(" ");
            for (String scopeName : scopesName) {
                org.oxauth.persistence.model.Scope s = scopeService.getScopeByDisplayName(scopeName);
                if (s != null && s.getDescription() != null) {
                    result.add(s);
                }
            }
        }

        return result;
    }

    private void invalidateSessionCookiesIfNeeded() {
        if (appConfiguration.getInvalidateSessionCookiesAfterAuthorizationFlow()) {
            invalidateSessionCookies();
        }
    }

    private void invalidateSessionCookies() {
        try {
            if (externalContext.getResponse() instanceof HttpServletResponse) {
                final HttpServletResponse httpResponse = (HttpServletResponse) externalContext.getResponse();

                log.trace("Invalidated {} cookie.", SessionIdService.SESSION_ID_COOKIE_NAME);
                httpResponse.addHeader("Set-Cookie", SessionIdService.SESSION_ID_COOKIE_NAME + "=deleted; Path=/; Secure; HttpOnly; Expires=Thu, 01 Jan 1970 00:00:01 GMT;");

                log.trace("Invalidated {} cookie.", SessionIdService.CONSENT_SESSION_ID_COOKIE_NAME);
                httpResponse.addHeader("Set-Cookie", SessionIdService.CONSENT_SESSION_ID_COOKIE_NAME + "=deleted; Path=/; Secure; HttpOnly; Expires=Thu, 01 Jan 1970 00:00:01 GMT;");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
