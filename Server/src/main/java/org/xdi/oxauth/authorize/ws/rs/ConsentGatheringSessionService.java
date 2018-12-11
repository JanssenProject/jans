/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2015, Gluu
 */

package org.xdi.oxauth.authorize.ws.rs;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.SessionId;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.ClientService;
import org.xdi.oxauth.service.SessionIdService;
import org.xdi.oxauth.service.UserService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    private UserService userService;

    @Inject
    private ClientService clientService;

    public SessionId getConnectSession(HttpServletRequest httpRequest) {
        String cookieId = sessionIdService.getSessionIdFromCookie(httpRequest);
        log.trace("Cookie - session_id: ", cookieId);
        if (StringUtils.isNotBlank(cookieId)) {
            return sessionIdService.getSessionId(cookieId);
        }

        return null;
    }

    public SessionId getSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String userDn, boolean create) {
        String cookieId = sessionIdService.getConsentSessionIdFromCookie(httpRequest);
        log.trace("Cookie - consent_session_id: ", cookieId);

        if (StringUtils.isNotBlank(cookieId)) {
            SessionId sessionId = sessionIdService.getSessionId(cookieId);
            if (sessionId != null) {
                log.trace("Loaded consent_session_id from cookie, session: ", sessionId);
                return sessionId;
            } else {
                log.error("Failed to load consent_session_id from cookie: ", cookieId);
            }
        } else {
            log.error("consent_session_id cookie is not set.");
        }

        if (!create) {
            return null;
        }

        log.trace("Generating new consent_session_id ...");
        SessionId session = sessionIdService.generateUnauthenticatedSessionId(userDn);

        sessionIdService.createSessionIdCookie(session.getId(), session.getSessionState(), session.getOPBrowserState(), httpResponse, SessionIdService.CONSENT_SESSION_ID_COOKIE_NAME);
        log.trace("consent_session_id cookie created.");

        return session;
    }

    public void setAuthenticatedSessionState(HttpServletRequest httpRequest, SessionId sessionId) {
        SessionId connectSession = getConnectSession(httpRequest);
        sessionIdService.setSessionIdStateAuthenticated(httpRequest, sessionId, connectSession.getDn());
    }

    public boolean isSessionStateAuthenticated(HttpServletRequest httpRequest) {
        final SessionId session = getSession(httpRequest, null, null, false);

        return sessionIdService.isSessionIdAuthenticated(session);
    }

    public boolean persist(SessionId session) {
        try {
            if (sessionIdService.persistSessionId(session, true)) {
                log.trace("Session persisted successfully. Session: " + session);
                return true;
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
        String userDn = getUserDn(httpRequest);
        if (StringUtils.isNotBlank(userDn)) {
            return userService.getUserByDn(userDn, returnAttributes);
        }

        return null;
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
