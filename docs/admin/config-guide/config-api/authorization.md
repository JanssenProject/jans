---
tags:
  - administration
  - config-api
---

## Endpoints protection
Config API endpoints are OAuth 2.0 protected. It supports simple bearer and JWT token.

## 


1) If a user has write permission then should be able to read data as well. That is no need for explicit read permission
2) Need super permission to execute endpoints
 - `https://jans.io/oauth/config/read-all` should be able to able to execute read for all endpoints
- `https://jans.io/oauth/config/write-all` should be able to execute add/update/delete for all endpoints
3) Functionality wise group permission to enable excecution of all endpoints
 - example all openid endpoints (client, scope), all admin-ui endpoints(role, mapping, permission, license)
 https://jans.io/oauth/config/oauth-write
https://jans.io/oauth/config/oauth-read

### Implementation
**Metadata:**
An endpoint can be annotated with endpoint specific scope, feature level scope(Group) or admin level scope using [@ProtectedApi](https://github.com/JanssenProject/jans/blob/main/jans-config-api/shared/src/main/java/io/jans/configapi/core/rest/ProtectedApi.java) wherein;
- scope -> endpoint specific granular permissions
- groupScopes -> feature level permissions
- superScopes -> Admin level permissions

**Logic:**
- superScopes: If the access token has **any** of super scopes then no need to check group or granular scopes
- groupScopes: If the access token does not have any of the applicable super scopes then check if the access token has group level scopes. If **any** of the group level scope present then no need to check group or granular scopes.
- scope: If access token does not have any of the super or group level scopes then check if **all** the applicable endpoint specific granular scopes are present.


**Example:**
OpenID Client GET endpoint annotation
`@ProtectedApi(scopes = { "https://jans.io/oauth/config/openid/clients.readonly" }, groupScopes = {
            "https://jans.io/oauth/config/openid/openid-write", "https://jans.io/oauth/config/openid-read" }, superScopes = { "https://jans.io/oauth/config/read-all")`

- superScopes: If the access token has **https://jans.io/oauth/config/read-all** scopes then no need to check presence of any other scope.
- groupScopes: If the access token does not of any of applicable superScopes but has either of **https://jans.io/oauth/config/openid/openid-write** or **https://jans.io/oauth/config/openid-read** then no need to check presence of any other scope.
- scope: If access token does not have any of the super or group level scopes then check it should have the applicable **https://jans.io/oauth/config/openid/clients.readonly** scope to invoke the endpoint.

