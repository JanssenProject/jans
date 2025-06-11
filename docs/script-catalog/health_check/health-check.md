---
tags:
  - administration
  - developer
  - script-catalog
---

# Create User Custom Script


"HealthCheck" custom script is used when Health Check Endpoint is called.
Custom script allows inject/modify response from Health Check Endpoint.

## Interface
The HealthCheck script implements the [HealthCheckType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/health_check/HealthCheckType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`def healthCheck(self, context)`| Returns response for health check endpoint


### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |


### Saample Scrip in Java

```java
package io.jans.as.server._scripts;

import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.health.HealthCheckType;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class HealthCheck implements HealthCheckType {

    private static final Logger log = LoggerFactory.getLogger(HealthCheck.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public String healthCheck(Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        HttpServletRequest httpRequest = scriptContext.getExecutionContext().getHttpRequest();

        String appStatus = "running";
        String dbStatus = "online";
        return String.format("{\"status\": \"%s\", \"db_status\":\"%s\"}", appStatus, dbStatus);
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized HealthCheck Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized HealthCheck Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed HealthCheck Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}

``` 

## Sample Scripts
- [Sample HealthCheck script](../../../script-catalog/health_check/HealthCheck.java)
