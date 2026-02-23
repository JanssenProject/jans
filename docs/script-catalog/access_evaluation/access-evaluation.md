---
tags:
  - administration
  - developer
  - script-catalog
---

# Access Evaluation Custom Script


The Jans-Auth server implements [OpenID AuthZEN Authorization API 1.0](https://openid.github.io/authzen/).
The AuthZEN Authorization API 1.0 specification defines a standardized interface for communication between
Policy Enforcement Points (PEPs) and Policy Decision Points (PDPs) to facilitate consistent authorization decisions across diverse systems.
It introduces an Access Evaluation API that allows PEPs to query PDPs about specific access requests,
enhancing interoperability and scalability in authorization processes.
The specification is transport-agnostic, with an initial focus on HTTPS bindings, and emphasizes secure, fine-grained,
and dynamic authorization mechanisms.

This script is used to control Access Evaluation Endpoints described in specification.

## Behavior

The Access Evaluation Endpoint in the AuthZEN specification serves as a mechanism for Policy Enforcement Points (PEPs)
to request access decisions from a Policy Decision Point (PDP) for specific resources and actions.
Upon receiving a request, the endpoint evaluates the subject, resource, and action against defined policies to determine
if access should be granted, denied, or if additional information is needed.
The endpoint's responses are typically concise, aiming to provide a rapid decision that PEPs can enforce in real-time.
The goal is to provide a scalable, secure interface for dynamic and fine-grained access control across applications.

During Access Evaluation request processing the `Access Evaluation` custom script is executed.
Name of the script must be specified by `accessEvaluationScriptName` configuration property.
If AS can't find such script or if configuration property is not specified then server executes first script it finds on database.
AS comes with sample demo script which shows simple example of custom validation and granting access.

**Sample Single Evaluation Request**
```http
POST /jans-auth/restv1/access/v1/evaluation HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Authorization: Basic <encoded_credentials>

{
  "subject": {
    "type": "super_admin",
    "id": "alice@acmecorp.com"
  },
  "resource": {
    "type": "account",
    "id": "123"
  },
  "action": {
    "name": "can_read"
  },
  "context": {
    "ip_address": "192.168.1.1",
    "time": "2024-01-15T10:30:00Z"
  }
}
```

**Sample Batch Evaluations Request**
```http
POST /jans-auth/restv1/access/v1/evaluations HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Authorization: Basic <encoded_credentials>

{
  "subject": {
    "type": "user",
    "id": "alice@acmecorp.com"
  },
  "evaluations": [
    {"action": {"name": "read"}},
    {"action": {"name": "write"}},
    {"action": {"name": "delete"}}
  ],
  "options": {
    "evaluations_semantic": "deny_on_first_deny"
  }
}
```


## Interface

The Access Evaluation script implements the [AccessEvaluationType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authzen/AccessEvaluationType.java) interface.
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
| `evaluate(request, context)` | Called for single evaluation requests. Must return `AccessEvaluationResponse`. |
| `searchSubject(request, context)` | Called for subject search requests. Returns `SearchResponse<Subject>` or `null` for empty response. |
| `searchResource(request, context)` | Called for resource search requests. Returns `SearchResponse<Resource>` or `null` for empty response. |
| `searchAction(request, context)` | Called for action search requests. Returns `SearchResponse<Action>` or `null` for empty response. |

### Method Details

#### evaluate(request, context)

The main evaluation method called when a single evaluation or batch evaluation request is received.

**Parameters:**
- `request` - `AccessEvaluationRequest` containing subject, resource, action, and context
- `context` - `ExternalScriptContext` providing access to HTTP request and other contextual data

**Returns:** `AccessEvaluationResponse` indicating the access decision (true/false) with optional context

#### searchSubject(request, context)

Called when a subject search request is received at `/access/v1/search/subject`.

**Parameters:**
- `request` - `SearchSubjectRequest` containing search criteria
- `context` - Script context

**Returns:** `SearchResponse<Subject>` with matching subjects, or `null` for default empty response

#### searchResource(request, context)

Called when a resource search request is received at `/access/v1/search/resource`.

**Parameters:**
- `request` - `SearchResourceRequest` containing search criteria
- `context` - Script context

**Returns:** `SearchResponse<Resource>` with matching resources, or `null` for default empty response

#### searchAction(request, context)

Called when an action search request is received at `/access/v1/search/action`.

**Parameters:**
- `request` - `SearchActionRequest` containing search criteria
- `context` - Script context

**Returns:** `SearchResponse<Action>` with matching actions, or `null` for default empty response


### Objects

| Object name | Object description |
|:-----|:------|
| `customScript` | The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
| `context` | [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |
| `AccessEvaluationRequest` | Request containing subject, resource, action, context, evaluations (for batch), and options |
| `AccessEvaluationResponse` | Response with decision (boolean) and optional context |
| `SearchResponse` | Paginated response for search operations |


## Sample Demo Custom Script

### Script Type: Java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.authzen.*;
import io.jans.model.custom.script.model.CustomScript;
import io.jans.model.custom.script.type.authzen.AccessEvaluationType;
import io.jans.service.custom.script.CustomScriptManager;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

/**
 * Sample AccessEvaluation script demonstrating:
 * - Single evaluation with custom validation
 * - Search method stubs (return null for default empty response)
 *
 * @author Yuriy Z
 */
public class AccessEvaluation implements AccessEvaluationType {

    private static final Logger scriptLogger = LoggerFactory.getLogger(CustomScriptManager.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AccessEvaluationResponse evaluate(AccessEvaluationRequest request, Object scriptContext) {

        ExternalScriptContext context = (ExternalScriptContext) scriptContext;
        // 1. access http request via context.getHttpRequest()
        // 2. access all access evaluation specific data directly with 'request', e.g. request.getSubject()

        // 3. perform custom validation if needed
        validateResource(request.getResource());

        // typically some internal validation must be performed here
        // request data alone must not be trusted, it's just sample to demo script with endpoint
        if ("super_admin".equalsIgnoreCase(request.getSubject().getType())) {
            final ObjectNode reasonAdmin = objectMapper.createObjectNode();
            reasonAdmin.put("reason", "super_admin");

            final AccessEvaluationResponseContext responseContext = new AccessEvaluationResponseContext();
            responseContext.setId(UUID.randomUUID().toString());
            responseContext.setReasonAdmin(reasonAdmin);

            return new AccessEvaluationResponse(true, responseContext);
        }
        return AccessEvaluationResponse.FALSE;
    }

    private void validateResource(Resource resource) {
        // sample for custom validation
        if (resource.getType().equalsIgnoreCase("file")) {
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .entity("{\n" +
                            "  \"error\": \"invalid_resource_type\",\n" +
                            "  \"error_description\": \"Resource type 'file' is not allowed.\"\n" +
                            "}")
                    .build());
        }
    }

    @Override
    public boolean init(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AccessEvaluation Java custom script.");
        return true;
    }

    @Override
    public boolean init(CustomScript customScript, Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Initialized AccessEvaluation Java custom script.");
        return true;
    }

    @Override
    public boolean destroy(Map<String, SimpleCustomProperty> configurationAttributes) {
        scriptLogger.info("Destroyed AccessEvaluation Java custom script.");
        return false;
    }

    @Override
    public int getApiVersion() {
        return 11;
    }

    @Override
    public SearchResponse<Subject> searchSubject(SearchSubjectRequest request, Object context) {
        // Implement subject search logic here
        // Return null to use default empty response
        return null;
    }

    @Override
    public SearchResponse<Resource> searchResource(SearchResourceRequest request, Object context) {
        // Implement resource search logic here
        // Return null to use default empty response
        return null;
    }

    @Override
    public SearchResponse<Action> searchAction(SearchActionRequest request, Object context) {
        // Implement action search logic here
        // Return null to use default empty response
        return null;
    }
}
```


## Search Implementation Example

Here's an example of implementing a search method that returns actual results:

```java
@Override
public SearchResponse<Subject> searchSubject(SearchSubjectRequest request, Object context) {
    // Example: Return users who have access to the requested resource/action
    List<Subject> subjects = new ArrayList<>();

    // Query your authorization backend/database here
    // This is just a sample - implement your actual logic
    subjects.add(new Subject().setType("user").setId("alice@acmecorp.com"));
    subjects.add(new Subject().setType("user").setId("bob@acmecorp.com"));

    SearchResponse<Subject> response = new SearchResponse<>();
    response.setResults(subjects);

    // Set pagination info
    PageResponse page = new PageResponse();
    page.setCount(subjects.size());
    page.setTotal(subjects.size());
    response.setPage(page);

    return response;
}
```


## Context Object

The `context` field in requests is fully dynamic and accepts any key-value pairs per AuthZEN specification:

```json
{
  "context": {
    "ip_address": "192.168.1.100",
    "time": "2024-01-15T10:30:00Z",
    "device_type": "mobile",
    "region": "us-east-1",
    "custom_field": "any_value"
  }
}
```

Access context values in your script:
```java
Context ctx = request.getContext();
String ipAddress = (String) ctx.get("ip_address");
String region = (String) ctx.get("region");
```


## Batch Evaluation Options

When handling batch evaluations, the `options.evaluations_semantic` field controls processing:

| Value | Behavior |
|:------|:---------|
| `execute_all` | Process all evaluations regardless of results (default) |
| `deny_on_first_deny` | Stop on first deny result |
| `permit_on_first_permit` | Stop on first permit result |

The AS handles semantic logic automatically - your `evaluate()` method is called for each individual evaluation.


## Sample Scripts

### Access Evaluation Script

```java
--8<-- "script-catalog/access_evaluation/AccessEvaluation.java"
```
