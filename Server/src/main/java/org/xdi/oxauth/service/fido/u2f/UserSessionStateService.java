/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistrationResult;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.ws.rs.fido.u2f.U2fAuthenticationWS;
import org.xdi.util.StringHelper;

/**
 * Configure user session to confirm user {@link U2fAuthenticationWS} authentication
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Stateless
@Named
public class UserSessionStateService {

	@Inject
	private Logger log;

	@Inject
	private SessionStateService sessionStateService;

	public void updateUserSessionStateOnFinishRequest(String sessionState, String userInum, DeviceRegistrationResult deviceRegistrationResult, boolean enroll, boolean oneStep) {
		SessionState ldapSessionState = getLdapSessionState(sessionState);
		if (ldapSessionState == null) {
			return;
		}
		
		Map<String, String> sessionAttributes = ldapSessionState.getSessionAttributes();
		if (DeviceRegistrationResult.Status.APPROVED == deviceRegistrationResult.getStatus()) {
			sessionAttributes.put("session_custom_state", "approved");
		} else {
			sessionAttributes.put("session_custom_state", "declined");
		}
		sessionAttributes.put("oxpush2_u2f_device_id", deviceRegistrationResult.getDeviceRegistration().getId());
		sessionAttributes.put("oxpush2_u2f_device_user_inum", userInum);
		sessionAttributes.put("oxpush2_u2f_device_enroll", Boolean.toString(enroll));
		sessionAttributes.put("oxpush2_u2f_device_one_step", Boolean.toString(oneStep));
		
		sessionStateService.updateSessionState(ldapSessionState, true);
	}

	public void updateUserSessionStateOnError(String sessionState)  {
		SessionState ldapSessionState = getLdapSessionState(sessionState);
		if (ldapSessionState == null) {
			return;
		}
		
		Map<String, String> sessionAttributes = ldapSessionState.getSessionAttributes();
		sessionAttributes.put("session_custom_state", "declined");
		
		sessionStateService.updateSessionState(ldapSessionState, true);
	}

	private SessionState getLdapSessionState(String sessionState) {
		if (StringHelper.isEmpty(sessionState)) {
			return null;
		}

		SessionState ldapSessionState = sessionStateService.getSessionState(sessionState);
		if (ldapSessionState == null) {
			log.warn("Failed to load session state '{0}'", sessionState);
			return null;
		}
		
		if (SessionIdState.UNAUTHENTICATED != ldapSessionState.getState()) {
			log.warn("Unexpected session '{0}' state: '{1}'", sessionState, ldapSessionState.getState());
			return null;
		}

		return ldapSessionState;
	}

}
