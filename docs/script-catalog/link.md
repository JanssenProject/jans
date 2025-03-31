---
tags:
  - administration
  - developer
  - scripts
  - Jans-link
---

## Overview

In order to integrate your Jans instance with backend LDAP servers handling authentication in your existing network environment, Janssen provides a mechanism called Jans Link to copy user data to the Jans local LDAP server. During this process it is possible to specify key attribute(s) and specify attribute name transformations. There are also cases when it can be used to overwrite attribute values or to add new attributes based on other attribute values.

## Interface
The Jans Link (Link Interception) script implements the [CacheRefreshType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/user/CacheRefreshType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header                                                       | Method description                                       |
|:--------------------------------------------------------------------|:---------------------------------------------------------|
| `def isStartProcess(self, configurationAttributes)`               | This method is called during start of jans link process. |
| `def getBindCredentials(self, configId, configurationAttributes)` | Get bind credentials required to access source server    |
| `def updateUser(self, user, configurationAttributes)`   | Update user entry before persist it    |

### Objects
| Object name | Object description                                                                                                                                                           |
|:-----|:-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java)   |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes`                                                   |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java)          |
|`configId`| ConfigId is the source server.                                                                                                                                               |
|`user`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-link/model/src/main/java/io/jans/link/model/GluuCustomPerson.java) |

## Use case: Dummy Jans Link (Link Interception) script

This was adapted from [Jans link (Link Interception) script example](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/link_interception/sample-script/SampleScript.py).

### Script Type: Python

```python
# oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
# Copyright (c) 2016, Janssen
#
# Author: Yuriy Movchan
#

from io.jans.model.custom.script.type.user import LinkInterceptionType
from io.jans.util import StringHelper, ArrayHelper
from java.util import Arrays, ArrayList
from io.jans.oxtrust.model import JanssenCustomAttribute
from io.jans.model.custom.script.model.bind import BindCredentials

import java

class LinkInterception(LinkInterceptionType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Link interception. Initialization"
        print "Link interception. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Link interception. Destroy"
        print "Link interception. Destroyed successfully"
        return True

    # Check if this instance conform starting conditions 
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    #   return True/False
    def isStartProcess(self, configurationAttributes):
        print "Link interception. Is start process method"

        return False

    # Get bind credentials required to access source server 
    #   configId is the source server
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    #   return None (use password from configuration) or io.jans.model.custom.script.model.bind.BindCredentials
    def getBindCredentials(self, configId, configurationAttributes):
        print "Link interception. GetBindCredentials method"
        #        if configId == "source":
        #            return BindCredentials("cn=Directory Manager", "password")

        return None

    # Update user entry before persist it
    #   user is io.jans.oxtrust.model.JanssenCustomPerson
    #   configurationAttributes is java.util.Map<String, SimpleCustomProperty>
    def updateUser(self, user, configurationAttributes):
        print "Link interception. UpdateUser method"

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