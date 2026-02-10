---
tags:
  - administration
  - developer
  - script-catalog
---

# Discovery Script



This script is used to modify the response of the OpenID well-known discovery endpoint, `/.well-known/openid-configuration`. The specification for this endpoint is defined in the [OpenID Connect documentation](https://openid.net/specs/openid-connect-discovery-1_0.html#ProviderConfigurationResponse).

## Interface

### Methods

The discovery interception script extends the base script type with the `init`, `destroy` and `getApiVersion` methods:

| Inherited Methods | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

The `configurationAttributes` parameter is `java.util.Map<String, SimpleCustomProperty>`.

The discovery interception script also adds the following method(s):

|Method |`def modifyResponse(self, responseAsJsonObject, context)`|
|:-----|:------|
| Method Paramater| `responseAsJsonObject` is `org.json.JSONObject`<br/> `context` is `io.jans.as.server.model.common.ExecutionContext`|

### Objects

Definitions of all objects used in the script
### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`SimpleCustomProperty`| Map of configuration properties. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/util/src/main/java/io/jans/model/SimpleCustomProperty.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java)


## Common Use Case

### Script Type: Python

```python
--8<-- "script-catalog/discovery/discovery/scripts/Custom_OpenID_Config.py"
```

### Script Type: Java

```java
--8<-- "script-catalog/discovery/discovery/Discovery.java"
```

