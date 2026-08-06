---
tags:
  - administration
  - auth-server
  - uma
  - feature
  - endpoint
---
# RPT Endpoint

## Overview

The Requesting Party Token (RPT) endpoint is part of the Janssen Server User-Managed Access (UMA) 2.0 implementation. It is used by clients to exchange a permission ticket for a Requesting Party Token (RPT) or to update an existing RPT with additional permissions.

For complete protocol details, see the
[UMA 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html).

## Endpoint

The URL for the RPT endpoint is the OAuth 2.0 token endpoint published in the response of the Janssen Server well-known configuration endpoint shown below.

```text
https://janssen.server.host/jans-auth/.well-known/openid-configuration
```

The `token_endpoint` claim in the response specifies the URL used for UMA token requests. By default, the token endpoint looks similar to the following:

```text
https://janssen.server.host/jans-auth/restv1/token
```

Clients obtain an RPT by sending a request to this endpoint using the UMA grant type (`urn:ietf:params:oauth:grant-type:uma-ticket`). A typical request includes:

- A permission ticket issued by the protected resource.
- Client authentication according to the registered client's authentication method.
- An existing RPT when requesting additional permissions (optional).

If the authorization policies are satisfied, Janssen Server issues a new RPT or updates the existing RPT with the granted permissions. If additional claims are required before access can be granted, the Authorization Server returns the appropriate UMA response so the client can continue the Claims Gathering flow.

For more information about RPT usage, permission upgrades, and UMA-related configuration properties, see the [UMA RPT Token](../../auth-server/tokens/uma-rpt-token.md).

## Configure Using Jans CLI

For information about configuring UMA resources using the Jans CLI, see [Using Command Line](../../config-guide/auth-server-config/oauth-umaresources-config.md#using-command-line).
