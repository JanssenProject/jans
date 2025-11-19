---
tags:
  - administration
  - developer
  - script-catalog
---
# Transaction Token Script (tx_token)

By overriding the interface methods in [`TxTokenType`](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/token/TxTokenType.java) inside a custom script you can

1. Modify JWT payload of 'tx_token' before it is persisted.

2. Modify response from `/token` endpoint when Transaction Token are used.

3. Modify lifetime of tx_token.

## Interface

### Methods

The TxTokenType interception script extends the base script type with the `init`, `destroy` and `getApiVersion` methods:

| Inherited Methods | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

The `configurationAttributes` parameter is `java.util.Map<String, SimpleCustomProperty>`.

The TxTokenType interception script also adds the following method(s):

| Method                                                                                                                            | Method description                                                                                                                                                                                             |
|:----------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `def getTxTokenLifetimeInSeconds(self, context)`                                                                                  | Used to modify Tx Token lifetime in seconds. <br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`                                                                             |
| `def modifyTokenPayload(self, jsonWebResponse, context)`                                                                          | Used to modify TxToken object before it is persisted. <br/> `jsonWebResponse` is `io.jans.as.model.token.JsonWebResponse`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext` |
| `def modifyResponse(self, response, context)`                                                                                     | Used to modify response from `/token` endpoint for transaction tokens.  <br/>  `response` is `org.json.JSONObject`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`        |


## Script Type: Java


```java


import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.token.TxTokenType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TxToken implements TxTokenType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    /**
     *
     * @param context context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return lifetime of tx_token in seconds. It must be more than 0 or otherwise it will be ignored by server.
     */
    @Override
    public int getTxTokenLifetimeInSeconds(Object context) {
        boolean condition = false; // under some condition return 1 day lifetime
        if (condition) {
            return 86400;
        }
        return 0;
    }

    /**
     *
     * @param jsonWebResponse refers to io.jans.as.model.token.JsonWebResponse
     * @param context refers to io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if tx_token should be created or false to forbid tx_token creation.
     */
    @Override
    public boolean modifyTokenPayload(Object jsonWebResponse, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;
        JsonWebResponse jwr = (JsonWebResponse) jsonWebResponse;
        jwr.getClaims().setClaim("custom_claim", "custom_value");

        return true;
    }

    /**
     * @param responseAsJsonObject - response represented by org.json.JSONObject
     * @param context              - script context represented by io.jans.as.server.service.external.context.ExternalScriptContext
     * @return true if changes must be applied to final response or false if whatever made in this method has to be cancelled
     */
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        ExternalScriptContext scriptContext = (ExternalScriptContext) context;

        JSONObject json = (JSONObject) responseAsJsonObject;
        json.accumulate("custom_key", "custom_value");
        return true;
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TxToken Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized TxToken Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed TxToken Java custom script.");
        return true;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }
}


```

