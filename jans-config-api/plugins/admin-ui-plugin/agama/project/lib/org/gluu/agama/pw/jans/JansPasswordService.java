package org.gluu.agama.pw.jans;

import io.jans.agama.engine.service.FlowService;
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.AuthenticationService;
import io.jans.as.server.service.UserService;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.service.CacheService;
import io.jans.service.cdi.util.CdiUtil;
import org.gluu.agama.pw.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class JansPasswordService extends PasswordService {

    private static final Logger logger = LoggerFactory.getLogger(FlowService.class);
    public static final String JANS_STATUS = "jansStatus";
    public static final String INACTIVE = "inactive";
    public static final String ACTIVE = "active";
    public static final String CACHE_PREFIX = "lock_user_";
    private static AuthenticationService authenticationService = CdiUtil.bean(AuthenticationService.class);
    private static UserService userService = CdiUtil.bean(UserService.class);
    private static CacheService cacheService = CdiUtil.bean(CacheService.class);
    private String INVALID_LOGIN_COUNT_ATTRIBUTE = "jansCountInvalidLogin";
    private int DEFAULT_MAX_LOGIN_ATTEMPT = 3;
    private int DEFAULT_LOCK_EXP_TIME = 180;

    private HashMap<String, String> flowConfig;

    public JansPasswordService(HashMap config) {
        logger.info("Flow config provided is  {}.", config);
        flowConfig = config;
        DEFAULT_MAX_LOGIN_ATTEMPT = flowConfig.get("maxLoginAttempt") != null ? flowConfig.get("maxLoginAttempt") : DEFAULT_MAX_LOGIN_ATTEMPT;
        DEFAULT_LOCK_EXP_TIME = flowConfig.get("lockExpTime") != null ? flowConfig.get("lockExpTime") : DEFAULT_LOCK_EXP_TIME;
    }

    public JansPasswordService() {
    }

    @Override
    public boolean validate(String username, String password) {
        logger.info("Validating user credentials.");
        boolean hasLogin = authenticationService.authenticate(username, password);
        if (hasLogin && Boolean.valueOf(flowConfig.get("ENABLE_ACCOUNT_LOCK"))) {
            logger.info("Credentials are valid and user account locked feature is activated");
            User currentUser = userService.getUser(username);
            userService.setCustomAttribute(currentUser, INVALID_LOGIN_COUNT_ATTRIBUTE, 0);
            userService.updateUser(currentUser);
            logger.info("Invalid login count reset to zero for {} .", username);
        }
        return hasLogin;
    }

    @Override
    public String lockAccount(String username) {
        User currentUser = userService.getUser(username);
        if (currentUser == null) {
            LogUtils.log("User % not found. Cannot lock account.", username);
            return "Authenticaton failed";
        }          
        int currentFailCount = 1;
        String invalidLoginCount = getCustomAttribute(currentUser, INVALID_LOGIN_COUNT_ATTRIBUTE);
        if (invalidLoginCount != null) {
            currentFailCount = Integer.parseInt(invalidLoginCount) + 1;
        }
        String currentStatus = getCustomAttribute(currentUser, JANS_STATUS);
        logger.info("Current user status is: {}", currentStatus);
        if (currentFailCount < DEFAULT_MAX_LOGIN_ATTEMPT) {
            int remainingCount = DEFAULT_MAX_LOGIN_ATTEMPT - currentFailCount;
            logger.info("Remaining login count: {} for user {}", remainingCount, username);
            if (remainingCount > 0 && currentStatus == "active") {
                setCustomAttribute(currentUser, INVALID_LOGIN_COUNT_ATTRIBUTE, String.valueOf(currentFailCount));
                logger.info("{}  more attempt(s) before account is LOCKED!", remainingCount);
            }
            return "You have " + remainingCount + " more attempt(s) before your account is locked.";
        }
        if (currentFailCount >= DEFAULT_MAX_LOGIN_ATTEMPT && currentStatus == "active") {
            logger.info("Locking {} account for {} seconds.", username, DEFAULT_LOCK_EXP_TIME);
            String object_to_store = "{'locked': 'true'}";
            setCustomAttribute(currentUser, JANS_STATUS, INACTIVE);
            cacheService.put(DEFAULT_LOCK_EXP_TIME, CACHE_PREFIX + username, object_to_store);
            return "Your account have been locked.";
        }
        if (currentFailCount >= DEFAULT_MAX_LOGIN_ATTEMPT && currentStatus == "inactive") {
            logger.info("User {} account is already locked. Checking if we can unlock", username);
            String cache_object = cacheService.get(CACHE_PREFIX + username);
            if (cache_object == null) {
                logger.info("Unlocking user {} account", username);
                setCustomAttribute(currentUser, JANS_STATUS, ACTIVE);
                setCustomAttribute(currentUser, INVALID_LOGIN_COUNT_ATTRIBUTE, "0");
                return "Your account  is now unlock. Try login ";
            }

        }
        return null;
    }

    private String getCustomAttribute(User user, String attributeName) {
        CustomObjectAttribute customAttribute = userService.getCustomAttribute(user, attributeName);
        if (customAttribute != null) {
            return customAttribute.getValue();
        }
        return null;
    }

    private User setCustomAttribute(User user, String attributeName, String value) {
        userService.setCustomAttribute(user, attributeName, value);
        return userService.updateUser(user);
    }
}
