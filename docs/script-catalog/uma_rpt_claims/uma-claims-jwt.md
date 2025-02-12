---
tags:
  - administration
  - developer
  - script-catalog
---

## Interface
The UmaRptClaims script implements the [UmaRptClaimsType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/uma/UmaRptClaimsType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
|:-----|:------|
| `def modify(self, rptAsJsonObject, context)` | responseAsJsonObject - is `org.codehaus.jettison.json.JSONObject`, you can use any method to manipulate json. `context` is reference of `io.jans.as.service.external.context.ExternalUmaRptClaimsContext` (in `https://github.com/JanssenFederation/oxauth` project, )|

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`context`| Execution Context [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |

### Script Type: Python

```python
from io.jans.model.custom.script.type.uma import UmaRptClaimsType
from java.lang import String

class UmaRptClaims(UmaRptClaimsType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "RPT Claims script. Initializing ..."
        print "RPT Claims script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "RPT Claims script. Destroying ..."
        print "RPT Claims script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply changes from script method, false - ignore it.
    # Note :
    # responseAsJsonObject - is org.codehaus.jettison.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.service.external.context.ExternalUmaRptClaimsContext (in https://github.com/JanssenFederation/oxauth project, )
    def modify(self, rptAsJsonObject, context):
        rptAsJsonObject.accumulate("key_from_script", "value_from_script")
        return True



```


## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).