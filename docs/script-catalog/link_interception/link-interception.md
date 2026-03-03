---
tags:
  - administration
  - developer
  - script-catalog
  - Jans-link
---

# Link Interception Detail Custom Script (LinkInterception)

## Overview

In order to integrate your Jans instance with backend LDAP servers handling authentication in your existing network environment, Janssen provides a mechanism called Jans Link to copy user data to the Jans local LDAP server. During this process it is possible to specify key attribute(s) and specify attribute name transformations. There are also cases when it can be used to overwrite attribute values or to add new attributes based on other attribute values.

## Interface
The Jans Link (Link Interception) script implements the [LinkInterceptionType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/user/LinkInterceptionType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
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
--8<-- "script-catalog/link_interception/sample-script/SampleScript.py"
```
