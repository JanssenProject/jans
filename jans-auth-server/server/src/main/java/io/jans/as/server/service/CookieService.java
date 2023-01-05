/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service;

import com.google.common.collect.Sets;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.common.model.session.SessionIdState;
import io.jans.as.server.model.config.ConfigurationFactory;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.service.cdi.util.CdiUtil;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Set;

import static io.jans.as.model.util.StringUtils.toList;

/**
 * @author Yuriy Zabrovarnyy
 */
@RequestScoped
public class CookieService {

    private static final String SESSION_STATE_COOKIE_NAME = "session_state";
    public static final String OP_BROWSER_STATE = "opbs";
    public static final String SESSION_ID_COOKIE_NAME = "session_id";
    private static final String RP_ORIGIN_ID_COOKIE_NAME = "rp_origin_id";
    private static final String UMA_SESSION_ID_COOKIE_NAME = "uma_session_id";
    public static final String CONSENT_SESSION_ID_COOKIE_NAME = "consent_session_id";
    public static final String CURRENT_SESSIONS_COOKIE_NAME = "current_sessions";

    @Inject
    private Logger log;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ExternalContext externalContext;

    @Inject
    private ConfigurationFactory configurationFactory;

    @Inject
    private AppConfiguration appConfiguration;

    public String getSessionIdFromCookie(HttpServletRequest request) {
        return getValueFromCookie(request, SESSION_ID_COOKIE_NAME);
    }

    public String getUmaSessionIdFromCookie(HttpServletRequest request) {
        return getValueFromCookie(request, UMA_SESSION_ID_COOKIE_NAME);
    }

    public String getConsentSessionIdFromCookie(HttpServletRequest request) {
        return getValueFromCookie(request, CONSENT_SESSION_ID_COOKIE_NAME);
    }

    public String getSessionStateFromCookie(HttpServletRequest request) {
        return getValueFromCookie(request, SESSION_STATE_COOKIE_NAME);
    }

    public Set<String> getCurrentSessions() {
        try {
            if (facesContext == null) {
                return null;
            }
            final HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            if (request != null) {
                return getCurrentSessions(request);
            } else {
                log.trace("Faces context returns null for http request object.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public Set<String> getCurrentSessions(HttpServletRequest request) {
        final String valueFromCookie = getValueFromCookie(request, CURRENT_SESSIONS_COOKIE_NAME);
        if (StringUtils.isBlank(valueFromCookie)) {
            return Sets.newHashSet();
        }

        try {
            return Sets.newHashSet(toList(new JSONArray(valueFromCookie)));
        } catch (JSONException e) {
            log.error("Failed to parse current_sessions, value: " + valueFromCookie, e);
            return Sets.newHashSet();
        }
    }

    public void addCurrentSessionCookie(SessionId sessionId, HttpServletRequest request, HttpServletResponse httpResponse) {
        final Set<String> currentSessions = getCurrentSessions(request);
        removeOutdatedCurrentSessions(currentSessions, sessionId);
        currentSessions.add(sessionId.getId());

        String header = CURRENT_SESSIONS_COOKIE_NAME + "=" + new JSONArray(currentSessions).toString();
        header += "; Path=/";
        header += "; Secure";
        header += "; HttpOnly";

        createCookie(header, httpResponse);
    }

    private void removeOutdatedCurrentSessions(Set<String> currentSessions, SessionId session) {
        if (session != null) {
            final String oldSessionId = session.getSessionAttributes().get(SessionId.OLD_SESSION_ID_ATTR_KEY);
            if (StringUtils.isNotBlank(oldSessionId)) {
                currentSessions.remove(oldSessionId);
            }
        }

        if (currentSessions.isEmpty()) {
            return;
        }

        SessionIdService sessionIdService = CdiUtil.bean(SessionIdService.class); // avoid cycle dependency

        Set<String> toRemove = Sets.newHashSet();
        for (String sessionId : currentSessions) {
            SessionId sessionIdObject = null;
            try {
                sessionIdObject = sessionIdService.getSessionId(sessionId, true);
            } catch (EntryPersistenceException e) {
                // ignore - valid case if session is outdated
            }
            if (sessionIdObject == null) {
                toRemove.add(sessionId);
            }
        }
        currentSessions.removeAll(toRemove);
    }

    public String getValueFromCookie(HttpServletRequest request, String cookieName) {
        try {
            final Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(cookieName) /*&& cookie.getSecure()*/) {
                        log.trace("Found cookie: '{}'", cookie.getValue());
                        return cookie.getValue();
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return "";
    }


    public String getRpOriginIdCookie() {
        return getValueFromCookie(RP_ORIGIN_ID_COOKIE_NAME);
    }

    public String getValueFromCookie(String cookieName) {
        try {
            if (facesContext == null) {
                return null;
            }
            final HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            if (request != null) {
                return getValueFromCookie(request, cookieName);
            } else {
                log.trace("Faces context returns null for http request object.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public String getSessionIdFromCookie() {
        try {
            if (facesContext == null) {
                return null;
            }
            final HttpServletRequest request = (HttpServletRequest) externalContext.getRequest();
            if (request != null) {
                return getSessionIdFromCookie(request);
            } else {
                log.trace("Faces context returns null for http request object.");
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public void creatRpOriginIdCookie(String rpOriginId) {
        try {
            final Object response = externalContext.getResponse();
            if (response instanceof HttpServletResponse) {
                final HttpServletResponse httpResponse = (HttpServletResponse) response;

                creatRpOriginIdCookie(rpOriginId, httpResponse);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void creatRpOriginIdCookie(String rpOriginId, HttpServletResponse httpResponse) {
        String header = RP_ORIGIN_ID_COOKIE_NAME + "=" + rpOriginId;
        header += "; Path=" + configurationFactory.getContextPath();
        header += "; Secure";
        header += "; HttpOnly";

        createCookie(header, httpResponse);
    }

    public void createCookieWithState(String sessionId, String sessionState, String opbs, HttpServletRequest request, HttpServletResponse httpResponse, String cookieName) {
        String header = cookieName + "=" + sessionId;
        header += "; Path=/";
        header += "; Secure";
        header += "; HttpOnly";

        createCookie(header, httpResponse);

        createSessionStateCookie(sessionState, httpResponse);
        createOPBrowserStateCookie(opbs, httpResponse);
    }

    public void createSessionIdCookie(SessionId sessionId, HttpServletRequest request, HttpServletResponse httpResponse, boolean isUma) {
        String cookieName = isUma ? UMA_SESSION_ID_COOKIE_NAME : SESSION_ID_COOKIE_NAME;
        if (!isUma && sessionId.getState() == SessionIdState.AUTHENTICATED) {
            addCurrentSessionCookie(sessionId, request, httpResponse);
        }
        createCookieWithState(sessionId.getId(), sessionId.getSessionState(), sessionId.getOPBrowserState(), request, httpResponse, cookieName);
    }

    public void createSessionIdCookie(SessionId sessionId, boolean isUma) {
        try {
            final Object response = externalContext.getResponse();
            final Object request = externalContext.getRequest();
            if (response instanceof HttpServletResponse && request instanceof HttpServletRequest) {
                final HttpServletResponse httpResponse = (HttpServletResponse) response;
                final HttpServletRequest httpRequest = (HttpServletRequest) request;

                createSessionIdCookie(sessionId, httpRequest, httpResponse, isUma);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public void createSessionStateCookie(String sessionState, HttpServletResponse httpResponse) {
        // Create the special cookie header with secure flag but not HttpOnly because the session_state
        // needs to be read from the OP iframe using JavaScript
        String header = SESSION_STATE_COOKIE_NAME + "=" + sessionState;
        header += "; Path=/";
        header += "; Secure";

        createCookie(header, httpResponse);
    }

    public void createOPBrowserStateCookie(String opbs, HttpServletResponse httpResponse) {
        // Create the special cookie header with secure flag but not HttpOnly because the opbs
        // needs to be read from the OP iframe using JavaScript
        String header = OP_BROWSER_STATE + "=" + opbs;
        header += "; Path=/";
        header += "; Secure";
        Integer sessionStateLifetime = appConfiguration.getSessionIdLifetime();
        if (sessionStateLifetime != null && sessionStateLifetime > 0) {
            DateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.SECOND, sessionStateLifetime);
            header += "; Expires=" + formatter.format(expirationDate.getTime()) + ";";
            if (StringUtils.isNotBlank(appConfiguration.getCookieDomain())) {
                header += "Domain=" + appConfiguration.getCookieDomain() + ";";
            }
        }
        httpResponse.addHeader("Set-Cookie", header);
    }

    protected void createCookie(String header, HttpServletResponse httpResponse) {
        Integer sessionStateLifetime = appConfiguration.getSessionIdLifetime();
        if (sessionStateLifetime != null && sessionStateLifetime > 0) {
            DateFormat formatter = new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss Z");
            Calendar expirationDate = Calendar.getInstance();
            expirationDate.add(Calendar.SECOND, sessionStateLifetime);
            header += "; Expires=" + formatter.format(expirationDate.getTime()) + ";";
            if (StringUtils.isNotBlank(appConfiguration.getCookieDomain())) {
                header += "Domain=" + appConfiguration.getCookieDomain() + ";";
            }
        }

        httpResponse.addHeader("Set-Cookie", header);
    }

    public void removeSessionIdCookie(HttpServletResponse httpResponse) {
        removeCookie(SESSION_ID_COOKIE_NAME, httpResponse);
    }

    public void removeOPBrowserStateCookie(HttpServletResponse httpResponse) {
        removeCookie(OP_BROWSER_STATE, httpResponse);
    }

    public void removeUmaSessionIdCookie(HttpServletResponse httpResponse) {
        removeCookie(UMA_SESSION_ID_COOKIE_NAME, httpResponse);
    }

    public void removeConsentSessionIdCookie(HttpServletResponse httpResponse) {
        removeCookie(CONSENT_SESSION_ID_COOKIE_NAME, httpResponse);
    }

    public void removeCookie(String cookieName, HttpServletResponse httpResponse) {
        final Cookie cookie = new Cookie(cookieName, null); // Not necessary, but saves bandwidth.
        cookie.setPath("/");
        cookie.setMaxAge(0); // Don't set to -1 or it will become a session cookie!
        if (StringUtils.isNotBlank(appConfiguration.getCookieDomain())) {
            cookie.setDomain(appConfiguration.getCookieDomain());
        }
        httpResponse.addCookie(cookie);
    }
}