---
tags:
  - administration
  - developer
  - script-catalog
---

# Create User Custom Script


"CreateUser" custom script is used when Authorization Endpoint is called with `prompt=create` parameter.
In this case AS shows user registration form instead of authn/authz UI.
"CreateUser" custom script allows inject/modify user registration process.

## Behavior

When AS receives request on Authorization Endpoint with `prompt=create` it redirect to user creation page.

Example of initial request (line breaks are for display purpose only)
```curl
https://sample.as.com/jans-auth/restv1/authorize?
    response_type=code+token&
    client_id=9999&
    scope=openid+profile&
    redirect_uri=https://cb.example.com&
    state=0ba6bba8-d2a4-44e6-8192-57012e41d506&
    nonce=963ecc9f-b1d8-4bb0-a0b5-d53be34e7e4e&
    acr_values=simple_password_auth
    &prompt=create
```
 
Sample redirect to user creation page (line breaks are for display purpose only)
```curl
https://sample.as.com/jans-auth/createUser.htm?
    scope=openid+profile&
    acr_values=simple_password_auth&
    response_type=code+token&
    redirect_uri=https%3A%2F%2Fcb.example.com&
    state=0ba6bba8-d2a4-44e6-8192-57012e41d506&
    nonce=963ecc9f-b1d8-4bb0-a0b5-d53be34e7e4e&    
    client_id=9999
```

It's possible to redirect to custom page instead of built-in `/createUser.htm`. For this
use `getCreateUserPage` method.

```java
    public String getCreateUserPage(Object context) {
        return "/customCreateUser";
    }
```

Once user is redirected, it enters all details and hits action button. 
At this point `prepare` method is called. It can be used to make preparetions needed for business logic.
`prepare` method must return `true`. If `false` is returned, AS will show error and user registration will be interrupted.

```java
    public boolean prepare(Object context) {
        // make some preparations
        return true;
    }
```

If `prepare` method returned `true`, then `createUser` method is called. It can be used to prepare `user` object for persistence.

```java
    public boolean createUser(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        final User user = scriptContext.getExecutionContext().getUser();
        final Map<String, String[]> parameterMap = scriptContext.getExecutionContext().getHttpRequest().getParameterMap();
        return true;
    }
```

`createUser` method must return `true` to proceed with persistence. If `false` is returned, new user is not created. Process is interrupted.

After successful user creation, user-agent is redirect to Authorization Endpoint which is same url as original request except 
`prompt=create` in it. Redirect url to Authorization Endpoint after user creation can be customized with `buildPostAuthorizeUrl` method.

```java
    public String buildPostAuthorizeUrl(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        final User user = scriptContext.getExecutionContext().getUser();
        final HttpServletRequest httpRequest = scriptContext.getExecutionContext().getHttpRequest();
        final Map<String, String[]> requestParameters = httpRequest.getParameterMap();

        final Map<String, String> parameters = new HashMap<>();
        // construct/fill parameters here

        try {
            RequestParameterService requestParameterService = CdiUtil.bean(RequestParameterService.class);
            return httpRequest.getContextPath() + "/restv1/authorize?" + requestParameterService.parametersAsString(parameters);
        } catch (Exception e) {
            scriptLogger.error("Failed to build post authorization url.", e);
            return null;
        }
    }
```

## Interface
The CreateUser script implements the [CreateUserType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/create_user/CreateUserType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`def getCreateUserPage(self, context)`| Called when the prompt=create authorization request is send and AS figuring out the right page to land the end-user
|`def prepare(self, context)`| Called when the form is filled and user hit submit button
|`def createUser(self, context)`| Called right before user object persistence which allows to modify user object
|`def buildPostAuthorizeUrl(self, context)`| Called after user persistence and construct authorization url to redirect end-user to


### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |


### Saample Scrip in Java

```java
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.RequestParameterService;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.createuser.CreateUserType;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Sample custom script for user creation on prompt=create
 *
 * @author Yuriy Z
 */
public class CreateUser implements CreateUserType {

    private static final Logger log = LoggerFactory.getLogger(CreateUser.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     * Returns custom page for user creation
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return custom page
     */
    @Override
    public String getCreateUserPage(Object context) {
        return "/customCreateUser";
    }

    /**
     * Preparetion of user create action (called right before page is shown)
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return whether preparetion is successful or not. If "false" is returned then error is shown.
     */
    @Override
    public boolean prepare(Object context) {
        // make some preparations
        return true;
    }

    /**
     * Method is called before user creation (persistence to DB).
     * "context.getExecutionContext().getUser()" allows to access and modify user object that
     * will be persisted.
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return whether user is created successfully. If "false" is returned, user creation is interrupted.
     */
    @Override
    public boolean createUser(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        final User user = scriptContext.getExecutionContext().getUser();
        final Map<String, String[]> parameterMap = scriptContext.getExecutionContext().getHttpRequest().getParameterMap();
        return true;
    }

    /**
     * Returns post authorization url. After user is created, user-agent is redirected to Authorization Endpoint. Here it can be customized/modified.
     *
     * @param context ExternalScriptContext, see https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java
     * @return authorization url
     */
    @Override
    public String buildPostAuthorizeUrl(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        final User user = scriptContext.getExecutionContext().getUser();
        final HttpServletRequest httpRequest = scriptContext.getExecutionContext().getHttpRequest();
        final Map<String, String[]> requestParameters = httpRequest.getParameterMap();

        final Map<String, String> parameters = new HashMap<>();
        // construct/fill parameters here

        try {
            RequestParameterService requestParameterService = CdiUtil.bean(RequestParameterService.class);
            return httpRequest.getContextPath() + "/restv1/authorize?" + requestParameterService.parametersAsString(parameters);
        } catch (Exception e) {
            scriptLogger.error("Failed to build post authorization url.", e);
            return null;
        }
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized CreateUser Java custom script.");
        return false;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized CreateUser Java custom script.");
        return false;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed CreateUser Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}

``` 

## Sample Scripts
- [Sample CreateUser script](../../../script-catalog/create_user/CreateUser.java)
