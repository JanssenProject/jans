package org.gluu.agama.pw.jans;

import io.jans.agama.engine.service.FlowService;
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.AuthenticationService;
import io.jans.as.server.service.UserService;
import io.jans.model.GluuStatus;
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
    // Keeps the lock record past its expiry so an unlock can prove this mechanism set the lock.
    // Memcached reads TTLs above 30 days as absolute timestamps.
    private static final int LOCK_RECORD_RETENTION = 30 * 24 * 3600;
    // The record must outlive the lock, so locks are capped one day below the retention.
    private static final int MAX_LOCK_EXP_TIME = LOCK_RECORD_RETENTION - 24 * 3600;
    private static AuthenticationService authenticationService = CdiUtil.bean(AuthenticationService.class);
    private static UserService userService = CdiUtil.bean(UserService.class);
    private static CacheService cacheService = CdiUtil.bean(CacheService.class);
    private String INVALID_LOGIN_COUNT_ATTRIBUTE = "jansCountInvalidLogin";
    private int DEFAULT_MAX_LOGIN_ATTEMPT = 3;
    private int DEFAULT_LOCK_EXP_TIME = 180;

    private HashMap<String, String> flowConfig;
    private String authenticatedUserId;

    public JansPasswordService(HashMap config) {
        logger.info("Flow config provided is  {}.", config);
        flowConfig = config;
        DEFAULT_MAX_LOGIN_ATTEMPT = flowConfig.get("maxLoginAttempt") != null ? Integer.valueOf(flowConfig.get("maxLoginAttempt").toString()) : DEFAULT_MAX_LOGIN_ATTEMPT;
        DEFAULT_LOCK_EXP_TIME = flowConfig.get("lockExpTime") != null ? Integer.valueOf(flowConfig.get("lockExpTime").toString()) : DEFAULT_LOCK_EXP_TIME;
        if (DEFAULT_LOCK_EXP_TIME > MAX_LOCK_EXP_TIME) {
            logger.warn("lockExpTime {} exceeds the supported maximum; using {} seconds.", DEFAULT_LOCK_EXP_TIME, MAX_LOCK_EXP_TIME);
            DEFAULT_LOCK_EXP_TIME = MAX_LOCK_EXP_TIME;
        }
    }

    public JansPasswordService() {
    }

    @Override
    public boolean validate(String username, String password) {
        logger.info("Validating user credentials.");
        authenticatedUserId = null;
        boolean hasLogin = authenticationService.authenticate(username, password);
        if (!hasLogin) {
            return false;
        }
        User currentUser = authenticationService.getAuthenticatedUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            logger.warn("Authenticated user could not be resolved.");
            return false;
        }
        authenticatedUserId = currentUser.getUserId();
        if (Boolean.parseBoolean(String.valueOf((Object) flowConfig.get("enableLock")))) {
            logger.info("Credentials are valid and user account locked feature is activated");
            // Identity's user may be a partial or pre-login snapshot; merging it would drop attributes.
            User freshUser = userService.getUser(authenticatedUserId);
            if (freshUser != null) {
                userService.setCustomAttribute(freshUser, INVALID_LOGIN_COUNT_ATTRIBUTE, 0);
                userService.updateUser(freshUser);
            }
            logger.info("Invalid login count reset to zero for {} .", authenticatedUserId);
        }
        return true;
    }

    @Override
    public String getAuthenticatedUserId() {
        return authenticatedUserId;
    }

    @Override
    public String lockAccount(String username) {
        User currentUser = userService.getUser(username);
        if (currentUser == null) {
            logger.info("User {} not found. Cannot lock account.", username);
            return "Authenticaton failed";
        }          
        int currentFailCount = 1;
        String invalidLoginCount = getCustomAttribute(currentUser, INVALID_LOGIN_COUNT_ATTRIBUTE);
        if (invalidLoginCount != null) {
            currentFailCount = Integer.parseInt(invalidLoginCount) + 1;
        }
        String currentStatus = currentUser.getStatus() != null ? currentUser.getStatus().getValue() : null;
        logger.info("Current user status is: {}", currentStatus);
        if (currentStatus == null) {
            logger.info("User status is not set. Marking the user status as inactive.");
            currentUser.setStatus(GluuStatus.INACTIVE); // this line of code required to save user's status
            setCustomAttribute(currentUser, JANS_STATUS, INACTIVE);
            return "User status is not set so marking the user as inactive. Contact administrator to make user account active.";
        }
        if (currentFailCount < DEFAULT_MAX_LOGIN_ATTEMPT) {
            int remainingCount = DEFAULT_MAX_LOGIN_ATTEMPT - currentFailCount;
            logger.info("Remaining login count: {} for user {}", remainingCount, username);
            if (remainingCount > 0 && currentStatus.equalsIgnoreCase(GluuStatus.ACTIVE.getValue())) {
                setCustomAttribute(currentUser, INVALID_LOGIN_COUNT_ATTRIBUTE, String.valueOf(currentFailCount));
                logger.info("{}  more attempt(s) before account is LOCKED!", remainingCount);
            }
            return "You have " + remainingCount + " more attempt(s) before your account is locked.";
        }
        if (currentFailCount >= DEFAULT_MAX_LOGIN_ATTEMPT && currentStatus.equalsIgnoreCase(GluuStatus.ACTIVE.getValue())) {
            logger.info("Locking {} account for {} seconds.", username, DEFAULT_LOCK_EXP_TIME);
            long lockExpiresAt = System.currentTimeMillis() + DEFAULT_LOCK_EXP_TIME * 1000L;
            currentUser.setStatus(GluuStatus.INACTIVE); // this line of code required to save user's status
            User lockedUser = setCustomAttribute(currentUser, JANS_STATUS, INACTIVE);
            // Any later change to the entry (e.g. an administrator disable) bumps updatedAt and blocks auto-unlock
            cacheService.put(LOCK_RECORD_RETENTION, lockCacheKey(currentUser),
                    lockExpiresAt + ":" + updatedAtMillis(lockedUser));
            return "Your account has been locked.";
        }
        if (currentFailCount >= DEFAULT_MAX_LOGIN_ATTEMPT && currentStatus.equalsIgnoreCase(GluuStatus.INACTIVE.getValue())) {
            logger.info("User {} account is already locked. Checking if we can unlock", username);
            String lockKey = lockCacheKey(currentUser);
            Object lockRecord = cacheService.get(lockKey);
            if (lockRecord == null) {
                logger.info("User {} account was not locked by this flow. Not unlocking.", username);
                return null;
            }
            String[] recordParts = lockRecord.toString().split(":");
            long lockExpiresAt;
            long lockedUpdatedAt;
            try {
                lockExpiresAt = Long.parseLong(recordParts[0]);
                lockedUpdatedAt = Long.parseLong(recordParts[1]);
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                logger.warn("Invalid lock record for user {}. Not unlocking.", username);
                return null;
            }
            if (recordParts.length != 2 || lockedUpdatedAt < 0 || lockedUpdatedAt != updatedAtMillis(currentUser)) {
                logger.info("User {} account changed after this flow locked it. Not unlocking.", username);
                cacheService.remove(lockKey);
                return null;
            }
            if (System.currentTimeMillis() >= lockExpiresAt) {
                logger.info("Unlocking user {} account", username);
                cacheService.remove(lockKey);
                currentUser.setStatus(GluuStatus.ACTIVE); // this line of code required to save user's status
                setCustomAttribute(currentUser, JANS_STATUS, ACTIVE);
                setCustomAttribute(currentUser, INVALID_LOGIN_COUNT_ATTRIBUTE, "0");
                return "Your account  is now unlock. Try login ";
            }

        }
        return null;
    }

    private static long updatedAtMillis(User user) {
        return user != null && user.getUpdatedAt() != null ? user.getUpdatedAt().getTime() : -1L;
    }

    private static String lockCacheKey(User user) {
        return CACHE_PREFIX + user.getDn();
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
