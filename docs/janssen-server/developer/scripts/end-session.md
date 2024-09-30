---
tags:
  - administration
  - developer
  - scripts
  - EndSession
---

## Overview
End Session scripts allows the administrator to modify HTML response for OpenID Connect Frontchannel logout ([spec](https://openid.net/specs/openid-connect-frontchannel-1_0.html)).

## Interface
The end session script implements the [EndSessionType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/logout/EndSessionType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods
| Method header | Method description |
|:-----|:------|
| `def getFrontchannelHtml(self, context)` | Returns string, it must be valid HTML (with iframes according to [specification](http://openid.net/specs/openid-connect-frontchannel-1_0.html)). This method is called on `/end_session` after actual session is killed and authorization server constructs HTML to return to RP. |

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/EndSessionContext.java)


## Use case: Dummy Logout Page
This script has been adapted from the Gluu Server [sample end session script](https://github.com/GluuFederation/community-edition-setup/blob/version_4.4.0/static/extension/end_session/end_session.py). 

!!! Note

    The example script is a proof of concept, as the `getFrontchannelHtml()` must return an actual HTML string.

### Script Type: Python
```python
from io.jans.model.custom.script.type.logout import EndSessionType
from java.lang import String

class EndSession(EndSessionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "EndSession script. Initializing ..."
        print "EndSession script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "EndSession script. Destroying ..."
        print "EndSession script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Must return an HTML string
    def getFrontchannelHtml(self, context):
        return ""
```

### Script Type: Java
```java
import java.util.Map;

import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.logout.EndSessionType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.as.server.service.external.context.EndSessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class EndSession implements EndSessionType {
	
	private static final Logger log = LoggerFactory.getLogger(CustomScriptManager.class);
	
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
	public String getFrontchannelHtml(Object context) {
		EndSessionContext endSessionContext = (EndSessionContext) context;
		// Must return a real HTML string as per OIDC front channel logout spec
		return "";
	}

}

```