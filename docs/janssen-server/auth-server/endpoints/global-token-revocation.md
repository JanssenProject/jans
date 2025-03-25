---
tags:
- administration
- auth-server
- session-revocation
- endpoint
---
# Global Token Revocation
## Overview

Janssen Server provides global token revocation endpoint to enable the client to revoke all tokens and sessions of a user.
Janssen Server provides this endpoint to allow greater 
control and better management of sessions on OP.

URL to access revocation endpoint on Janssen Server is listed in the response of Janssen Server's well-known
[configuration endpoint](./configuration.md) given below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

`global_token_revocation_endpoint` claim in the response specifies the URL for global token revocation endpoint. By default, global token revocation endpoint
looks like below:

```
https://janssen.server.host/jans-auth/restv1/global-token-revocation
```

More information about request and response of the global token revocation endpoint can be found in
the OpenAPI specification of [jans-auth-server module](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-auth-server/docs/swagger.yaml).

## Usage

A request to this endpoint can revoke all tokens and sessions of one particular user. Use the request parameters to specify 
criteria to select the user. If there are multiple users matching the given criteria, the first found user will be affected.

- View full sample execution log [here](../../../assets/log/global-token-revocation-run-log.txt)

## Disabling The Endpoint Using Feature Flag

`Global Token Revocation` endpoint can be enabled or disable using [GLOBAL_TOKEN_REVOCATION feature flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#globaltokenrevocation).
Use [Janssen Text-based UI(TUI)](../../config-guide/config-tools/jans-tui/README.md) or [Janssen command-line interface](../../config-guide/config-tools/jans-cli/README.md) to perform this task.

When using TUI, navigate via `Auth Server`->`Properties`->`enabledFeatureFlags` to screen below. From here, enable or
disable `GLOBAL_TOKEN_REVOCATION` flag as required.

![](../../../assets/image-tui-enable-components.png)

## Required Scopes

A client must have the following scope in order to use this endpoint:

- `global_token_revocation`

