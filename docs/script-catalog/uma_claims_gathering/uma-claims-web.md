---
tags:
  - administration
  - developer
  - script-catalog
---

# UMA Claims Gathering (Web Flow)

## Interface
The UmaClaimsGathering script implements the [UmaClaimsGatheringType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/uma/UmaClaimsGatheringType.java) interface. This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
|:-----|:------|
| `def gather(self, step, context):` | Main gather method. Must return True (if gathering performed successfully) or False (if fail). Method must set claim into context (via context.putClaim('name', value)) in order to persist it (otherwise it will be lost). All user entered values can be access via Map<String, String> context.getPageClaims()|
| `def prepareForStep(self, step, context)` | ...|
| `def getNextStep(self, step, context)` | ...|
| `def getPageForStep(self, step, context)` | ...|

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`configurationAttributes`| `configurationProperties` passed in when adding custom script. `Map<String, SimpleCustomProperty> configurationAttributes` |
|`context`| Execution Context [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |

### Script Type: Python

```python
--8<-- "script-catalog/uma_claims_gathering/uma-claims-gathering/uma_claims_gathering.py"
```

## Want to contribute?

If you'd like to contribute content to this page, check out our [Contribution guide](https://docs.jans.io/head/CONTRIBUTING/).
