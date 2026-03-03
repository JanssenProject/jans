---
tags:
  - administration
  - developer
  - script-catalog
---

# Config Api interception Script

## Interface
The Config Api Interception script implements the [ConfigApiType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/configapi/ConfigApiType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`authorize(self, responseAsJsonObject, context)`| responseAsJsonObject - is `org.codehaus.jettison.json.JSONObject`, you can use any method to manipulate `json`. context is reference of `io.jans.as.service.external.context.ExternalIntrospectionContext` (in https://github.com/JanssenFederation/oxauth project,)|

## Sample script which demonstrates basic client authentication

### Script Type: Python

```python
--8<-- "script-catalog/config_api/config-api-interception/config_api_interception.py"
```


## Sample Scripts
[ConfigApiInterception](../../script-catalog/config_api/config-api-interception/config_api_interception.py)
