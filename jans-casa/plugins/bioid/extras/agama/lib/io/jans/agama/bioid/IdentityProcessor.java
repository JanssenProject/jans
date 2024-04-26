package io.jans.agama.bioid;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.EncryptionService;
import io.jans.as.common.service.common.UserService;
import io.jans.service.UserAuthenticatorService;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.as.server.service.AuthenticationService;
import io.jans.model.user.authenticator.UserAuthenticator;
import io.jans.util.StringHelper;
import io.jans.orm.model.base.CustomObjectAttribute;

import java.io.IOException;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityProcessor implements IdentityProcessorInterface {

    private static final Logger logger = LoggerFactory.getLogger(IdentityProcessor.class);

    public IdentityProcessor() {
    }

    public boolean validateBioIdCode(String username, String bioIdCode) {
        User user = getUser("uid", username);
        UserService userService = CdiUtil.bean(UserService.class);
        UserAuthenticatorService userAuthenticatorService = CdiUtil.bean(UserAuthenticatorService.class);
        UserAuthenticator authenticator = userAuthenticatorService.getUserAuthenticatorById(user, "bioid");
        if (authenticator == null) {
            logger.info("BioID code missing. Aborting...");
            return false;
        }
        HashMap<String, Object> bioIdMap = authenticator.getCustom();
        String storedCode = bioIdMap.get("code");
        long expiration = bioIdMap.get("expiration");
        boolean isValid = bioIdCode == storedCode;
        if (new Date().getTime() > expiration) {
            logger.info("BioID code has expired. ");
            isValid = false;
        }
        userAuthenticatorService.removeUserAuthenticator(user, authenticator);
        userService.updateUser(user);
        return isValid;
    }

    private static User getUser(String attributeName, String value) {
        UserService userService = CdiUtil.bean(UserService.class);
        return userService.getUserByAttribute(attributeName, value, true);
    }
}
