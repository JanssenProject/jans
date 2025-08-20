---
tags:
  - administration
  - developer
  - script-catalog
---

# Application Session Script Guide

The **Jans-Auth** server allows you to modify the session flow through this script.

## Interface

The Application Session script implement
the [ApplicationSessionType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/session/ApplicationSessionType.java)
interface. This extends methods form the base script type in addition to adding new methods:

### Inherited Methods

| Method header                                                    | Method description                                                                                                                                                                                         |
|:-----------------------------------------------------------------|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `def init(self, customScript, configurationAttributes)`          | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc                                                                    |
| `def destroy(self, configurationAttributes)`                     | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method                                                                                   |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods

| Method header                                                             | Method description                                                           |
|:--------------------------------------------------------------------------|------------------------------------------------------------------------------|
| `def startSession(self, httpRequest, sessionId, configurationAttributes)` | Called when a session is started.                                            |
| `def endSession(self, httpRequest, sessionId, configurationAttributes)`   | Called when a session is ended.                                              |
| `def onEvent(self, event)`                                                | Called when a specific session event occurs..                                |
| `def modifyActiveSessionsResponse(self, jsonArray, context)`              | Called it during /session/active endpoint call to modify response if needed. |

All methods return `true`/`false`, the server issues an error if this response is `false`.

If parameters is not present then error has to be created and `false` returned. If all is good script has to
return `true`.
### Objects

| Object name               | Object description                                                                                                                                                                                                               |
|:--------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `customScript`            | [io.jans.model.custom.script.model.CustomScript](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java)                                            |
| `context`                 | [io.jans.as.server.service.external.context.ExternalScriptContext](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |
| `sessionId`               | [io.jans.as.common.model.session.SessionId](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/common/src/main/java/io/jans/as/common/model/session/SessionId.java)                                               |
| `event`                   | [io.jans.as.server.service.external.session.SessionEvent](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/session/SessionEvent.java)                   |
| `httpRequest`             | jakarta.servlet.http.HttpServletRequest                                                                                                                                                                                          |
| `jsonArray`               | org.json.JSONArray                                                                                                                                                                                                               |
| `configurationAttributes` | java.util.Map<String, [SimpleCustomProperty](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java)>                                                             |

## Common Use Cases

- `startSession`: Validate first session
- `endSession`: Print logs
- `onEvent`: If event type is AUTHENTICATED, print sessionId in logs
- `modifyActiveSessionsResponse`: Print logs

### Script type: Python

```python
from io.jans.model.custom.script.type.session import ApplicationSessionType
from io.jans.service.cdi.util import CdiUtil
from io.jans.orm import PersistenceEntryManager
from io.jans.as.model.config import StaticConfiguration
from io.jans.as.server.model.ldap import TokenEntity
from jakarta.faces.application import FacesMessage
from io.jans.jsf2.message import FacesMessages
from io.jans.util import StringHelper, ArrayHelper
from io.jans.as.server.model.config import Constants
from java.util import Arrays, ArrayList
from io.jans.as.server.service.external.session import SessionEventType

import java

class ApplicationSession(ApplicationSessionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Application session. Initialization"

        self.entryManager = CdiUtil.bean(PersistenceEntryManager)
        self.staticConfiguration = CdiUtil.bean(StaticConfiguration)

        print "Application session. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Application session. Destroy"
        print "Application session. Destroyed successfully"
        return True   

    def getApiVersion(self):
        return 11

    def onEvent(self, event):
        if event.getType() == SessionEventType.AUTHENTICATED:
            print "Session is authenticated, session: " + event.getSessionId().getId()
        return

    def startSession(self, httpRequest, sessionId, configurationAttributes):
        print "Application session. Starting external session"

        user_name = sessionId.getSessionAttributes().get(Constants.AUTHENTICATED_USER)

        first_session = self.isFirstSession(user_name)
        if not first_session:
            facesMessages = CdiUtil.bean(FacesMessages)
            facesMessages.add(FacesMessage.SEVERITY_ERROR, "Please, end active session first!")
            return False

        print "Application session. External session started successfully"
        return True

    def endSession(self, httpRequest, sessionId, configurationAttributes):
        print "Application session. Starting external session end"

        print "Application session. External session ended successfully"
        return True

    def modifyActiveSessionsResponse(self, jsonArray, context):
        print "Application session. Starting external modify active session"

        print "Application session. External modify active session successfully"
        return False

    def isFirstSession(self, user_name):
        tokenLdap = TokenEntity()
        tokenLdap.setDn(self.staticConfiguration.getBaseDn().getClients())
        tokenLdap.setUserId(user_name)

        tokenLdapList = self.entryManager.findEntries(tokenLdap, 1)
        print "Application session. isFirstSession. Get result: '%s'" % tokenLdapList

        if (tokenLdapList != None) and (tokenLdapList.size() > 0):
            print "Application session. isFirstSession: False"
            return False

        print "Application session. isFirstSession: True"
        return True
```


## Sample Script 

[jans-session-audit.py](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/application_session/jans-session-audit/jans-session-audit.py) - This script performs advanced session auditing for monitoring and tracking user sessions.
