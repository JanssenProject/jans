---
tags:
  - administration
  - developer
  - script-catalog
---

# Authorization Challenge Custom Script Guide

The Janssen Authorization Challenge custom script controls authentication and authorization decisions made through the Authorization Challenge endpoint. Janssen supports this endpoint as part of its [OAuth 2.0 for First-Party Applications](https://www.ietf.org/archive/id/draft-parecki-oauth-first-party-apps-02.html) functionality.

The script allows developers to implement custom authentication logic, validate request parameters, manage authorization challenge sessions, and determine whether an authorization code should be returned.

## Purpose

The Authorization Challenge script is executed when a request is sent to the `/jans-auth/restv1/authorize-challenge` endpoint.

The script can:

- Validate authentication parameters submitted by the client.
- Authenticate users using custom logic.
- Return authorization errors when validation or authentication fails.
- Associate an authenticated user with the authorization request.
- Maintain session information across multiple authorization steps.

Janssen provides a default script named `default_challenge`, which performs username/password authentication.

The `acr_values` request parameter can identify the custom script to execute. If it is absent or no matching script is found, Janssen falls back to the script configured through `authorizationChallengeDefaultAcr`, which defaults to `default_challenge`.


## Source of Script

The following Java examples are available in the Janssen script catalog:

- [Default Authorization Challenge](AuthorizationChallenge.java) — validates username and password and authenticates an active user.
- [Multi-step Authorization Challenge](multi_step/AuthorizationChallenge.java) — demonstrates a two-step authorization flow using a username and an OTP parameter.

Both examples implement the same Authorization Challenge script interface. The multi-step implementation is an alternative example, not a separate script type.

## Interface
The Authorization Challenage script implements the [AuthorizationChallenageType](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/type/authzchallenge/AuthorizationChallengeType.java) interface.

### Inherited Methods
| Method | Description |
|:-----|:------|
| `def init(self, customScript, configurationAttributes)` | Called during script initialization. Can be used to initialize objects and resources. |
| `def destroy(self, configurationAttributes)` | Called when the script is destroyed. Can be used to release initialized resources. |
| `def getApiVersion(self)` | Returns the API version supported by the script implementation. |

### New methods
| Method | Description |
|:-----|:------|
|`def authorize(Object context)`| Processes the authorization challenge request and returns `true` or `false`. |
|`def getAuthenticationMethodClaims(Object context)`| Returns authentication method claims that can be included in the ID token. |
|`def prepareAuthzRequest(Object context)`| Prepares the authorization request before authorization, including restoring session information when needed |

The `authorize()` method returns true or false, indicating whether the Authorization Challenge endpoint should issue an authorization code.

If required parameters are missing or authentication fails, the script should create an appropriate error and return false.

When authentication succeeds, the script should associate the authenticated user with the authorization context:

`context.getExecutionContext().setUser(user);`

This allows Janssen Auth Server to associate the authenticated user with the resulting tokens.

The `prepareAuthzRequest()` method allows the script to modify the authorization request before `authorize()` is invoked. For multi-step authentication, it can restore information stored in an authorization challenge session into the current request.

### Objects
| Object name | Object description |
|:-----|:------|
|`customScript`| Custom script configuration object. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-core/script/src/main/java/io/jans/model/custom/script/model/CustomScript.java) |
|`context`| Context object provided to the active sessions response callback. [Reference](https://github.com/JanssenProject/jans/blob/main/jans-auth-server/server/src/main/java/io/jans/as/server/service/external/context/ExternalScriptContext.java) |
