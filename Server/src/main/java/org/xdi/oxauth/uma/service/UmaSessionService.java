package org.xdi.oxauth.uma.service;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.util.Util;
import org.xdi.oxauth.service.SessionStateService;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

    public SessionState getSession(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String cookieSessionId = sessionStateService.getSessionStateFromCookie();
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

    public void persist(SessionState session) {
        sessionStateService.persistSessionState(session, true);
    }

    public int getStep(SessionState session) {
        Preconditions.checkNotNull(session);

        String stepString = session.getSessionAttributes().get("step");
        int step = Util.parseIntSilently(stepString);
        if (step == -1) {
            step = 1;
            setStep(step, session);
        }
        return step;
    }

    public void setStep(int step, SessionState session) {
        Preconditions.checkNotNull(session);

        session.getSessionAttributes().put("step", Integer.toString(step));
    }
}
