---
tags:
  - administration
  - developer
  - scripts
---

## Overview
The dynamic scope custom script allows the authorization server to generate a list of claims (and their values) on the fly, depending on circumstances such as the ID of the client requesting it, authenticated user's session parameters, values of other users' attributes, results of some calculations implementing specific business logic and/or requests to remote APIs or databases. Claims are then returned the usual way in a response to a call to the `/userinfo` endpoint. 

## Interface
The dynamic scope script implements the [DynamicScopeType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/scope/DynamicScopeType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def update(self, dynamicScopeContext, configurationAttributes)` | Main dynamic scope method. Peforms any needed logic, updates JSON Web Token and returns True if dynamic scope was added successfully, false otherwise. |
| `def getSupportedClaims(self, configurationAttributes)` | Returns an array of claims that are allowed to be added by the custom script |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
| `dynamicScopeContext` | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/DynamicScopeExternalContext.java) |

## Use case: Add dynamic scope with the `org_name` claim

This script has been adapted from the Gluu Server [sample dynamic scope script](https://gluu.org/docs/gluu-server/4.4/admin-guide/sample-dynamic-script.py)

### Script Type: Python
```python
from java.util import Arrays, ArrayList
from io.jans.model.custom.script.type.scope import DynamicScopeType


class DynamicScope(DynamicScopeType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, configurationAttributes):
        print "Dynamic scope. Initialization"

        print "Dynamic scope. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Dynamic scope. Destroy"
        print "Dynamic scope. Destroyed successfully"
        return True   

    def update(self, dynamicScopeContext, configurationAttributes):
        print "Dynamic scope. Update method"

        dynamicScopes = dynamicScopeContext.getDynamicScopes()
        user = dynamicScopeContext.getUser()
        jsonToken = dynamicScopeContext.getJsonToken()
        claims = jsonToken.getClaims()

        # Iterate through list of dynamic scopes in order to add custom scopes if needed
        print "Dynamic scope. Dynamic scopes:", dynamicScopes
        for dynamicScope in dynamicScopes:
            # Add organization name if there is scope = org_name
            if (StringHelper.equalsIgnoreCase(dynamicScope, "org_name")):
                claims.setClaim("org_name", "Test Value")
                continue

            # Add work phone if there is scope = work_phone

        return True

    def getApiVersion(self):
        return 1

    def getSupportedClaims(self, configurationAttributes):
        return Arrays.asList("org_name")
```

### Script Type: Java

```java
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.model.custom.script.type.scope.DynamicScopeType;
import io.jans.as.server.service.external.context.DynamicScopeExternalContext;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.util.StringHelper;




import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DynamicScope implements DynamicScopeType {

  private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);
	
    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
      log.info("Dynamic Scope. Initializing...");
      log.info("Dynamic Scope. Initialized");
      return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Dynamic Scope. Initializing...");
        log.info("Dynamic Scope. Initialized");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Dynamic Scope. Destroying...");
        log.info("Dynamic Scope. Destroyed.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    @Override
    public boolean update(Object dynamicScopeContext, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Dynamic Scope. Updating...");
        DynamicScopeExternalContext dynamicContext = (DynamicScopeExternalContext) dynamicScopeContext;
        ArrayList<String> dynamicScopes = (ArrayList<String>) dynamicContext.getDynamicScopes();
        JsonWebResponse jwt = dynamicContext.getJsonWebResponse();
        JwtClaims claims = jwt.getClaims();
        
        log.info("Dynamic Scope. Dynamic scopes: " + dynamicScopes.toString());
        for (String dynamicScope : dynamicScopes) {
            if (StringHelper.equalsIgnoreCase(dynamicScope, "org_name")) {
                claims.setClaim("org_name", "Test Value");
                continue;
            }
        }
        
        
        return true;
    }

    @Override
    public List<String> getSupportedClaims(Map<String, SimpleCustomProperty> configurationAttributes) {
        return Arrays.asList("org_name");
    }
}

```