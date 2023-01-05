/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.authorize.ws.rs;

import io.jans.as.common.model.common.User;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.util.Util;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.CookieService;
import io.jans.as.server.service.SessionIdService;
import io.jans.orm.exception.EntryPersistenceException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * @author Yuriy Movchan
 * @version December 8, 2018
 */
@Stateless
@Named
public class ConsentGatheringSessionService {

    @Inject
    private Logger log;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private CookieService cookieService;

    @Inject
    private ClientService clientService;

    public SessionId getConnectSession(HttpServletRequest httpRequest) {
        String cookieId = cookieService.getSessionIdFromCookie(httpRequest);
        log.trace("Cookie - session_id: {}", cookieId);
        if (StringUtils.isNotBlank(cookieId)) {
            return sessionIdService.getSessionId(cookieId);
        }

        return null;
    }

    public boolean hasCookie(HttpServletRequest httpRequest) {
        String cookieId = cookieService.getConsentSessionIdFromCookie(httpRequest);

        return StringUtils.isNotBlank(cookieId);
    }

    public SessionId getConsentSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String userDn, boolean create) {
        String cookieId = cookieService.getConsentSessionIdFromCookie(httpRequest);
        log.trace("Cookie - consent_session_id: {}", cookieId);

        if (StringUtils.isNotBlank(cookieId)) {
            SessionId sessionId = sessionIdService.getSessionId(cookieId);
            if (sessionId != null) {
                log.trace("Loaded consent_session_id from cookie, session: {}", sessionId);
                return sessionId;
            } else {
                log.error("Failed to load consent_session_id from cookie: {}", cookieId);
            }
        } else {
            if (!create) {
                log.error("consent_session_id cookie is not set.");
            }
        }

        if (!create) {
            return null;
        }

        log.trace("Generating new consent_session_id ...");
        SessionId session = sessionIdService.generateUnauthenticatedSessionId(userDn);

        cookieService.createCookieWithState(session.getId(), session.getSessionState(), session.getOPBrowserState(), httpRequest, httpResponse, CookieService.CONSENT_SESSION_ID_COOKIE_NAME);
        log.trace("consent_session_id cookie created.");

        return session;
    }

    public void setAuthenticatedSessionState(HttpServletRequest httpRequest, HttpServletResponse httpResponse, SessionId sessionId) {
        SessionId connectSession = getConnectSession(httpRequest);
        sessionIdService.setSessionIdStateAuthenticated(httpRequest, httpResponse, sessionId, connectSession.getUserDn());
    }

    public boolean isSessionStateAuthenticated(HttpServletRequest httpRequest) {
        boolean hasSession = hasCookie(httpRequest);

        if (hasSession) {
            final SessionId session = getConsentSession(httpRequest, null, null, false);
            return sessionIdService.isSessionIdAuthenticated(session);
        } else {
            return false;
        }
    }

    public boolean persist(SessionId session) {
        try {
            if (sessionIdService.updateSessionId(session, true)) {
                log.trace("Session updated successfully. Session: " + session);
                return true;
            }
        } catch (EntryPersistenceException e) {
            try {
                if (sessionIdService.persistSessionId(session, true)) {
                    log.trace("Session persisted successfully. Session: " + session);
                    return true;
                }
            } catch (Exception ex) {
                log.error("Failed to persist session, id: " + session.getId(), ex);
            }
        } catch (Exception e) {
            log.error("Failed to persist session, id: " + session.getId(), e);
        }
        return false;
    }

    public int getStep(SessionId session) {
        String stepString = session.getSessionAttributes().get("step");
        int step = Util.parseIntSilently(stepString);
        if (step == -1) {
            step = 1;
            setStep(step, session);
        }
        return step;
    }

    public void setStep(int step, SessionId session) {
        session.getSessionAttributes().put("step", Integer.toString(step));
    }

    public void configure(SessionId session, String scriptName, String clientId, String state) {
        setStep(1, session);
        setScriptName(session, scriptName);

        setClientId(session, clientId);
        persist(session);
    }

    public boolean isStepPassed(SessionId session, Integer step) {
        return Boolean.parseBoolean(session.getSessionAttributes().get(String.format("consent_step_passed_%d", step)));
    }

    public boolean isPassedPreviousSteps(SessionId session, Integer step) {
        for (int i = 1; i < step; i++) {
            if (!isStepPassed(session, i)) {
                return false;
            }
        }
        return true;
    }

    public void markStep(SessionId session, Integer step, boolean value) {
        String key = String.format("consent_step_passed_%d", step);
        if (value) {
            session.getSessionAttributes().put(key, Boolean.TRUE.toString());
        } else {
            session.getSessionAttributes().remove(key);
        }
    }

    public String getScriptName(SessionId session) {
        return session.getSessionAttributes().get("gather_script_name");
    }

    public void setScriptName(SessionId session, String scriptName) {
        session.getSessionAttributes().put("gather_script_name", scriptName);
    }

    public String getClientId(SessionId session) {
        return session.getSessionAttributes().get("client_id");
    }

    public void setClientId(SessionId session, String clientId) {
        session.getSessionAttributes().put("client_id", clientId);
    }

    public void resetToStep(SessionId session, int overridenNextStep, int step) {
        for (int i = overridenNextStep; i <= step; i++) {
            markStep(session, i, false);
        }

        setStep(overridenNextStep, session);
    }

    public User getUser(HttpServletRequest httpRequest, String... returnAttributes) {
        return sessionIdService.getUser(getConnectSession(httpRequest));
    }

    public String getUserDn(HttpServletRequest httpRequest) {
        SessionId connectSession = getConnectSession(httpRequest);
        if (connectSession != null) {
            return connectSession.getUserDn();
        }

        log.trace("No logged in user.");
        return null;
    }

    public Client getClient(SessionId session) {
        String clientId = getClientId(session);
        if (StringUtils.isNotBlank(clientId)) {
            return clientService.getClient(clientId);
        }
        log.trace("client_id is not in session.");
        return null;
    }

}
