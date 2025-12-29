---
tags:
- administration
- auth-server
- session-revocation
- endpoint
---

# Global Token Revocation Endpoint

The Global Token Revocation endpoint is a critical security feature that allows for the immediate invalidation of 
all tokens and sessions associated with a user. This is particularly useful in scenarios where a user's account may have been compromised, 
as it ensures that all active sessions are terminated and all tokens are revoked, preventing any further unauthorized access.

Janssen Server provides this endpoint implementation to allow greater
control and better management of sessions on OP.

The URL for the Global Token Revocation endpoint is specified in the Janssen Server's well-known [configuration endpoint](./configuration.md) 
response:

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

The `global_token_revocation_endpoint` claim in the response specifies the URL for the endpoint, which by default is:

```text
https://janssen.server.host/jans-auth/restv1/global-token-revocation
```

More information about request and response of the global token revocation endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml).

## Usage

A request to this endpoint can revoke all tokens and sessions of one particular user. Use the request parameters to specify
criteria to select the user. If there are multiple users matching the given criteria, the first found user will be affected.

**Sample request**
```text
POST /global-token-revocation
Host: example.com
Content-Type: application/json
Authorization: Bearer f5641763544a7b24b08e4f74045

{
  "sub_id": {
    "format": "uid",
    "id": "breakfast"
  }
}
```

- `format` - specifies user attribute name
- `id` - specifies user attribute value

In sample above it identifies user by `uid=breakfast`.

**Sample response**

```text
HTTP/1.1 204
```

- View full sample execution log [here](../../../assets/log/global-token-revocation-run-log.txt)

## Disabling The Endpoint Using Feature Flag

`Global Token Revocation` endpoint can be enabled or disable using [GLOBAL_TOKEN_REVOCATION feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#globaltokenrevocation).
Use [Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) or [Janssen command-line interface](../../config-guide/config-tools/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `GLOBAL_TOKEN_REVOCATION` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Required Scope

A client must have the following scope in order to use this endpoint:

- `global_token_revocation`

## Difference with Front-Channel End Session

The Global Token Revocation endpoint provides a comprehensive approach to session termination by 
revoking all tokens and sessions associated with a user across all applications. 
This is a "back-end" operation that ensures all access is immediately cut off.

In contrast, the Front-Channel End Session (`/end_session`) is a "front-end" mechanism 
that logs a user out of a single application. 
It relies on the user's browser to clear the session cookie and does not guarantee that 
all tokens and sessions are invalidated across all applications.

For more details, see [End Session](end-session.md).
