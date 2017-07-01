package org.xdi.oxauth.uma.service;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.model.custom.script.conf.CustomScriptConfiguration;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.uma.persistence.UmaPermission;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.SessionStateService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author yuriyz on 06/20/2017.
 */
@Stateless
@Named
public class UmaSessionService {

    @Inject
    private Logger log;
    @Inject
    private ErrorResponseFactory errorResponseFactory;
    @Inject
    private SessionStateService sessionStateService;
    @Inject
    private ExternalUmaClaimsGatheringService external;

    public SessionState getSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String cookieSessionId = sessionStateService.getUmaSessionStateFromCookie(httpRequest);
        log.trace("Cookie - uma_session_state: " + cookieSessionId);

        if (StringUtils.isNotBlank(cookieSessionId)) {
            SessionState ldapSessionState = sessionStateService.getSessionState(cookieSessionId);
            if (ldapSessionState != null) {
                log.trace("Loaded uma_session_state from cookie, session: " + ldapSessionState);
                return ldapSessionState;
            } else {
                log.error("Failed to load uma_session_state from cookie: " + cookieSessionId);
            }
        } else {
            log.error("uma_session_state cookie is not set.");
        }

        log.trace("Generating new uma_session_state ...");
        SessionState session = sessionStateService.generateAuthenticatedSessionState("no");

        sessionStateService.createSessionStateCookie(session.getId(), httpResponse, true);
        log.trace("uma_session_state cookie created.");
        return session;
    }

    public boolean persist(SessionState session) {
        try {
            if (sessionStateService.persistSessionState(session, true)) {
                log.trace("Session persisted successfully. Session: " + session);
                return true;
            }
        } catch (Exception e) {
            log.error("Failed to persist session, id: " + session.getId(), e);
        }
        return false;
    }

    public int getStep(SessionState session) {
        String stepString = session.getSessionAttributes().get("step");
        int step = Util.parseIntSilently(stepString);
        if (step == -1) {
            step = 1;
            setStep(step, session);
        }
        return step;
    }

    public void setStep(int step, SessionState session) {
        session.getSessionAttributes().put("step", Integer.toString(step));
    }

    public CustomScriptConfiguration getScript(SessionState session) {
        String scriptName = getScriptName(session);
        if (StringUtils.isNotBlank(scriptName)) {
            return external.getCustomScriptConfigurationByName(scriptName);
        }
        return null;
    }

    public void configure(SessionState session, String scriptName, Boolean reset, List<UmaPermission> permissions,
                          String clientId, String claimRedirectUri, String state) {
//        if (reset != null && reset) {
        setStep(1, session);
//        }
        setState(session, state);
        setClaimsRedirectUri(session, claimRedirectUri);
        setTicket(session, permissions.get(0).getTicket());

//        if (StringUtils.isBlank(getScriptName(session))) {
        setScriptName(session, scriptName);
//        }

//        if (StringUtils.isBlank(getPct(session))) {
        String pct = permissions.get(0).getAttributes().get("pct");

        if (StringUtils.isBlank(pct)) {
            log.error("PCT code is null or blank in permission object.");
            throw new RuntimeException("PCT code is null or blank in permission object.");
        }

        setPct(session, pct);
//        }

//        if (StringUtils.isBlank(getClientId(session))) {
        setClientId(session, clientId);
//        }

//        getStep(session); // init step
        persist(session);
    }

    public boolean isStepPassed(SessionState session, Integer step) {
        return Boolean.parseBoolean(session.getSessionAttributes().get(String.format("uma_step_passed_%d", step)));
    }

    public boolean isPassedPreviousSteps(SessionState session, Integer step) {
        for (int i = 1; i < step; i++) {
            if (!isStepPassed(session, i)) {
                return false;
            }
        }
        return true;
    }

    public void markStep(SessionState session, Integer step, boolean value) {
        String key = String.format("uma_step_passed_%d", step);
        if (value) {
            session.getSessionAttributes().put(key, Boolean.TRUE.toString());
        } else {
            session.getSessionAttributes().remove(key);
        }
    }

    public String getScriptName(SessionState session) {
        return session.getSessionAttributes().get("gather_script_name");
    }

    public void setScriptName(SessionState session, String scriptName) {
        session.getSessionAttributes().put("gather_script_name", scriptName);
    }

    public String getPct(SessionState session) {
        return session.getSessionAttributes().get("pct");
    }

    public void setPct(SessionState session, String pct) {
        session.getSessionAttributes().put("pct", pct);
    }

    public String getClientId(SessionState session) {
        return session.getSessionAttributes().get("client_id");
    }

    public void setClientId(SessionState session, String clientId) {
        session.getSessionAttributes().put("client_id", clientId);
    }

    public String getClaimsRedirectUri(SessionState session) {
        return session.getSessionAttributes().get("claims_redirect_uri");
    }

    public void setClaimsRedirectUri(SessionState session, String claimsRedirectUri) {
        session.getSessionAttributes().put("claims_redirect_uri", claimsRedirectUri);
    }

    public String getState(SessionState session) {
        return session.getSessionAttributes().get("state");
    }

    public void setState(SessionState session, String state) {
        session.getSessionAttributes().put("state", state);
    }

    public String getTicket(SessionState session) {
        return session.getSessionAttributes().get("ticket");
    }

    public void setTicket(SessionState session, String ticket) {
        session.getSessionAttributes().put("ticket", ticket);
    }

    public void resetToStep(SessionState session, int overridenNextStep, int step) {
        for (int i = overridenNextStep; i <= step; i++) {
            markStep(session, i, false);
        }

        setStep(overridenNextStep, session);
    }
}
