---
tags:
  - administration
  - developer
  - script-catalog
---

# Access Evaluation Custom Script

## Overview

The Jans-Auth server implements [OpenID AuthZEN Authorization API 1.0 – draft 01](https://openid.github.io/authzen/).
The AuthZEN Authorization API 1.0 specification defines a standardized interface for communication between 
Policy Enforcement Points (PEPs) and Policy Decision Points (PDPs) to facilitate consistent authorization decisions across diverse systems. 
It introduces an Access Evaluation API that allows PEPs to query PDPs about specific access requests, 
enhancing interoperability and scalability in authorization processes. 
The specification is transport-agnostic, with an initial focus on HTTPS bindings, and emphasizes secure, fine-grained, 
and dynamic authorization mechanisms.

This script is used to control Access Evaluation Endpoint described in specification.

## Behavior

The Access Evaluation Endpoint in the AuthZEN specification serves as a mechanism for Policy Enforcement Points (PEPs) 
to request access decisions from a Policy Decision Point (PDP) for specific resources and actions. 
Upon receiving a request, the endpoint evaluates the subject, resource, and action against defined policies to determine 
if access should be granted, denied, or if additional information is needed. 
The endpoint's responses are typically concise, aiming to provide a rapid decision that PEPs can enforce in real-time. 
The goal is to provide a scalable, secure interface for dynamic and fine-grained access control across applications.

During Access Evaluation request processing `Access Evaluation` custom script is executed. 
Name of the script must be specified by `accessEvaluationScriptName` configuration property. 
If AS can't find such script or if configuration property is not specified then server executes first script it finds on database.
AS comes with sample demo script which shows simple example of custom validation and granting access.

**Sample request**
```
POST /jans-auth/restv1/access/v1/evaluation HTTP/1.1
Host: happy-example.gluu.info
Content-Type: application/json
Authorization: Basic M2NjOTdhYWItMDE0Zi00ZWM5LWI4M2EtNTE3MTRlODE3MDMwOmFlYmMwZWFhLWY5N2YtNDU5NS04ZWExLWFlNmU1NDFmNDZjNg==

{"subject":{"id":"alice@acmecorp.com","type":"super_admin","properties":null},"resource":{"id":"123","type":"account","properties":null},"action":{"name":"can_read","properties":{"method":"GET"}},"context":{"properties":null}}
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

### New methods
| Method header | Method description |
|:-----|:------|
|`def evaluation(self, context)`| Called when the request is received and contains main logic of evaluation. It must return `AccessEvaluationResponse`. |

`evaluation` method returns `AccessEvaluationResponse` which indicates to RP whether to grant access or deny it.


### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| The custom script object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |


## Sample Demo Custom Script

### Script Type: Java

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.jans.as.server.service.external.context.ExternalScriptContext;
import io.jans.model.SimpleCustomProperty;
import io.jans.model.authzen.AccessEvaluationRequest;
import io.jans.model.authzen.AccessEvaluationResponse;
import io.jans.model.authzen.AccessEvaluationResponseContext;
import io.jans.model.authzen.Resource;
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
}

```


## Sample Scripts
- [Access Evaluation](../../../script-catalog/access_evaluation/AccessEvaluation.java)
