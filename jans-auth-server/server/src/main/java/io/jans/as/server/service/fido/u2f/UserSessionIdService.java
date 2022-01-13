/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido.u2f;

import io.jans.as.server.model.common.SessionId;
import io.jans.as.server.model.common.SessionIdState;
import io.jans.as.server.model.fido.u2f.DeviceRegistrationResult;
import io.jans.as.server.service.SessionIdService;
import io.jans.as.server.ws.rs.fido.u2f.U2fAuthenticationWS;
import io.jans.util.StringHelper;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Map;

/**
 * Configure user session to confirm user {@link U2fAuthenticationWS} authentication
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@Stateless
@Named
public class UserSessionIdService {

    @Inject
    private Logger log;

    @Inject
    private SessionIdService sessionIdService;

    public void updateUserSessionIdOnFinishRequest(String sessionId, String userInum, DeviceRegistrationResult deviceRegistrationResult, boolean enroll, boolean oneStep) {
        SessionId ldapSessionId = getLdapSessionId(sessionId);
        if (ldapSessionId == null) {
            return;
        }

        Map<String, String> sessionAttributes = ldapSessionId.getSessionAttributes();
        if (DeviceRegistrationResult.Status.APPROVED == deviceRegistrationResult.getStatus()) {
            sessionAttributes.put("session_custom_state", "approved");
        } else {
            sessionAttributes.put("session_custom_state", "declined");
        }
        sessionAttributes.put("oxpush2_u2f_device_id", deviceRegistrationResult.getDeviceRegistration().getId());
        sessionAttributes.put("oxpush2_u2f_device_user_inum", userInum);
        sessionAttributes.put("oxpush2_u2f_device_enroll", Boolean.toString(enroll));
        sessionAttributes.put("oxpush2_u2f_device_one_step", Boolean.toString(oneStep));

        sessionIdService.updateSessionId(ldapSessionId, true);
    }

    public void updateUserSessionIdOnError(String sessionId) {
        SessionId ldapSessionId = getLdapSessionId(sessionId);
        if (ldapSessionId == null) {
            return;
        }

        Map<String, String> sessionAttributes = ldapSessionId.getSessionAttributes();
        sessionAttributes.put("session_custom_state", "declined");

        sessionIdService.updateSessionId(ldapSessionId, true);
    }

    private SessionId getLdapSessionId(String sessionId) {
        if (StringHelper.isEmpty(sessionId)) {
            return null;
        }

        SessionId ldapSessionId = sessionIdService.getSessionId(sessionId);
        if (ldapSessionId == null) {
            log.warn("Failed to load session id '{}'", sessionId);
            return null;
        }

        if (SessionIdState.UNAUTHENTICATED != ldapSessionId.getState()) {
            log.warn("Unexpected session id '{}' state: '{}'", sessionId, ldapSessionId.getState());
            return null;
        }

        return ldapSessionId;
    }

}
