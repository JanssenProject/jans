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

## Script Type: Python

### Add custom values to the OpenID configuration endpoint

```python
from io.jans.model.custom.script.type.discovery import DiscoveryType
from java.lang import String

class Discovery(DiscoveryType):
    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        print "Custom Discovery script. Initializing ..."
        print "Custom Discovery script. Initialized successfully"

        return True

    def destroy(self, configurationAttributes):
        print "Custom Discovery script. Destroying ..."
        print "Custom Discovery script. Destroyed successfully"
        return True

    def getApiVersion(self):
        return 11

    # Returns boolean, true - apply Discovery method, false - ignore it.
    # This method is called after Discovery response is ready. This method can modify Discovery response.
    # Note :
    # responseAsJsonObject - is org.json.JSONObject, you can use any method to manipulate json
    # context is reference of io.jans.as.server.model.common.ExecutionContext (in https://github.com/JanssenProject project, )
    def modifyResponse(self, responseAsJsonObject, context):

        print "Custom - Inside modifyResponse method of Discovery script ...."
        
        # Add a value to Discovery Response
        responseAsJsonObject.accumulate("key_from_script", "value_from_script")

        # Filter out a value from Discovery Response
        responseAsJsonObject.remove("pushed_authorization_request_endpoint")

        # Get an IP Address of the Client making the request
        responseAsJsonObject.accumulate("Client IP Address", context.getHttpRequest().getHeader("X-Forwarded-For"))
        
        return True
```


## Script Type: Python

### Add custom values, remove values, and add client's IP request

```python
from io.jans.model.custom.script.type.discovery import DiscoveryType
from java.lang import String

class Discovery(DiscoveryType):
    def __init__(self, currentTimeMillis):
      self.currentTimeMillis = currentTimeMillis
  
    def init(self, customScript, configurationAttributes):
        print "Custom Discovery script. Initializing ..."
        print "Custom Discovery script. Initialized successfully"
        return True
  
   def destroy(self, configurationAttributes):
       print "Custom Discovery script. Destroying ..."
       print "Custom Discovery script. Destroyed successfully"
       return True
  
    def getApiVersion(self):
      return 11
  
   def modifyResponse(self, responseAsJsonObject, context):
  
       print "Custom - Inside modifyResponse method of Discovery script ...."
       
       # Add a value to Discovery Response
       responseAsJsonObject.accumulate("key_from_script", "value_from_script")
  
       # Filter out a value from Discovery Response
       responseAsJsonObject.remove("pushed_authorization_request_endpoint")
  
       # Get an IP Address of the Client making the request
       responseAsJsonObject.accumulate("Client IP Address", context.getHttpRequest().getHeader("X-Forwarded-For"))
       
       return True
```

## Script Type: Java 

### Add custom values, remove values, and add client's IP request

```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.discovery.DiscoveryType;
import io.jans.service.custom.script.CustomScriptManager;
import io.jans.as.server.model.common.ExecutionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.json.JSONObject;

import java.util.Map;

public class Discovery implements DiscoveryType {

    private static final Logger log = LoggerFactory.getLogger(Discovery.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);


    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Discovery Java custom script");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of Discovery Java custom script");
        scriptLogger.info("Discovery Java script. Initializing ...");
        scriptLogger.info("Discovery Java script. Initialized successfully");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Destroy of Discovery Java custom script");
        scriptLogger.info("Discovery Java script. Destroying ...");
        scriptLogger.info("Discovery Java script. Destroyed successfully");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {

        scriptLogger.info("Custom Java - Inside modifyResponse method of Discovery script ....");

        JSONObject response = (JSONObject) responseAsJsonObject;
        ExecutionContext ctx = (ExecutionContext) context;

        // Add a value to Discovery Response
        response.accumulate("key_from_java", "value_from_script_on_java");

        // Filter out a value from Discovery Response
        response.remove("pushed_authorization_request_endpoint");

        // Get an IP Address of the Client making the request
        response.accumulate("Client IP Address", ctx.getHttpRequest().getHeader("X-Forwarded-For"));
        
        return true;
    }
}
```

