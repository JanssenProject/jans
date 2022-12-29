---
tags:
  - administration
  - developer
  - scripts
  - ResourceOwnerPasswordCredentials
---

## Overview
Resource Owner Password Credentials script allows modifying the behavior of Resource Owner Password Credentials Grant ([RFC 6749](https://www.rfc-editor.org/rfc/rfc6749#section-4.3)).

The script is invoked after normal authentication and can either leave current result or change it - authenticate if not authenticated - it should return True and optionally set user (via `context.setUser(user)`).

## Interface
The ROPC script implements the [ResourceOwnerPasswordCredentialsType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/owner/ResourceOwnerPasswordCredentialsType.java) interface. This extends methods from the base script type in addition to adding new method:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def authenticate(self, context)` | This method is called after normal ROPC authentication. This method can cancel normal authentication if it returns false and sets `context.setUser(null)` |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
| `context` | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalResourceOwnerPasswordCredentialsContext.java) |

## Use case: Basic ROPC authentication script

This script has been adapted from the Gluu Server [sample ROPC script](https://github.com/GluuFederation/community-edition-setup/blob/version_4.4.0/static/extension/resource_owner_password_credentials/resource_owner_password_credentials.py)

### Script Type: Python

```python
from io.jans.model.custom.script.type.owner import ResourceOwnerPasswordCredentialsType
from io.jans.as.server.service import AuthenticationService
from io.jans.service.cdi.util import CdiUtil
from java.lang import String

class ResourceOwnerPasswordCredentials(ResourceOwnerPasswordCredentialsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "ROPC script. Initializing ..."

        self.usernameParamName = "username"
        self.passwordParamName = "password"

        print "ROPC script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "ROPC script. Destroying ..."
        print "ROPC script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns True and set user into context when user authenticated succesfully
    # Returns False when user not authenticated or it's needed to cancel notmal flow
    def authenticate(self, context):
        print "ROPC script. Authenticate"
        deviceIdParam = context.getHttpRequest().getParameterValues("device_id")
        if deviceIdParam != None and (deviceIdParam.length > 0 ):
            result = deviceIdParam[0] == "device_id_1"
            if not result:
                return False

            # Set authenticated user in context
            # context.setUser(user)
            return True

        # Do generic authentication in other cases
        authService = CdiUtil.bean(AuthenticationService)

        username = context.getHttpRequest().getParameter(self.usernameParamName)
        password = context.getHttpRequest().getParameter(self.passwordParamName)
        result = authService.authenticate(username, password)
        if not result:
            print "ROPC script. Authenticate. Could not authenticate user '%s' " % username
            return False

        context.setUser(authService.getAuthenticatedUser())

        return True
```

### Script Type: Java
```java
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.owner.ResourceOwnerPasswordCredentialsType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.as.server.service.AuthenticationService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.as.server.service.external.context.ExternalResourceOwnerPasswordCredentialsContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceOwnerPasswordCredentials implements ResourceOwnerPasswordCredentialsType {
	
	private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);
	
	private final String usernameParamName = "username";
	private final String passwordParamName = "password";

	@Override
	public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("ROPC Script. Initializing...");
        log.info("ROPC Script. Initialized");
        return true;
	}

	@Override
	public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("ROPC Script. Initializing...");
        log.info("ROPC Script. Initialized");
        return true;
	}

	@Override
	public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("ROPC Script. Destroying...");
        log.info("ROPC Script. Destroyed.");
        return true;
	}

	@Override
	public int getApiVersion() {
		return 11;
	}

	@Override
	public boolean authenticate(Object context) {
		log.info("ROPC script. Authenticate");
		ExternalResourceOwnerPasswordCredentialsContext ropcContext = (ExternalResourceOwnerPasswordCredentialsContext) context;
		String[] deviceIdParam = ropcContext.getHttpRequest().getParameterValues("device_id");
		if(deviceIdParam != null && deviceIdParam.length > 0) {
			boolean result = deviceIdParam[0] == "device_id_1";
			if (!result) {
				return false;
			}
            // Set authenticated user in context
            // context.setUser(user)
            return true;
		}
		// generic authentication in other cases

		AuthenticationService authService = CdiUtil.bean(AuthenticationService.class);
		String username = ropcContext.getHttpRequest().getParameter(usernameParamName);
		String password = ropcContext.getHttpRequest().getParameter(passwordParamName);
		boolean result = authService.authenticate(username, password);
		if(!result) {
			log.info("ROPC script. Authenticate. Could not authenticate " + username);
			return false;
		}
		ropcContext.setUser(authService.getAuthenticatedUser());
		return true;
	}

}
```
### Sample Scripts
- [Super Gluu ROPW Script](https://github.com/GluuFederation/radius/blob/master/setup/scripts/super_gluu_ro.py)
- [3 Step ROPW Script](../../../script-catalog/resource_owner_password_credentials/resource-owner-password-credentials-2fa/)