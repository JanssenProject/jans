/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service;

import com.unboundid.ldap.sdk.Filter;
import com.unboundid.util.StaticUtils;
import org.apache.commons.lang.StringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.Prompt;
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.config.ConfigurationFactory;
import org.xdi.oxauth.model.util.Util;
import org.xdi.util.StringHelper;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuriy Zabrovarnyy
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version February 23, 2016
 */

@Scope(ScopeType.STATELESS)
@Name("sessionStateService")
@AutoCreate
public class SessionStateService {

    public static final String SESSION_STATE_COOKIE_NAME = "session_state";
    public static final String SESSION_CUSTOM_STATE = "session_custom_state";

    @Logger
    private Log log;

    @In
    private LdapEntryManager ldapEntryManager;

    @In
    private AuthenticationService authenticationService;

    public static SessionStateService instance() {
        if (!Contexts.isEventContextActive() && !Contexts.isApplicationContextActive()) {
            Lifecycle.beginCall();
        }
        return (SessionStateService) Component.getInstance(SessionStateService.class);
    }


    // #34 - update session attributes with each request
    // 1) redirect_uri change -> update session
    // 2) acr change -> send error
    // 3) client_id change -> do nothing
    // https://github.com/GluuFederation/oxAuth/issues/34
    public SessionState assertAuthenticatedSessionCorrespondsToNewRequest(SessionState session, String redirectUri, String acrValuesStr) throws AcrChangedException {
        if (session != null && !session.getSessionAttributes().isEmpty() && session.getState() == SessionIdState.AUTHENTICATED) {

            final Map<String, String> sessionAttributes = session.getSessionAttributes();

            boolean isAcrChanged = acrValuesStr != null && !acrValuesStr.equals(sessionAttributes.get("acr_values"));
            if (isAcrChanged) {
                throw new AcrChangedException();
            }

            final Map<String, String> currentSessionAttributes = getCurrentSessionAttributes(sessionAttributes);
            if (!currentSessionAttributes.equals(sessionAttributes)) {
                sessionAttributes.putAll(currentSessionAttributes);

                // Reinit login
                sessionAttributes.put("auth_step", "1");

                for (Iterator<Entry<String, String>> it = currentSessionAttributes.entrySet().iterator(); it.hasNext(); ) {
                    Entry<String, String> currentSessionAttributesEntry = it.next();
                    String name = currentSessionAttributesEntry.getKey();
                    if (name.startsWith("auth_step_passed_")) {
                        it.remove();
                    }
                }

                session.setSessionAttributes(currentSessionAttributes);

                boolean updateResult = updateSessionState(session, true, true);
                if (!updateResult) {
                    log.debug("Failed to update session entry: '{0}'", session.getId());
                }
            }
        }
        return session;
    }

    private Map<String, String> getCurrentSessionAttributes(Map<String, String> sessionAttributes) {
        // Update from request
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            // Clone before replacing new attributes
            final Map<String, String> currentSessionAttributes = new HashMap<String, String>(sessionAttributes);

            final ExternalContext externalContext = facesContext.getExternalContext();
            Map<String, String> parameterMap = externalContext.getRequestParameterMap();
            Map<String, String> newRequestParameterMap = authenticationService.getAllowedParameters(parameterMap);
            for (Entry<String, String> newRequestParameterMapEntry : newRequestParameterMap.entrySet()) {
                String name = newRequestParameterMapEntry.getKey();
                if (!StringHelper.equalsIgnoreCase(name, "auth_step")) {
                    currentSessionAttributes.put(name, newRequestParameterMapEntry.getValue());
                }
            }

            return currentSessionAttributes;
        } else {
            return sessionAttributes;
        }
    }


    public String getSessionStateFromCookie(HttpServletRequest request) {
        try {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(SESSION_STATE_COOKIE_NAME) /*&& cookie.getSecure()*/) {
                        log.trace("Found session_state cookie: '{0}'", cookie.getValue());
                        return cookie.getValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }

    public String getSessionStateFromCookie() {
        try {
		    FacesContext facesContext = FacesContext.getCurrentInstance();
		    if (facesContext == null) {
		    	return null;
		    }
            final HttpServletRequest request = (HttpServletRequest) facesContext.getExternalContext().getRequest();
            return getSessionStateFromCookie(request);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public void createSessionStateCookie(String sessionState) {
        try {
            final Object response = FacesContext.getCurrentInstance().getExternalContext().getResponse();
            if (response instanceof HttpServletResponse) {
                final Cookie sessionStateCookie = new Cookie(SESSION_STATE_COOKIE_NAME, sessionState);
                sessionStateCookie.setPath("/");

                // ATTENTION : we have to set also HttpOnly flag but it's supported from Servlet 3.0
                // we need to upgrade to Servlet 3.0 and target to Tomcat 7 : http://tomcat.apache.org/whichversion.html
                // sessionStateCookie.setSecure(true);
                // sessionStateCookie.setHttpOnly(true);
                final HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.addCookie(sessionStateCookie);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void removeSessionStateCookie() {
        try {
            final FacesContext currentInstance = FacesContext.getCurrentInstance();
            if (currentInstance != null && currentInstance.getExternalContext() != null) {
                final Object response = currentInstance.getExternalContext().getResponse();
                if (response instanceof HttpServletResponse) {
                    removeSessionStateCookie((HttpServletResponse) response);
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void removeSessionStateCookie(HttpServletResponse httpResponse) {
        final Cookie cookie = new Cookie(SESSION_STATE_COOKIE_NAME, null); // Not necessary, but saves bandwidth.
        cookie.setPath("/");
        cookie.setMaxAge(0); // Don't set to -1 or it will become a session cookie!
        httpResponse.addCookie(cookie);
    }

    public SessionState getSessionState() {
        String sessionState = getSessionStateFromCookie();

        if (StringHelper.isNotEmpty(sessionState)) {
            return getSessionState(sessionState);
        }

        return null;
    }

    public Map<String, String> getSessionAttributes(SessionState sessionState) {
        if (sessionState != null) {
            return sessionState.getSessionAttributes();
        }

        return null;
    }

    public SessionState generateAuthenticatedSessionState(String userDn) {
        return generateAuthenticatedSessionState(userDn, "");
    }

    public SessionState generateAuthenticatedSessionState(String userDn, String prompt) {
        Map<String, String> sessionIdAttributes = new HashMap<String, String>();
        sessionIdAttributes.put("prompt", prompt);

        return generateSessionState(userDn, new Date(), SessionIdState.AUTHENTICATED, sessionIdAttributes, true);
    }

    public SessionState generateAuthenticatedSessionState(String userDn, Map<String, String> sessionIdAttributes) {
        return generateSessionState(userDn, new Date(), SessionIdState.AUTHENTICATED, sessionIdAttributes, true);
    }

    public SessionState generateSessionState(String p_userDn, Date authenticationDate, SessionIdState state, Map<String, String> sessionIdAttributes, boolean persist) {
        final String uuid = UUID.randomUUID().toString();
        final String dn = dn(uuid);

        if (StringUtils.isBlank(dn)) {
            return null;
        }

        if (SessionIdState.AUTHENTICATED == state) {
            if (StringUtils.isBlank(p_userDn)) {
                return null;
            }
        }

        final SessionState sessionState = new SessionState();
        sessionState.setId(uuid);
        sessionState.setDn(dn);

        if (StringUtils.isNotBlank(p_userDn)) {
            sessionState.setUserDn(p_userDn);
        }

        if (authenticationDate != null) {
            sessionState.setAuthenticationTime(authenticationDate);
        }

        if (state != null) {
            sessionState.setState(state);
        }

        sessionState.setSessionAttributes(sessionIdAttributes);

        boolean persisted = false;
        if (persist) {
            persisted = persistSessionState(sessionState);
        }

        log.trace("Generated new session, id = '{0}', state = '{1}', persisted = '{2}'", sessionState.getId(), sessionState.getState(), persisted);
        return sessionState;
    }

    public SessionState setSessionStateAuthenticated(SessionState sessionState, String p_userDn) {
        sessionState.setUserDn(p_userDn);
        sessionState.setAuthenticationTime(new Date());
        sessionState.setState(SessionIdState.AUTHENTICATED);

        boolean persisted = updateSessionState(sessionState, true, true);

        log.trace("Authenticated session, id = '{0}', state = '{1}', persisted = '{2}'", sessionState.getId(), sessionState.getState(), persisted);
        return sessionState;
    }

    public boolean persistSessionState(final SessionState sessionState) {
        return persistSessionState(sessionState, false);
    }

    public boolean persistSessionState(final SessionState sessionState, boolean forcePersistence) {
        List<Prompt> prompts = getPromptsFromSessionState(sessionState);

        try {
            final int unusedLifetime = ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime();
            if ((unusedLifetime > 0 && isPersisted(prompts)) || forcePersistence) {
                sessionState.setLastUsedAt(new Date());

                sessionState.setPersisted(true);
                ldapEntryManager.persist(sessionState);
                return true;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    public boolean updateSessionState(final SessionState sessionState) {
        return updateSessionState(sessionState, true);
    }

    public boolean updateSessionState(final SessionState sessionState, boolean updateLastUsedAt) {
        return updateSessionState(sessionState, updateLastUsedAt, false);
    }

    public boolean updateSessionState(final SessionState sessionState, boolean updateLastUsedAt, boolean forceUpdate) {
        List<Prompt> prompts = getPromptsFromSessionState(sessionState);

        try {
            final int unusedLifetime = ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime();
            if ((unusedLifetime > 0 && isPersisted(prompts)) || forceUpdate) {
                if (updateLastUsedAt) {
                    sessionState.setLastUsedAt(new Date());
                }

                sessionState.setPersisted(true);
                ldapEntryManager.merge(sessionState);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }

        return true;
    }

    private static boolean isPersisted(List<Prompt> prompts) {
        if (prompts != null && prompts.contains(Prompt.NONE)) {
            final Boolean persistOnPromptNone = ConfigurationFactory.instance().getConfiguration().getSessionIdPersistOnPromptNone();
            return persistOnPromptNone != null && persistOnPromptNone;
        }
        return true;
    }

    private static String dn(String p_id) {
        final String baseDn = getBaseDn();
        final StringBuilder sb = new StringBuilder();
        if (Util.allNotBlank(p_id, getBaseDn())) {
            sb.append("uniqueIdentifier=").append(p_id).append(",").append(baseDn);
        }
        return sb.toString();
    }

    public SessionState getSessionByDN(String p_dn) {
        try {
            return ldapEntryManager.find(SessionState.class, p_dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }
        return null;
    }

    public SessionState getSessionState(String sessionState) {
        if (StringHelper.isEmpty(sessionState)) {
            return null;
        }

        String dn = dn(sessionState);
        boolean contains = containsSessionState(dn);
        if (!contains) {
            return null;
        }

        try {
            final SessionState entity = getSessionByDN(dn);
            log.trace("Try to get session by id: {0} ...", sessionState);
            if (entity != null) {
                log.trace("Session dn: {0}", entity.getDn());

                if (isSessionValid(entity)) {
                    return entity;
                }
            }
        } catch (Exception ex) {
            log.trace(ex.getMessage(), ex);
        }

        log.trace("Failed to get session by id: {0}", sessionState);
        return null;
    }

    public boolean containsSessionState(String dn) {
        try {
            return ldapEntryManager.contains(SessionState.class, dn);
        } catch (Exception e) {
            log.trace(e.getMessage(), e);
        }

        return false;
    }

    private static String getBaseDn() {
        return ConfigurationFactory.instance().getBaseDn().getSessionId();
    }

    public boolean remove(SessionState p_sessionState) {
        try {
            ldapEntryManager.remove(p_sessionState);
        } catch (Exception e) {
            log.error(e.getMessage(), e);

            return false;
        }
        return true;
    }

    public void remove(List<SessionState> list) {
        for (SessionState id : list) {
            remove(id);
        }
    }

    public void cleanUpSessions() {
        final int interval = ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime();
        final int unauthenticatedInterval = ConfigurationFactory.instance().getConfiguration().getSessionIdUnauthenticatedUnusedLifetime();

        remove(getUnauthenticatedIdsOlderThan(unauthenticatedInterval));
        remove(getIdsOlderThan(interval));
    }

    public List<SessionState> getUnauthenticatedIdsOlderThan(int p_intervalInSeconds) {
        try {
            final long dateInPast = new Date().getTime() - TimeUnit.SECONDS.toMillis(p_intervalInSeconds);
            final Filter filter = Filter.create(String.format("&(lastModifiedTime<=%s)(oxState=unauthenticated)", StaticUtils.encodeGeneralizedTime(new Date(dateInPast))));
            return ldapEntryManager.findEntries(getBaseDn(), SessionState.class, filter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }


    public List<SessionState> getIdsOlderThan(int p_intervalInSeconds) {
        try {
            final long dateInPast = new Date().getTime() - TimeUnit.SECONDS.toMillis(p_intervalInSeconds);
            final Filter filter = Filter.create(String.format("(lastModifiedTime<=%s)", StaticUtils.encodeGeneralizedTime(new Date(dateInPast))));
            return ldapEntryManager.findEntries(getBaseDn(), SessionState.class, filter);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public boolean isSessionValid(SessionState sessionState) {
        if (sessionState == null) {
            return false;
        }

        final long sessionInterval = TimeUnit.SECONDS.toMillis(ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime());
        final long sessionUnauthenticatedInterval = TimeUnit.SECONDS.toMillis(ConfigurationFactory.instance().getConfiguration().getSessionIdUnauthenticatedUnusedLifetime());

        final long timeSinceLastAccess = System.currentTimeMillis() - sessionState.getLastUsedAt().getTime();
        if (timeSinceLastAccess > sessionInterval && ConfigurationFactory.instance().getConfiguration().getSessionIdUnusedLifetime() != -1) {
            return false;
        }
        if (sessionState.getState() == SessionIdState.UNAUTHENTICATED && timeSinceLastAccess > sessionUnauthenticatedInterval && ConfigurationFactory.instance().getConfiguration().getSessionIdUnauthenticatedUnusedLifetime() != -1) {
            return false;
        }

        return true;
    }

    private List<Prompt> getPromptsFromSessionState(final SessionState sessionState) {
        String promptParam = sessionState.getSessionAttributes().get("prompt");
        return Prompt.fromString(promptParam, " ");
    }

}
