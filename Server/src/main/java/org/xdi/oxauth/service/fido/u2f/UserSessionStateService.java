/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.SessionIdState;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.fido.u2f.DeviceRegistration;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.oxauth.ws.rs.fido.u2f.U2fAuthenticationWS;
import org.xdi.util.StringHelper;

/**
 * Configure user session to confirm user {@link U2fAuthenticationWS} authentication
 *
 * @author Yuriy Movchan Date: 05/19/2015
 */
@Scope(ScopeType.STATELESS)
@Name("userSessionStateService")
@AutoCreate
public class UserSessionStateService {

	@Logger
	private Log log;

	@In
	private SessionStateService sessionStateService;

	public void updateUserSessionStateOnFinishRequest(String sessionState, String userInum, DeviceRegistration deviceRegistration, boolean enroll, boolean oneStep)  {
		SessionState ldapSessionState = getLdapSessionState(sessionState);
		if (ldapSessionState == null) {
			return;
		}
		
		Map<String, String> sessionAttributes = ldapSessionState.getSessionAttributes();
		sessionAttributes.put("session_custom_state", "approved");
		sessionAttributes.put("oxpush2_u2f_device_id", deviceRegistration.getId());
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
