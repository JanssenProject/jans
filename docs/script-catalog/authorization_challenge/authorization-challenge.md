---
tags:
  - administration
  - developer
  - script-catalog
---

# Authorization Challenge Custom Script

## Overview

The Jans-Auth server implements [OAuth 2.0 for First-Party Applications](https://www.ietf.org/archive/id/draft-parecki-oauth-first-party-apps-02.html).
This script is used to control/customize Authorization Challenge Endpoint.

## Behavior

In request to Authorization Challenge Endpoint to is expected to have `acr_values` request parameter which specifies name of the custom script.
If parameter is absent or AS can't find script with this name then it falls back to script with name `default_challenge`.

This script is provided during installation and performs basic `username`/`password` authentication.

```
POST /jans-auth/restv1/authorize-challenge HTTP/1.1
Host: yuriyz-fond-skink.gluu.info

client_id=999e13b8-f4a2-4fed-ad3c-6c88bd2c92ea&scope=openid+profile+address+email+phone+user_name&state=b4a41b29-51c8-4354-9c8c-fda38b4dbd43&nonce=3a56f8d0-f78e-4b15-857c-3e792801be68&acr_values=&request_session_id=false&password=secret&username=admin
```

There is **authorizationChallengeDefaultAcr** AS configuration property which allows to change fallback script name from `default_challenge` to some other value (value must be valid script name present on AS).


## Interface
The Authorization Challenage script implements the [AuthorizationChallenageType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authzchallenge/AuthorizationChallengeType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`def authorize(self, context)`| Called when the request is received. |
|`def getAuthenticationMethodClaims(self, context)`| Called to get authn method claims. It is injected into `id_token`. Returns key-value map. |
|`def prepareAuthzRequest(self, context)`| Prepared authorization request before `authorize` method. It's good place to restore data from session if needed. |

`authorize` method returns true/false which indicates to server whether to issue `authorization_code` in response or not.

If parameters is not present then error has to be created and `false` returned.
If all is good script has to return `true` and it's strongly recommended to set user `context.getExecutionContext().setUser(user);` so AS can keep tracking what exactly user is authenticated.

`prepareAuthzRequest` should typically be used for authorization request manipulation before `authorize` method is invoked. 
Also if there is multi-step flow where some data are stored in `session` object, it is good place to restore data from session into request (please find example in sample below). 

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |


## Common Use Case: Authorize user by username/password 

### Script Type: Java

```java
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
import java.util.HashMap;
import java.util.UUID;

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
        DeviceSession authorizationChallengeSessionObject = context.getAuthzRequest().getDeviceSessionObject();
        if (authorizationChallengeSessionObject != null) {
            prepareDeviceSession(context, authorizationChallengeSessionObject);
            return String.format(",\"auth_session\":\"%s\"", authorizationChallengeSessionObject.getId());
        } else if (context.getAuthzRequest().isUseDeviceSession()) {
            authorizationChallengeSessionObject = prepareDeviceSession(context, null);
            return String.format(",\"auth_session\":\"%s\"", authorizationChallengeSessionObject.getId());
        }
        return "";
    }

    private DeviceSession prepareDeviceSession(ExternalScriptContext context, DeviceSession authorizationChallengeSessionObject) {
        DeviceSessionService deviceSessionService = CdiUtil.bean(DeviceSessionService.class);
        boolean newSave = authorizationChallengeSessionObject == null;
        if (newSave) {
            authorizationChallengeSessionObject = deviceSessionService.newDeviceSession();
        }

        String username = context.getHttpRequest().getParameter(USERNAME_PARAMETER);
        if (StringUtils.isNotBlank(username)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put(USERNAME_PARAMETER, username);
        }

        String password = context.getHttpRequest().getParameter(PASSWORD_PARAMETER);
        if (StringUtils.isNotBlank(password)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put(PASSWORD_PARAMETER, password);
        }
        
        String clientId = context.getHttpRequest().getParameter("client_id");
        if (StringUtils.isNotBlank(clientId)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put("client_id", clientId);
        }
        
        String acrValues = context.getHttpRequest().getParameter("acr_values");
        if (StringUtils.isNotBlank(acrValues)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put("acr_values", acrValues);
        }

        if (newSave) {
            deviceSessionService.persist(authorizationChallengeSessionObject);
        } else {
            deviceSessionService.merge(authorizationChallengeSessionObject);
        }

        return authorizationChallengeSessionObject;
    }

    private String getParameterFromDeviceSession(ExternalScriptContext context, String parameterName) {
        final DeviceSession authorizationChallengeSessionObject = context.getAuthzRequest().getDeviceSessionObject();
        if (authorizationChallengeSessionObject != null) {
            return authorizationChallengeSessionObject.getAttributes().getAttributes().get(parameterName);
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
}

```

## Multi-step authorization by username and OTP 

### Script Type: Java

```text
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
import java.util.HashMap;
import java.util.UUID;

/**
 * Script is to demo 2 steps flow:
 * 1) First step -> send username
 * 2) Second step -> send OTP
 *
 * AS tracks data by 'auth_session'. See "prepareDeviceSession()" method implemention for details.
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
        //    Note: prepareDeviceSession method implemention
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
        DeviceSession authorizationChallengeSessionObject = context.getAuthzRequest().getDeviceSessionObject();
        if (authorizationChallengeSessionObject != null) {
            prepareDeviceSession(context, authorizationChallengeSessionObject);
            return String.format(",\"auth_session\":\"%s\"", authorizationChallengeSessionObject.getId());
        } else if (context.getAuthzRequest().isUseDeviceSession()) {
            authorizationChallengeSessionObject = prepareDeviceSession(context, null);
            return String.format(",\"auth_session\":\"%s\"", authorizationChallengeSessionObject.getId());
        }
        return "";
    }

    private DeviceSession prepareDeviceSession(ExternalScriptContext context, DeviceSession authorizationChallengeSessionObject) {
        DeviceSessionService deviceSessionService = CdiUtil.bean(DeviceSessionService.class);
        boolean newSave = authorizationChallengeSessionObject == null;
        if (newSave) {
            authorizationChallengeSessionObject = deviceSessionService.newDeviceSession();
        }

        String username = context.getHttpRequest().getParameter(USERNAME_PARAMETER);
        if (StringUtils.isNotBlank(username)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put(USERNAME_PARAMETER, username);
        }

        String otp = context.getHttpRequest().getParameter(OTP_PARAMETER);
        if (StringUtils.isNotBlank(otp)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put(OTP_PARAMETER, otp);
        }
        
        String clientId = context.getHttpRequest().getParameter("client_id");
        if (StringUtils.isNotBlank(clientId)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put("client_id", clientId);
        }
        
        String acrValues = context.getHttpRequest().getParameter("acr_values");
        if (StringUtils.isNotBlank(acrValues)) {
            authorizationChallengeSessionObject.getAttributes().getAttributes().put("acr_values", acrValues);
        }

        if (newSave) {
            deviceSessionService.persist(authorizationChallengeSessionObject);
        } else {
            deviceSessionService.merge(authorizationChallengeSessionObject);
        }

        return authorizationChallengeSessionObject;
    }

    private String getParameterFromDeviceSession(ExternalScriptContext context, String parameterName) {
        final DeviceSession authorizationChallengeSessionObject = context.getAuthzRequest().getDeviceSessionObject();
        if (authorizationChallengeSessionObject != null) {
            return authorizationChallengeSessionObject.getAttributes().getAttributes().get(parameterName);
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
            final String scopeFromSession = sessionAttributes.get("scope");
            if (StringUtils.isNotBlank(scopeFromSession) && StringUtils.isBlank(context.getAuthzRequest().getScope())) {
                context.getAuthzRequest().setScope(scopeFromSession);
            }
        }
    }
}

```


## Sample Scripts
- [AuthorizationChallenge](../../../script-catalog/authorization_challenge/AuthorizationChallenge.java)
- [Multi-step AuthorizationChallenge](../../../script-catalog/authorization_challenge/multi_step/AuthorizationChallenge.java)