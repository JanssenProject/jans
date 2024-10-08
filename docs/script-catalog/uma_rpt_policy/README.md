---
tags:
  - administration
  - developer
  - script-catalog
---

## Interface
The UmaRptPolicy script implements the [UmaRptPolicyType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/uma/UmaRptPolicyType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
|:-----|:------|
| `def authorize(self, context)` | `context` is reference of `io.jans.as.service.external.context.ExternalUmaRptClaimsContext` (in `https://github.com/JanssenFederation/oxauth` project, )|
| `def getClaimsGatheringScriptName(self, context)` | `context` is reference of `io.jans.as.service.external.context.ExternalUmaRptClaimsContext` (in `https://github.com/JanssenFederation/oxauth` project, )|
| `def getRequiredClaims(self, context)` | `context` is reference of `io.jans.as.service.external.context.ExternalUmaRptClaimsContext` (in `https://github.com/JanssenFederation/oxauth` project, )|

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`context`| Execution Context [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |

### Script Type: Python

```python
from io.jans.as.model.uma import UmaConstants
from io.jans.model.uma import ClaimDefinitionBuilder
from io.jans.model.custom.script.type.uma import UmaRptPolicyType
from io.jans.service.cdi.util import CdiUtil
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList, HashSet
from java.lang import String

class UmaRptPolicy(UmaRptPolicyType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "RPT Policy. Initializing ..."
        self.clientsSet = self.prepareClientsSet(configurationAttributes)
        print "RPT Policy. Initialized successfully"
        return True

    def destroy(self, configurationAttributes):
        print "RPT Policy. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    def getRequiredClaims(self, context):
        json = """[
        ]"""
        return ClaimDefinitionBuilder.build(json)

    def authorize(self, context): # context is reference of io.jans.as.uma.authorization.UmaAuthorizationContext
        print "RPT Policy. Authorizing ..."

        client_id=context.getClient().getClientId()
        print "UmaRptPolicy. client_id = %s" % client_id

        if (StringHelper.isEmpty(client_id)):
            return False
     
        if (self.clientsSet.contains(client_id)):
            print "UmaRptPolicy. Authorizing client"
            return True
        else:
            print "UmaRptPolicy. Client isn't authorized"
            return False

    def getClaimsGatheringScriptName(self, context):
        return UmaConstants.NO_SCRIPT

    def prepareClientsSet(self, configurationAttributes):
        clientsSet = HashSet()
        if (not configurationAttributes.containsKey("allowed_clients")):
            return clientsSet

        allowedClientsList = configurationAttributes.get("allowed_clients").getValue2()
        if (StringHelper.isEmpty(allowedClientsList)):
            print "UmaRptPolicy. The property allowed_clients is empty"
            return clientsSet    

        allowedClientsListArray = StringHelper.split(allowedClientsList, ",")
        if (ArrayHelper.isEmpty(allowedClientsListArray)):
            print "UmaRptPolicy. No clients specified in allowed_clients property"
            return clientsSet
        
        # Convert to HashSet to quick search
        i = 0
        count = len(allowedClientsListArray)
        while (i < count):
            client = allowedClientsListArray[i]
            clientsSet.add(client)
            i = i + 1

        return clientsSet

```











