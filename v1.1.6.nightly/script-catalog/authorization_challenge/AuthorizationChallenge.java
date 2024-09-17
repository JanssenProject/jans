import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.DeviceSession;
import io.jans.as.server.authorize.ws.rs.DeviceSessionService;
import io.jans.as.server.service.UserService;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzchallenge.AuthorizationChallengeType;
import io.jans.orm.PersistenceEntryManager;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

/**
 * @author Yuriy Z
 */
public class AuthorizationChallenge implements AuthorizationChallengeType {

    public static final String USERNAME_PARAMETER = "username";
    public static final String PASSWORD_PARAMETER = "password";

    private static final Logger log = LoggerFactory.getLogger(AuthorizationChallenge.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * Return true if Authorization Challenge Endpoint should return code successfully or otherwise false if error should be returned.
     * <p>
     * Implementation of this method should consist of 3 main parts:
     * 1. validate all parameters are present and if not -> set error and return false
     * 2. main authorization logic, if ok -> set authorized user into "context.getExecutionContext().setUser(user);" and return true
     * 3. if not ok -> set error which explains what is wrong and return false
     *
     * @param scriptContext ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return true if Authorization Challenge Endpoint should return code successfully or otherwise false if error should be returned.
     */
    @Override
    public boolean authorize(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;

        // 1. validate all required parameters are present
        final String username = getParameterOrCreateError(context, USERNAME_PARAMETER);
        if (StringUtils.isBlank(username)) {
            return false;
        }

        final String password = getParameterOrCreateError(context, PASSWORD_PARAMETER);
        if (StringUtils.isBlank(password)) {
            return false;
        }

        scriptLogger.trace("All required parameters are present");

        // 2. main authorization logic, if ok -> set authorized user into "context.getExecutionContext().setUser(user);" and return true
        UserService userService = CdiUtil.bean(UserService.class);
        PersistenceEntryManager entryManager = CdiUtil.bean(PersistenceEntryManager.class);

        final User user = userService.getUser(username);
        if (user == null) {
            scriptLogger.trace("User is not found by username {}", username);
            createError(context, "username_invalid");
            return false;
        }

        final boolean ok = entryManager.authenticate(user.getDn(), User.class, password);
        if (ok) {
            context.getExecutionContext().setUser(user); // <- IMPORTANT : without user set, user relation will not be associated with token
            scriptLogger.trace("User {} is authenticated successfully.", username);
            return true;
        }

        // 3. not ok -> set error which explains what is wrong and return false
        scriptLogger.trace("Failed to authenticate user {}. Please check username and password.", username);
        createError(context, "username_or_password_invalid");
        return false;
    }

    private String getParameterOrCreateError(ExternalScriptContext context, String parameterName) {
        String value = context.getHttpRequest().getParameter(parameterName);

        if (StringUtils.isBlank(value)) {
            scriptLogger.trace("No '{}' parameter in request", parameterName);
            value = getParameterFromDeviceSession(context, parameterName);
        }

        if (StringUtils.isBlank(value)) {
            scriptLogger.trace("{} is not provided", parameterName);
            createError(context, String.format("%s_required", parameterName));
            return null;
        }

        return value;
    }

    private void createError(ExternalScriptContext context, String errorCode) {
        String deviceSessionPart = prepareDeviceSessionSubJson(context);

        final String entity = String.format("{\"error\": \"%s\"%s}", errorCode, deviceSessionPart);
        context.createWebApplicationException(401, entity);
    }

    private String prepareDeviceSessionSubJson(ExternalScriptContext context) {
        DeviceSession deviceSessionObject = context.getAuthzRequest().getDeviceSessionObject();
        if (deviceSessionObject != null) {
            prepareDeviceSession(context, deviceSessionObject);
            return String.format(",\"device_session\":\"%s\"", deviceSessionObject.getId());
        } else if (context.getAuthzRequest().isUseDeviceSession()) {
            deviceSessionObject = prepareDeviceSession(context, null);
            return String.format(",\"device_session\":\"%s\"", deviceSessionObject.getId());
        }
        return "";
    }

    private DeviceSession prepareDeviceSession(ExternalScriptContext context, DeviceSession deviceSessionObject) {
        DeviceSessionService deviceSessionService = CdiUtil.bean(DeviceSessionService.class);
        boolean newSave = deviceSessionObject == null;
        if (newSave) {
            deviceSessionObject = deviceSessionService.newDeviceSession();
        }

        String username = context.getHttpRequest().getParameter(USERNAME_PARAMETER);
        if (StringUtils.isNotBlank(username)) {
            deviceSessionObject.getAttributes().getAttributes().put(USERNAME_PARAMETER, username);
        }

        String password = context.getHttpRequest().getParameter(PASSWORD_PARAMETER);
        if (StringUtils.isNotBlank(password)) {
            deviceSessionObject.getAttributes().getAttributes().put(PASSWORD_PARAMETER, password);
        }

        String clientId = context.getHttpRequest().getParameter("client_id");
        if (StringUtils.isNotBlank(clientId)) {
            deviceSessionObject.getAttributes().getAttributes().put("client_id", clientId);
        }

        String acrValues = context.getHttpRequest().getParameter("acr_values");
        if (StringUtils.isNotBlank(acrValues)) {
            deviceSessionObject.getAttributes().getAttributes().put("acr_values", acrValues);
        }

        if (newSave) {
            deviceSessionService.persist(deviceSessionObject);
        } else {
            deviceSessionService.merge(deviceSessionObject);
        }

        return deviceSessionObject;
    }

    private String getParameterFromDeviceSession(ExternalScriptContext context, String parameterName) {
        final DeviceSession deviceSessionObject = context.getAuthzRequest().getDeviceSessionObject();
        if (deviceSessionObject != null) {
            return deviceSessionObject.getAttributes().getAttributes().get(parameterName);
        }
        return null;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized Default AuthorizationChallenge Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized Default AuthorizationChallenge Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed Default AuthorizationChallenge Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    /**
     * Returns claims represented by key-value map. Claims are added to id_token jwt.
     *
     * @param context external script context
     * @return authentication method claims represented by key-value map.
     */
    @Override
    public Map<String, String> getAuthenticationMethodClaims(Object context) {
        return new HashMap<>();
    }
}