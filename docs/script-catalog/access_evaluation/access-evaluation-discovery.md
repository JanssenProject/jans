---
tags:
  - administration
  - developer
  - script-catalog
---

# Access Evaluation Discovery Custom Script

## Overview

This script is used to control Access Evaluation Discovery Endpoint (`/.well-known/authzen-configuration`).

**Sample request**
```
GET /.well-known/authzen-configuration HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json

{"access_evaluation_v1_endpoint":"https://happy-example.gluu.info/jans-auth/restv1/evaluation"}
```



## Interface
The Access Evaluation Discovery script implements the [AccessEvaluationDiscoveryType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authzen/AccessEvaluationDiscoveryType.java) interface.
This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
| `def modifyResponse(self, responseAsJsonObject, context)` | This method is called after discovery response is ready. This method can modify discovery response.<br/>`responseAsJsonObject` is `org.codehaus.jettison.json.JSONObject`<br/> `context` is `io.jans.as.server.model.common.ExecutionContext` |


`modifyResponse` method returns `true` to access modification or `false` to revert all changes.


### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |


## Sample Demo Custom Script

### Script Type: Java

```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzen.AccessEvaluationDiscoveryType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AccessEvaluationDiscovery implements AccessEvaluationDiscoveryType {

    private static final Logger log = LoggerFactory.getLogger(AccessEvaluationDiscovery.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Destroy of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public int getApiVersion() {
        log.info("getApiVersion AccessEvaluationDiscovery Java custom script: 11");
        return 11;
    }

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        scriptLogger.info("write to script logger");
        JSONObject response = (JSONObject) responseAsJsonObject;
        response.accumulate("key_from_java", "value_from_script_on_java");
        return true;
    }
}
```


## Sample Scripts
- [Access Evaluation Discovery](../../../script-catalog/access_evaluation/AccessEvaluationDiscovery.java)
