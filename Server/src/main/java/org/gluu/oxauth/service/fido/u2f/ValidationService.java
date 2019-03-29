/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.service.fido.u2f;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.oxauth.model.common.SessionId;
import org.gluu.oxauth.model.config.Constants;
import org.gluu.oxauth.model.fido.u2f.U2fConstants;
import org.gluu.oxauth.service.SessionIdService;
import org.gluu.util.StringHelper;
import org.slf4j.Logger;
import org.xdi.oxauth.model.common.User;
import org.xdi.oxauth.service.UserService;

/**
 * Utility to validate U2F input data
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@Stateless
@Named("u2fValidationService")
public class ValidationService {

    @Inject
    private Logger log;

    @Inject
    private SessionIdService sessionIdService;

    @Inject
    private UserService userService;

    public boolean isValidSessionId(String userName, String sessionId) {
        if (sessionId == null) {
            log.error("In two step authentication workflow session_id is mandatory");
            return false;
        }

        SessionId ldapSessionId = sessionIdService.getSessionId(sessionId);
        if (ldapSessionId == null) {
            log.error("Specified session_id '{}' is invalid", sessionId);
            return false;
        }

        String sessionIdUser = ldapSessionId.getSessionAttributes().get(Constants.AUTHENTICATED_USER);
        if (!StringHelper.equalsIgnoreCase(userName, sessionIdUser)) {
            log.error("Username '{}' and session_id '{}' don't match", userName, sessionId);
            return false;
        }

        return true;
    }

    public boolean isValidEnrollmentCode(String userName, String enrollmentCode) {
        if (enrollmentCode == null) {
            log.error("In two step authentication workflow enrollment_code is mandatory");
            return false;
        }

        User user = userService.getUser(userName, U2fConstants.U2F_ENROLLMENT_CODE_ATTRIBUTE);
        if (user == null) {
            log.error("Specified user_name '{}' is invalid", userName);
            return false;
        }

        String userEnrollmentCode = user.getAttribute(U2fConstants.U2F_ENROLLMENT_CODE_ATTRIBUTE);
        if (userEnrollmentCode == null) {
            log.error("Specified enrollment_code '{}' is invalid", enrollmentCode);
            return false;
        }

        if (!StringHelper.equalsIgnoreCase(userEnrollmentCode, enrollmentCode)) {
            log.error("Username '{}' and enrollment_code '{}' don't match", userName, enrollmentCode);
            return false;
        }

        return true;
    }

}
