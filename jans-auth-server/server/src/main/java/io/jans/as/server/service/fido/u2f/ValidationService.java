/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.service.fido.u2f;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.fido.u2f.U2fConstants;
import io.jans.as.common.model.session.SessionId;
import io.jans.as.server.model.config.Constants;
import io.jans.as.server.service.SessionIdService;
import io.jans.util.StringHelper;
import org.slf4j.Logger;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Utility to validate U2F input data
 *
 * @author Yuriy Movchan
 * @version August 9, 2017
 */
@ApplicationScoped
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
