---
tags:
- administration
- auth-server
- access-evaluation
- endpoint
---
# Access Evaluation Endpoint


The Jans-Auth server implements [OpenID AuthZEN Authorization API 1.0 (doc published on 11 January 2026)](https://openid.net/specs/authorization-api-1_0.html).
The AuthZEN Authorization API 1.0 specification defines a standardized interface for communication between
Policy Enforcement Points (PEPs) and Policy Decision Points (PDPs) to facilitate consistent authorization decisions across diverse systems.
It introduces an Access Evaluation API that allows PEPs to query PDPs about specific access requests,
enhancing interoperability and scalability in authorization processes.
The specification is transport-agnostic, with an initial focus on HTTPS bindings, and emphasizes secure, fine-grained,
and dynamic authorization mechanisms.

The Access Evaluation Endpoint in the AuthZEN specification serves as a mechanism for Policy Enforcement Points (PEPs)
to request access decisions from a Policy Decision Point (PDP) for specific resources and actions.
Upon receiving a request, the endpoint evaluates the subject, resource, and action against defined policies to determine
if access should be granted, denied, or if additional information is needed.
The endpoint's responses are typically concise, aiming to provide a rapid decision that PEPs can enforce in real-time.
The goal is to provide a scalable, secure interface for dynamic and fine-grained access control across applications.


## Endpoints Overview

Janssen Server implements the following AuthZEN endpoints:

| Endpoint | Path | Description |
|:---------|:-----|:------------|
| Single Evaluation | `/access/v1/evaluation` | Evaluate a single access request |
| Batch Evaluations | `/access/v1/evaluations` | Evaluate multiple access requests in one call |
| Search Subject | `/access/v1/search/subject` | Search for subjects that have access |
| Search Resource | `/access/v1/search/resource` | Search for resources a subject can access |
| Search Action | `/access/v1/search/action` | Search for actions a subject can perform |
| Discovery | `/.well-known/authzen-configuration` | PDP metadata discovery |


## Discovery

URL to access the access evaluation endpoint on Janssen Server is listed in both:
 - the response of Janssen Server's well-known [configuration endpoint](./configuration.md) given below.
 - the response of Janssen Server's `/.well-known/authzen-configuration` endpoint.

**OpenID Discovery**
```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

**AuthZEN Discovery**
```text
https://janssen.server.host/jans-auth/.well-known/authzen-configuration
```

`/.well-known/authzen-configuration` allows publishing data specific to AuthZEN only. Response of AuthZEN discovery endpoint can be
changed via `AccessEvaluationDiscoveryType` custom script.

**Sample AuthZEN Discovery Response**

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

**Snippet of AccessEvaluationDiscoveryType**
```java
    @Override
    public boolean modifyResponse(Object responseAsJsonObject, Object context) {
        scriptLogger.info("write to script logger");
        JSONObject response = (JSONObject) responseAsJsonObject;
        response.accumulate("key_from_java", "value_from_script_on_java");
        return true;
    }
```


## Authorization

To call Access Evaluation endpoints, the client must have `access_evaluation` scope.
If scope is not present, AS rejects the call with a 401 (unauthorized) HTTP status code.
Alternatively, it's possible to use `Basic` token with encoded client credentials if the
`accessEvaluationAllowBasicClientAuthorization` AS configuration property is set to `true`.

- Bearer token : `Authorization: Bearer <access_token>`
- Basic authorization : `Authorization: Basic <encoded client credentials>`


## Single Evaluation Endpoint

The single evaluation endpoint (`/access/v1/evaluation`) evaluates one access request.

**Endpoint**
```text
POST https://janssen.server.host/jans-auth/restv1/access/v1/evaluation
```

**Sample Request**
```http
POST /jans-auth/restv1/access/v1/evaluation HTTP/1.1
Host: janssen.server.host
Content-Type: application/json
Authorization: Basic M2NjOTdhYWItMDE0Zi00ZWM5LWI4M2EtNTE3MTRlODE3MDMwOmFlYmMwZWFhLWY5N2YtNDU5NS04ZWExLWFlNmU1NDFmNDZjNg==

{
  "subject": {
    "type": "user",
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
    "ip_address": "192.168.1.100",
    "time": "2024-01-15T10:30:00Z"
  }
}
```

**Sample Successful Response**
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "decision": true,
  "context": {
    "id": "9e04dd22-e980-4e54-bc04-d64a0c2e1afe",
    "reason_admin": {"reason": "super_admin"},
    "reason_user": null
  }
}
```

### Request Fields

| Field | Type | Required | Description |
|:------|:-----|:---------|:------------|
| `subject` | Object | Yes | The entity requesting access |
| `subject.type` | String | Yes | Type of the subject (e.g., "user", "service") |
| `subject.id` | String | Yes | Unique identifier of the subject |
| `resource` | Object | Yes | The resource being accessed |
| `resource.type` | String | Yes | Type of the resource (e.g., "document", "api") |
| `resource.id` | String | Yes | Unique identifier of the resource |
| `action` | Object | Yes | The action being performed |
| `action.name` | String | Yes | Name of the action (e.g., "read", "write", "delete") |
| `context` | Object | No | Additional context as key-value pairs |

### Context Object

The `context` object is fully dynamic and accepts any key-value pairs. This allows passing arbitrary contextual information such as:

- IP address
- Timestamp
- Device information
- Geographic location
- Custom application-specific data

```json
{
  "context": {
    "ip_address": "192.168.1.100",
    "time": "2024-01-15T10:30:00Z",
    "device_type": "mobile",
    "region": "us-east-1",
    "custom_field": "custom_value"
  }
}
```


## Batch Evaluations Endpoint

The batch evaluations endpoint (`/access/v1/evaluations`) allows evaluating multiple access requests in a single call.
This is useful for performance optimization when checking multiple permissions at once.

**Endpoint**
```text
POST https://janssen.server.host/jans-auth/restv1/access/v1/evaluations
```

**Sample Request**
```http
POST /jans-auth/restv1/access/v1/evaluations HTTP/1.1
Host: janssen.server.host
Content-Type: application/json
Authorization: Bearer <access_token>

{
  "subject": {
    "type": "user",
    "id": "alice@acmecorp.com"
  },
  "resource": {
    "type": "document",
    "id": "doc-456"
  },
  "evaluations": [
    {
      "action": {"name": "read"}
    },
    {
      "action": {"name": "write"}
    },
    {
      "action": {"name": "delete"},
      "resource": {"type": "document", "id": "doc-789"}
    }
  ],
  "options": {
    "evaluations_semantic": "execute_all"
  }
}
```

**Sample Response**
```json
HTTP/1.1 200 OK
Content-Type: application/json

{
  "evaluations": [
    {"decision": true, "context": {"id": "eval-1"}},
    {"decision": true, "context": {"id": "eval-2"}},
    {"decision": false, "context": {"id": "eval-3"}}
  ]
}
```

### Batch Request Fields

The batch request supports default values for `subject`, `resource`, `action`, and `context` at the top level.
Individual evaluations can override these defaults.

| Field | Type | Required | Description |
|:------|:-----|:---------|:------------|
| `subject` | Object | No | Default subject for all evaluations |
| `resource` | Object | No | Default resource for all evaluations |
| `action` | Object | No | Default action for all evaluations |
| `context` | Object | No | Default context for all evaluations |
| `evaluations` | Array | Yes | Array of individual evaluation requests |
| `options` | Object | No | Evaluation options |

### Evaluation Options

The `options` object controls how batch evaluations are processed:

| Option | Values | Description |
|:-------|:-------|:------------|
| `evaluations_semantic` | `execute_all` | Execute all evaluations regardless of results (default) |
| | `deny_on_first_deny` | Stop processing on first deny result |
| | `permit_on_first_permit` | Stop processing on first permit result |

**Example with short-circuit evaluation:**
```json
{
  "evaluations": [...],
  "options": {
    "evaluations_semantic": "deny_on_first_deny"
  }
}
```


## Search Endpoints

Search endpoints allow discovering which subjects, resources, or actions satisfy given access criteria.
These endpoints are useful for building UI components that show available permissions.

### Search Subject

Find subjects that have access to a specific resource and action.

**Endpoint**
```text
POST https://janssen.server.host/jans-auth/restv1/access/v1/search/subject
```

**Sample Request**
```json
{
  "subject": {
    "type": "user"
  },
  "resource": {
    "type": "document",
    "id": "doc-123"
  },
  "action": {
    "name": "read"
  },
  "page": {
    "limit": 10
  }
}
```

### Search Resource

Find resources that a subject can access with a specific action.

**Endpoint**
```text
POST https://janssen.server.host/jans-auth/restv1/access/v1/search/resource
```

**Sample Request**
```json
{
  "subject": {
    "type": "user",
    "id": "alice@acmecorp.com"
  },
  "resource": {
    "type": "document"
  },
  "action": {
    "name": "read"
  },
  "page": {
    "limit": 10
  }
}
```

### Search Action

Find actions that a subject can perform on a specific resource.

**Endpoint**
```text
POST https://janssen.server.host/jans-auth/restv1/access/v1/search/action
```

**Sample Request**
```json
{
  "subject": {
    "type": "user",
    "id": "alice@acmecorp.com"
  },
  "resource": {
    "type": "document",
    "id": "doc-123"
  },
  "page": {
    "limit": 10
  }
}
```

### Search Response

All search endpoints return paginated results:

```json
{
  "results": [
    {"type": "user", "id": "alice"},
    {"type": "user", "id": "bob"}
  ],
  "page": {
    "next_token": "abc123",
    "count": 2,
    "total": 50
  }
}
```


## Error Responses

**401 Unauthorized**
```json
{
  "error": "invalid_token"
}
```

**400 Bad Request**
```json
{
  "error": "invalid_request",
  "error_description": "Subject is required"
}
```


## Configuration Properties

Access Evaluation Endpoint AS configuration:

- **accessEvaluationScriptName** - Access evaluation custom script name. If not set AS falls back to first valid script found in database.
- **accessEvaluationAllowBasicClientAuthorization** - Allow basic client authorization for access evaluation endpoint.


## Custom Script

AS provides `AccessEvaluationType` custom script which must be used to control Access Evaluation Endpoint behavior.

Use `accessEvaluationScriptName` configuration property to specify custom script. If not set AS falls back to first valid script found in database.

The script interface provides the following methods:

| Method | Description |
|:-------|:------------|
| `evaluate(request, context)` | Main evaluation logic for single evaluation |
| `searchSubject(request, context)` | Handle subject search requests |
| `searchResource(request, context)` | Handle resource search requests |
| `searchAction(request, context)` | Handle action search requests |

### Sample Evaluation Script

```java
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
```

More details in [Access Evaluation Custom Script Page](../../../script-catalog/access_evaluation/access-evaluation.md).

Full sample script can be found [here](../../../script-catalog/access_evaluation/AccessEvaluation.java)


## Full Successful Access Evaluation Flow Sample

```
#######################################################
TEST: OpenID Connect Discovery
#######################################################
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
GET /.well-known/webfinger HTTP/1.1?resource=acct%3Aadmin%40happy-example.gluu.info&rel=http%3A%2F%2Fopenid.net%2Fspecs%2Fconnect%2F1.0%2Fissuer HTTP/1.1
Host: happy-example.gluu.info

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Content-Type: application/jrd+json;charset=iso-8859-1

{
    "subject": "acct:admin@happy-example.gluu.info",
    "links": [{
        "rel": "http://openid.net/specs/connect/1.0/issuer",
        "href": "https://happy-example.gluu.info"
    }]
}


#######################################################
TEST: accessEvaluation_whenSubjectTypeIsAcceptedByScript_shouldGrantAccess
#######################################################
-------------------------------------------------------
REQUEST:
-------------------------------------------------------
POST /jans-auth/restv1/access/v1/evaluation HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Authorization: Basic M2NjOTdhYWItMDE0Zi00ZWM5LWI4M2EtNTE3MTRlODE3MDMwOmFlYmMwZWFhLWY5N2YtNDU5NS04ZWExLWFlNmU1NDFmNDZjNg==

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
    "ip_address": "192.168.1.1"
  }
}

-------------------------------------------------------
RESPONSE:
-------------------------------------------------------
HTTP/1.1 200
Content-Type: application/json

{
  "decision": true,
  "context": {
    "id": "9e04dd22-e980-4e54-bc04-d64a0c2e1afe",
    "reason_admin": {"reason": "super_admin"},
    "reason_user": null
  }
}
```
