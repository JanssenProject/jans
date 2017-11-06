/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.gluu.jsf2.message.FacesMessages;
import org.gluu.jsf2.service.FacesService;
import org.slf4j.Logger;
import org.xdi.model.security.Identity;
import org.xdi.oxauth.auth.Authenticator;
import org.xdi.oxauth.model.authorize.AuthorizeErrorResponseType;
import org.xdi.oxauth.model.authorize.AuthorizeRequestParam;
import org.xdi.oxauth.model.common.*;
import org.xdi.oxauth.model.configuration.AppConfiguration;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.util.ServerUtil;

import javax.ejb.Stateless;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * 
 * @version November 3, 2017
 */
@Stateless
@Named
public class AuthorizeService {

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
    private AuthenticationService authenticationService;

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
    private AppConfiguration appConfiguration;

    @Inject
    private ScopeService scopeService;

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

            // oxAuth #441 Pre-Authorization + Persist Authorizations... don't write anything
            // If a client has pre-authorization=true, there is no point to create the entry under
            // ou=clientAuthorizations it will negatively impact performance, grow the size of the
            // ldap database, and serve no purpose.
            boolean persistDuringImplicitFlow = ServerUtil.isFalse(appConfiguration.getUseCacheForAllImplicitFlowObjects()) || !ResponseType.isImplicitFlow(responseType);
            if (client.getPersistClientAuthorizations() && !client.getTrustedClient() && persistDuringImplicitFlow) {
                final Set<String> scopes = Sets.newHashSet(org.xdi.oxauth.model.util.StringUtils.spaceSeparatedToList(scope));
                clientAuthorizationsService.add(user.getAttribute("inum"), client.getClientId(), scopes);
            }
            session.addPermission(clientId, true);
            sessionIdService.updateSessionId(session);

            // OXAUTH-297 - set session_id cookie
            sessionIdService.createSessionIdCookie(session.getId(), session.getSessionState(), false);

            Map<String, String> sessionAttribute = authenticationService.getAllowedParameters(session.getSessionAttributes());

            if (sessionAttribute.containsKey(AuthorizeRequestParam.PROMPT)) {
                List<Prompt> prompts = Prompt.fromString(sessionAttribute.get(AuthorizeRequestParam.PROMPT), " ");
                prompts.remove(Prompt.CONSENT);
                sessionAttribute.put(AuthorizeRequestParam.PROMPT, org.xdi.oxauth.model.util.StringUtils.implodeEnum(prompts, " "));
            }

            final String parametersAsString = authenticationService.parametersAsString(sessionAttribute);
            final String uri = httpRequest.getContextPath() + "/restv1/authorize?" + parametersAsString;
            log.trace("permissionGranted, redirectTo: {}", uri);

            facesService.redirectToExternalURL(uri);
        } catch (UnsupportedEncodingException e) {
            log.trace(e.getMessage(), e);
        }
    }

    public void permissionDenied(final SessionId session) {
        log.trace("permissionDenied");

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

    public List<org.xdi.oxauth.model.common.Scope> getScopes() {
    	SessionId session = getSession();
    	String scope = session.getSessionAttributes().get("scope");
    	
    	return getScopes(scope);
    	
    }

    public List<Scope> getScopes(String scopes) {
        List<org.xdi.oxauth.model.common.Scope> result = new ArrayList<org.xdi.oxauth.model.common.Scope>();

        if (scopes != null && !scopes.isEmpty()) {
            String[] scopesName = scopes.split(" ");
            for (String scopeName : scopesName) {
                org.xdi.oxauth.model.common.Scope s = scopeService.getScopeByDisplayName(scopeName);
                if (s != null && s.getDescription() != null) {
                	result.add(s);
                }
            }
        }

        return result;
    }

}
