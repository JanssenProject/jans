---
tags:
- administration
- auth-server
- session-revocation
- endpoint
---

# Overview

Janssen Server provides session revocation endpoint to enable the client to revoke all sessions of a users.
Though not being part of any industry standard/specification, Janssen Server provides this endpoint to allow greater 
control and better management of sessions on OP.

URL to access revocation endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`session_revocation_endpoint` claim in the response specifies the URL for revocation endpoint. By default, revocation endpoint
looks like below:

```
https://janssen.server.host/jans-auth/restv1/revoke_session
```

More information about request and response of the revocation endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/replace-janssen-version/jans-auth-server/docs/swagger.yaml#/Session_Management/revoke-session).

## Usage

A request to this endpoint can revoke all sessions of one particular user. Use the request parameters to specify 
criteria to select the user. If there are multiple users matching the given criteria, the first found user will be affected.

## Disabling The Endpoint Using Feature Flag

`Session revocation` endpoint can be enabled or disable using [REVOKE_SESSION feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#revokesession).
Use [Janssen Text-based UI(TUI)](../../config-guide/tui.md) or [Janssen command-line interface](../../config-guide/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `REVOKE_SESSION` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Required Scopes

A client must have the following scope in order to use this endpoint:

- `revoke_session`

