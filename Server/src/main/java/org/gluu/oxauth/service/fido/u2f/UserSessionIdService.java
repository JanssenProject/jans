/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.fido.u2f;

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.common.SessionIdState;
import org.gluu.oxauth.model.fido.u2f.DeviceRegistrationResult;
import org.gluu.oxauth.service.SessionIdService;
import org.gluu.oxauth.ws.rs.fido.u2f.U2fAuthenticationWS;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;

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
