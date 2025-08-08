---
tags:
  - administration
  - developer
  - script-catalog
---
# Logout Status Jwt

By overriding the interface methods in [`LogoutStatusJwtType`](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/token/LogoutStatusJwtType.java) inside a custom script you can

1. Enable transformation of claims and values in logout_status_jwt, e.g. add a custom claim to an `logout_status_jwt`, change the `sub` value, or remove the `nonce`.  
      
2. Set a specific logout_status_jwt lifetime

## Interface

### Methods

The LogoutStatusJwtType interception script extends the base script type with the `init`, `destroy` and `getApiVersion` methods:

| Inherited Methods | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

The `configurationAttributes` parameter is `java.util.Map<String, SimpleCustomProperty>`.

The LogoutStatusJwtType interception script also adds the following method(s):

|Method |Method description|
|:-----|:------|
| `def modifyPayload(self, jsonWebResponse, context)`| Used to modify logout_status_jwt claims. `jsonWebResponse` is `io.jans.as.model.token.JsonWebResponse`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`|
| `def getLifetimeInSeconds(self, context)`| Used to provide lifetime of logout_status_jwt. Value must be more then 0 or otherwise it's ignored by AS. (Lifetime will be set by `logoutStatusJwtLiftime` global AS configuration property.)<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`|


## Common Use Case

## Script Type: Java 

### Add/Modify claims inside logout_status_jwt

```java
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.LogoutStatusJwtType;
import io.jans.service.custom.script.CustomScriptManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class LogoutStatusJwt implements LogoutStatusJwtType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     *
     * @param jsonWebResponse refers to io.jans.as.model.token.JsonWebResponse
     * @param context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if logout_status_jwt should be created or false to forbid logout_status_jwt creation.
     */
    @Override
    public boolean modifyPayload(Object jsonWebResponse, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        JsonWebResponse jwr = (JsonWebResponse) jsonWebResponse;
        jwr.getClaims().setClaim("custom_claim", "custom_value");

        return true;
    }

    /**
     *
     * @param context context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return lifetime of logout_status_jwt in seconds. It must be more then 0 or otherwise it will be ignored by server.
     */
    @Override
    public int getLifetimeInSeconds(Object context) {
        boolean condition = false; // under some condition return 1 day lifetime
        if (condition) {
            return 86400;
        }
        return 0;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized LogoutStatusJwt Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized LogoutStatusJwt Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed LogoutStatusJwt Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}

```

