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

| Method                                                                                                                            | Method description                                                                                                                                                                                                                                                                     |
|:----------------------------------------------------------------------------------------------------------------------------------|:---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `def getTxTokenLifetimeInSeconds(self, context)`                                                                                  | Used to modify Tx Token lifetime in seconds. <br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`                                                                                                                                                     |
| `def modifyTokenPayload(self, jsonWebResponse, context)`                                                                          | Used to modify TxToken object before it is persisted. Returning false from modifyTokenPayload will prevent tx_token creation. <br/> `jsonWebResponse` is `io.jans.as.model.token.JsonWebResponse`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext` |
| `def modifyResponse(self, response, context)`                                                                                     | Used to modify response from `/token` endpoint for transaction tokens.  <br/>  `response` is `org.json.JSONObject`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`                                                                                |

## Script Type: Java

```java
--8<-- "script-catalog/tx_token/TxToken.java"
```

