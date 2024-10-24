---
tags:
  - administration
  - developer
  - script-catalog
---

# CacheRefresh Detail Custom Script (CacheRefresh)

## Overview

The Jans-Auth server implements [OAuth 2.0 Rich Authorization Requests](https://datatracker.ietf.org/doc/html/rfc9396).
This script is used to control/customize cache refresh.



## Interface
The CachRefresh script implements the [CachRefreshType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/user/CacheRefreshType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`getBindCredentials(self, configId, configurationAttributes)`| configId is the source server. configurationAttributes is `java.util.Map<String, SimpleCustomProperty>`. return None (use password from configuration) or `io.jans.model.custom.script.model.bind.BindCredentials` |
|`updateUser(self, user, configurationAttributes)`| user is `io.jans.oxtrust.model.JanssenCustomPerson`. configurationAttributes is `java.util.Map<String, SimpleCustomProperty>` |

### Script Type: Python

```java
from io.jans.model.custom.script.type.user import CacheRefreshType
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from io.jans.oxtrust.model import JanssenCustomAttribute
from io.jans.model.custom.script.model.bind import BindCredentials

import java

class CacheRefresh(CacheRefreshType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Cache refresh. Initialization"
        print "Cache refresh. Initialized successfully"

        return True   

    def destroy(self, configurationAttributes):
        print "Cache refresh. Destroy"
        print "Cache refresh. Destroyed successfully"
        return True

    # Check if this instance conform starting conditions 
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    #   return True/False
    def isStartProcess(self, configurationAttributes):
        print "Cache refresh. Is start process method"

        return False
    
    # Get bind credentials required to access source server 
    #   configId is the source server
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    #   return None (use password from configuration) or io.jans.model.custom.script.model.bind.BindCredentials
    def getBindCredentials(self, configId, configurationAttributes):
        print "Cache refresh. GetBindCredentials method"
#        if configId == "source":
#            return BindCredentials("cn=Directory Manager", "password")

        return None

    # Update user entry before persist it
    #   user is io.jans.oxtrust.model.JanssenCustomPerson
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateUser(self, user, configurationAttributes):
        print "Cache refresh. UpdateUser method"

        attributes = user.getCustomAttributes()

        # Add new attribute preferredLanguage
        attrPrefferedLanguage = JanssenCustomAttribute("preferredLanguage", "en-us")
        attributes.add(attrPrefferedLanguage)

        # Add new attribute userPassword
        attrUserPassword = JanssenCustomAttribute("userPassword", "test")
        attributes.add(attrUserPassword)

        # Update givenName attribute
        for attribute in attributes:
            attrName = attribute.getName()
            if (("givenname" == StringHelper.toLowerCase(attrName)) and StringHelper.isNotEmpty(attribute.getValue())):
                attribute.setValue(StringHelper.removeMultipleSpaces(attribute.getValue()) + " (updated)")

        return True

    def getApiVersion(self):
        return 11

```


## Sample Scripts
- [CacheRefreshScirpt](../../../script-catalog/cache_refresh/sample-script/SampleScript.py)
