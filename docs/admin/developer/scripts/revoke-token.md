---
tags:
  - administration
  - developer
  - scripts
---

## Overview
Revoke Token scripts allow injecting custom logic during token revocation.

## Interface
The revoke token script implements the [RevokeTokenType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/revoke/RevokeTokenType.java) interface. This extends methods from the base script type in addition to adding new method:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def revoke(self, context)` | Token is revoked if this method returns True, skipped otherwise |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
| `context` | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |

## Use Case: Basic Token Revocation
This script has been adapted from the Gluu Server [sample revoke token script](https://github.com/GluuFederation/community-edition-setup/blob/version_4.4.0/static/extension/revoke_token/revoke_token.py).

### Script Type: Python
```python
from io.jans.model.custom.script.type.revoke import RevokeTokenType
from java.lang import String

class RevokeToken(RevokeTokenType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Revoke Token script. Initializing ..."
        print "Revoke Token script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Revoke Token script. Destroying ..."
        print "Revoke Token script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # This method is called during Revoke Token call.
    # If True is returned, token is revoked. If False is returned, revoking is skipped.
    def revoke(self, context):
        return True
```

### Script Type: Java
```java
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.revoke.RevokeTokenType;
import io.jans.service.custom.script.CustomScriptManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RevokeToken implements RevokeTokenType {

    private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Token Revoke. Initializing...");
        log.info("Token Revoke. Initialized");
        return true;
	}

	@Override
	public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Token Revoke. Initializing...");
        log.info("Token Revoke. Initialized");
        return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Token Revoke. Destroying...");
        log.info("Token Revoke. Destroyed.");
        return true;
	}

	@Override
	public int getApiVersion() {
		return 11;
	}

    @Override
    public boolean revoke(Object context) {
        return true;
    }
}
```