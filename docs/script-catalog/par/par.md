---
tags:
  - administration
  - developer
  - script-catalog
---
# PAR Script

By overriding the interface methods in [`ParType`](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/par/ParType.java) inside a custom script you can

1. Modify all 'Par' object values before it is persisted.  e.g. modify scopes.  

2. Modify response from `/par` endpoint

## Interface

### Methods

The ParType interception script extends the base script type with the `init`, `destroy` and `getApiVersion` methods:

| Inherited Methods | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

The `configurationAttributes` parameter is `java.util.Map<String, SimpleCustomProperty>`.

The ParType interception script also adds the following method(s):

|Method |Method description|
|:-----|:------|
| `def createPar(self, par, context)`| Used to modify PAR object before it is persisted. `par` is `io.jans.as.persistence.model.Par`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`|
| `def modifyParResponse(self, response, context)`| Used to modify response from `/par` endpoint. `response` is `org.json.JSONObject`<br/> `context` is `io.jans.as.server.service.external.context.ExternalScriptContext`|


## Common Use Case

## Script Type: Java

```java
--8<-- "script-catalog/par/Par.java"
```

