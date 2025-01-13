import io.jans.as.common.model.common.User;
import io.jans.as.common.model.session.AuthorizationChallengeSession;
import io.jans.as.server.authorize.ws.rs.AuthorizationChallengeSessionService;
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
import java.util.HashMap;
import java.util.UUID;

/**
 * Script is to demo 2 steps flow:
 * 1) First step -> send username
 * 2) Second step -> send OTP
 *
 * AS tracks data by 'auth_session'. See "prepareAuthorizationChallengeSession()" method implemention for details.
 *
 * @author Yuriy Z
 */
public class AuthorizationChallenge implements AuthorizationChallengeType {

    public static final String USERNAME_PARAMETER = "username";
    public static final String OTP_PARAMETER = "otp";

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

        // 1. As first step we get username
        final String username = getParameterOrCreateError(context, USERNAME_PARAMETER);
        if (StringUtils.isBlank(username)) {
            return false;
        }

        // 2. During first execution OTP is not present, so error will be returned with auth_session (which has saved username)
        //    Note: prepareAuthorizationChallengeSession method implemention
        final String otp = getParameterOrCreateError(context, OTP_PARAMETER);
        if (StringUtils.isBlank(otp)) {
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

        // for simplicity OTP is here just username password but in real world scenario real OTP validation is needed.
        final boolean ok = entryManager.authenticate(user.getDn(), User.class, otp);
        if (ok) {
            context.getExecutionContext().setUser(user); // <- IMPORTANT : without user set, user relation will not be associated with token
            scriptLogger.trace("User {} is authenticated successfully.", username);
            return true;
        }

        // 3. not ok -> set error which explains what is wrong and return false
        scriptLogger.trace("Failed to authenticate user {}. Please check username and OTP.", username);
        createError(context, "username_or_otp_invalid");
        return false;
    }

    private String getParameterOrCreateError(ExternalScriptContext context, String parameterName) {
        String value = context.getHttpRequest().getParameter(parameterName);

        if (StringUtils.isBlank(value)) {
            scriptLogger.trace("No '{}' parameter in request", parameterName);
            value = getParameterFromAuthorizationChallengeSession(context, parameterName);
        }

        if (StringUtils.isBlank(value)) {
            scriptLogger.trace("{} is not provided", parameterName);
            createError(context, String.format("%s_required", parameterName));
            return null;
        }

        return value;
    }

    private void createError(ExternalScriptContext context, String errorCode) {
        String sessionPart = prepareAuthorizationChallengeSessionSubJson(context);

        final String entity = String.format("{\"error\": \"%s\"%s}", errorCode, sessionPart);
        context.createWebApplicationException(401, entity);
    }

    private String prepareAuthorizationChallengeSessionSubJson(ExternalScriptContext context) {
        AuthorizationChallengeSession sessionObject = context.getAuthzRequest().getAuthorizationChallengeSessionObject();
        if (sessionObject != null) {
            prepareAuthorizationChallengeSession(context, sessionObject);
            return String.format(",\"auth_session\":\"%s\"", sessionObject.getId());
        } else if (context.getAuthzRequest().isUseAuthorizationChallengeSession()) {
            sessionObject = prepareAuthorizationChallengeSession(context, null);
            return String.format(",\"auth_session\":\"%s\"", sessionObject.getId());
        }
        return "";
    }

    private AuthorizationChallengeSession prepareAuthorizationChallengeSession(ExternalScriptContext context, AuthorizationChallengeSession sessionObject) {
        AuthorizationChallengeSessionService authorizationChallengeSessionService = CdiUtil.bean(AuthorizationChallengeSessionService.class);
        boolean newSave = sessionObject == null;
        if (newSave) {
            sessionObject = authorizationChallengeSessionService.newAuthorizationChallengeSession();
        }

        String username = context.getHttpRequest().getParameter(USERNAME_PARAMETER);
        if (StringUtils.isNotBlank(username)) {
            sessionObject.getAttributes().getAttributes().put(USERNAME_PARAMETER, username);
        }

        String otp = context.getHttpRequest().getParameter(OTP_PARAMETER);
        if (StringUtils.isNotBlank(otp)) {
            sessionObject.getAttributes().getAttributes().put(OTP_PARAMETER, otp);
        }

        String clientId = context.getHttpRequest().getParameter("client_id");
        if (StringUtils.isNotBlank(clientId)) {
            sessionObject.getAttributes().getAttributes().put("client_id", clientId);
        }

        String acrValues = context.getHttpRequest().getParameter("acr_values");
        if (StringUtils.isNotBlank(acrValues)) {
            sessionObject.getAttributes().getAttributes().put("acr_values", acrValues);
        }

        if (newSave) {
            authorizationChallengeSessionService.persist(sessionObject);
        } else {
            authorizationChallengeSessionService.merge(sessionObject);
        }

        return sessionObject;
    }

    private String getParameterFromAuthorizationChallengeSession(ExternalScriptContext context, String parameterName) {
        final AuthorizationChallengeSession sessionObject = context.getAuthzRequest().getAuthorizationChallengeSessionObject();
        if (sessionObject != null) {
            return sessionObject.getAttributes().getAttributes().get(parameterName);
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

    @Override
    public Map<String, String> getAuthenticationMethodClaims(Object context) {
        return new HashMap<>();
    }

    @Override
    public void prepareAuthzRequest(Object scriptContext) {
        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        final AuthorizationChallengeSession sessionObject = context.getAuthzRequest().getAuthorizationChallengeSessionObject();
        if (sessionObject != null) {
            final Map<String, String> sessionAttributes = sessionObject.getAttributes().getAttributes();

            // set scope from session into request object
            final String scopeFromSession = sessionAttributes.get("scope");
            if (StringUtils.isNotBlank(scopeFromSession) && StringUtils.isBlank(context.getAuthzRequest().getScope())) {
                context.getAuthzRequest().setScope(scopeFromSession);
            }
        }
    }
}
