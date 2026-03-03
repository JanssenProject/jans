---
tags:
  - administration
  - developer
  - script-catalog
---

# Access Evaluation Discovery Custom Script

This script is used to control the Access Evaluation Discovery Endpoint (`/.well-known/authzen-configuration`).

The AuthZEN discovery endpoint publishes PDP (Policy Decision Point) metadata per the AuthZEN specification,
including all available endpoints and capabilities.

**Sample Request**

```http
GET /.well-known/authzen-configuration HTTP/1.1
Host: janssen.server.host
```

**Sample Response**

```json
{
  "policy_decision_point": "https://janssen.server.host",
  "access_evaluation_endpoint": "https://janssen.server.host/jans-auth/restv1/access/v1/evaluation",
  "access_evaluations_endpoint": "https://janssen.server.host/jans-auth/restv1/access/v1/evaluations",
  "subject_search_endpoint": "https://janssen.server.host/jans-auth/restv1/access/v1/search/subject",
  "resource_search_endpoint": "https://janssen.server.host/jans-auth/restv1/access/v1/search/resource",
  "action_search_endpoint": "https://janssen.server.host/jans-auth/restv1/access/v1/search/action",
  "capabilities": [],
  "access_evaluation_v1_endpoint": "https://janssen.server.host/jans-auth/restv1/access/v1"
}
```

## Discovery Response Fields

| Field | Description |
|:------|:------------|
| `policy_decision_point` | The issuer URL identifying the PDP |
| `access_evaluation_endpoint` | URL for single evaluation requests |
| `access_evaluations_endpoint` | URL for batch evaluation requests |
| `subject_search_endpoint` | URL for subject search requests |
| `resource_search_endpoint` | URL for resource search requests |
| `action_search_endpoint` | URL for action search requests |
| `capabilities` | Array of supported capabilities (extensible via script) |
| `access_evaluation_v1_endpoint` | Legacy base endpoint URL for backward compatibility |


## Interface

The Access Evaluation Discovery script implements the [AccessEvaluationDiscoveryType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authzen/AccessEvaluationDiscoveryType.java) interface.
This extends methods from the base script type in addition to adding new methods:

### Inherited Methods

| Method header | Method description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | This method is only called once during the script initialization. It can be used for global script initialization, initiate objects etc |
| `def destroy(self, configurationAttributes)` | This method is called once to destroy events. It can be used to free resource and objects created in the `init()` method |
| `def getApiVersion(self, configurationAttributes, customScript)` | The getApiVersion method allows API changes in order to do transparent migration from an old script to a new API. Only include the customScript variable if the value for getApiVersion is greater than 10 |

### New Methods

| Method header | Method description |
|:-----|:------|
| `def modifyResponse(self, responseAsJsonObject, context)` | This method is called after discovery response is ready. This method can modify discovery response.<br/>`responseAsJsonObject` is `org.json.JSONObject`<br/> `context` is `io.jans.as.server.model.common.ExecutionContext` |


`modifyResponse` method returns `true` to accept modifications or `false` to revert all changes.


### Objects

| Object name | Object description |
|:-----|:------|
| `customScript` | The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
| `context` | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/model/common/ExecutionContext.java) |


## Use Cases

The discovery script can be used to:

1. **Add custom capabilities** - Advertise PDP-specific features
2. **Add custom endpoints** - Include additional proprietary endpoints
3. **Add metadata** - Include organization-specific information
4. **Conditional responses** - Modify response based on request context


## Sample Demo Custom Script

### Script Type: Java

```java
import io.jans.model.SimpleCustomProperty;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzen.AccessEvaluationDiscoveryType;
import io.jans.service.custom.script.CustomScriptManager;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AccessEvaluationDiscovery implements AccessEvaluationDiscoveryType {

    private static final Logger log = LoggerFactory.getLogger(AccessEvaluationDiscovery.class);
    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Init of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        log.info("Destroy of AccessEvaluationDiscovery Java custom script");
        return true;
    }

    @Override
    public int getApiVersion() {
        log.info("getApiVersion AccessEvaluationDiscovery Java custom script: 11");
        return 11;
    }

    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        scriptLogger.info("Modifying AuthZEN discovery response");
        JSONObject response = (JSONObject) responseAsJsonObject;

        // Add custom capabilities
        JSONArray capabilities = response.optJSONArray("capabilities");
        if (capabilities == null) {
            capabilities = new JSONArray();
            response.put("capabilities", capabilities);
        }

        // Add custom metadata
        response.put("organization", "Acme Corp");
        response.put("pdp_version", "1.0.0");

        // Add custom endpoint
        response.put("custom_endpoint", "https://example.com/custom");

        return true; // Accept modifications
    }
}
```


## Example: Adding Capabilities

```java
@Override
public boolean modifyResponse(Object responseAsJsonObject, Object context) {
    JSONObject response = (JSONObject) responseAsJsonObject;

    // Add supported capabilities per AuthZEN spec
    JSONArray capabilities = new JSONArray();

    response.put("capabilities", capabilities);

    return true;
}
```


## Sample Scripts

### Access Evaluation Discovery Script

```java
--8<-- "script-catalog/access_evaluation/AccessEvaluationDiscovery.java"
```
