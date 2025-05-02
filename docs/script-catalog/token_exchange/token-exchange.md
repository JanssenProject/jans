---
tags:
  - administration
  - developer
  - script-catalog
---

# Token Exchange Custom Script

## Overview

The Jans-Auth server supports [OAuth 2.0 Token Exchange (RFC 8693)](https://tools.ietf.org/html/rfc8693), 
which enables clients to exchange one type of token for another. 
This capability allows scenarios such as delegation, constrained token issuance, and token transformation
to meet various security and interoperability requirements.

This script is used to control the Token Exchange behavior at Token Endpoint as defined by the OAuth 2.0 Token Exchange specification.

## Behavior

The Token Exchange Endpoint accepts requests where a client submits an existing token (the _subject token_) and 
requests a new token (the _requested token_) with potentially different properties, scopes, or lifetimes. 
Upon receiving a request, the endpoint validates the provided token, applies custom business or security logic, 
and returns a response indicating whether the token exchange is approved. 
If approved, the Token Endpoint issues a new token based on the rules defined in the custom script.

During token exchange processing, the `TokenExchange` custom script is executed. Jans-Auth includes a sample demo script that illustrates a simple example of custom token exchange logic.

**Sample request**
```http
POST /jans-auth/restv1/token HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Authorization: Bearer eyJraWQiOiJr...

{
  "subject_token": "eyJraWQiOiJr...",
  "subject_token_type": "urn:ietf:params:oauth:token-type:access_token",
  "audience": "<audience>",
  "actor_token": "eyJraWQiOiJr...",
  "actor_token_type": "urn:ietf:params:oauth:token-type:access_token"
}
```

## Interface

The Token Exchange script implements the [TokenExchangeType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/token/TokenExchangeType.java) 
interface. This interface extends the base custom script type with methods specific to token exchange operations:

### Inherited Methods
| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New methods
| Method header | Method description |
|:-----|:------|
|`def validate(self, context)`| Called before built-in validation, main purpose is to perform all validations in this method. It must return `ScriptTokenExchangeControl`. |
|`def modifyResponse(self, responseAsJsonObject, context)`| Called after validation and actual token exchange. It allows to modify response or return custom error if needed. |


### Objects
| Object name | Object description |
|:-----|:------|
|`responseAsJsonObject`| Response represented as json by `org.json.JSONObject` |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |


## Sample Demo Custom Script

### Script Type: Java

```java
package io.jans.as.server._scripts;

import io.jans.as.common.model.common.User;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.ScriptTokenExchangeControl;
import io.jans.model.custom.script.type.token.TokenExchangeType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * @author Yuriy Z
 */
public class TokenExchange implements TokenExchangeType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public ScriptTokenExchangeControl validate(Object context) {

        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        String audience = scriptContext.getRequestParameter("audience");
        String subjectToken = scriptContext.getRequestParameter("subject_token");
        String subjectTokenType = scriptContext.getRequestParameter("subject_token_type");
        String deviceSecret = scriptContext.getRequestParameter("actor_token");
        String actorTokenType = scriptContext.getRequestParameter("actor_token_type");

        // perform all validations here
        boolean validationFailed = false;
        if (validationFailed) {
            return ScriptTokenExchangeControl.fail();
        }

        // user identified by request information or otherwise null
        User user = new User();

        return ScriptTokenExchangeControl
                .ok()
                .setSkipBuiltinValidation(true) // this skip build-in validations of all parameters that come
                .setUser(user);
    }

    /**
     * @param responseAsJsonObject - response represented by org.json.JSONObject
     * @param context              - script context represented by io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if changes must be applied to final response or false if whatever made in this method has to be cancelled
     */
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        JSONObject response = (JSONObject) responseAsJsonObject;
        response.accumulate("key_from_script", "value_from_script");
        return true; // return false if you wish to cancel whatever modification was made before
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TokenExchange Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TokenExchange Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed TokenExchange Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}

```