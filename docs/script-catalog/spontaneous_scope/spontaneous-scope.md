---
tags:
  - administration
  - developer
  - script-catalog
---
# Spontaneous Scope
## Interface
The Spontaneous scope script implements the [SpontaneousScopeType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/spontaneous/SpontaneousScopeType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
|:-----|:------|
| `def manipulateScopes(self, context)` | This method is called before spontaneous scope is persisted. It's possible to disable persistence via `context.setAllowSpontaneousScopePersistence(false)` Also it's possible to manipulated already granted scopes, e.g. `context.getGrantedScopes().remove("transaction:456")` |


### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`context`| Execution Context [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |

### Script Type: Python

```python
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2020, Janssen
#
# Author: Yuriy Z
#

from io.jans.model.custom.script.type.spontaneous import SpontaneousScopeType
from java.lang import String

class SpontaneousScope(SpontaneousScopeType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Spontaneous scope script. Initializing ..."
        print "Spontaneous scope script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Spontaneous scope script. Destroying ..."
        print "Spontaneous scope script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # This method is called before spontaneous scope is persisted. It's possible to disable persistence via context.setAllowSpontaneousScopePersistence(false)
    # Also it's possible to manipulated already granted scopes, e.g. context.getGrantedScopes().remove("transaction:456")
    # Note :
    # context is reference of io.jans.as.service.external.context.SpontaneousScopeExternalContext(in https://github.com/JanssenFederation/oxauth project, )
    def manipulateScopes(self, context):
        return


```
## This content is in progress

The Janssen Project documentation is currently in development. Topic pages are being created in order of broadest relevance, and this page is coming in the near future.

## Have questions in the meantime?

While this documentation is in progress, you can ask questions through [GitHub Discussions](https://github.com/JanssenProject/jans/discussions) or the [community chat on Gitter](https://gitter.im/JanssenProject/Lobby). Any questions you have will help determine what information our documentation should cover.

## Want to contribute?

If you have content you'd like to contribute to this page in the meantime, you can get started with our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).