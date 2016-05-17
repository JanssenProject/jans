/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth.service.fido.u2f;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.xdi.oxauth.model.common.SessionState;
import org.xdi.oxauth.model.config.Constants;
import org.xdi.oxauth.service.SessionStateService;
import org.xdi.util.StringHelper;

/**
 * Utility to validate U2F input data
 *
 * @author Yuriy Movchan Date: 05/11/2016
 */
@Scope(ScopeType.STATELESS)
@Name("u2fValidationService")
@AutoCreate
public class ValidationService {

	@Logger
	private Log log;

	@In
	private SessionStateService sessionStateService;

	public boolean isValidSessionState(String userName, String sessionState) {
		if (sessionState == null) {
			log.error("In two step authentication workflow session_state is mandatory");
			return false;
		}
		
		SessionState ldapSessionState = sessionStateService.getSessionState(sessionState);
		if (ldapSessionState == null) {
			log.error("Specified session_state '{0}' is invalid", sessionState);
			return false;
		}
		
		String sessionStateUser = ldapSessionState.getSessionAttributes().get(Constants.AUTHENTICATED_USER);
		if (!StringHelper.equalsIgnoreCase(userName, sessionStateUser)) {
			log.error("Username '{0}' and session_state '{1}' don't match", userName, sessionState);
			return false;
		}

		return true;
	}

}
