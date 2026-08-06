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

The RPT endpoint is part of the Janssen Server UMA 2.0 implementation. It is used to exchange a permission ticket obtained from a protected resource for a Requesting Party Token (RPT), or to update an existing RPT with additional permissions.

The endpoint follows the [UMA 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html) specification. Requests typically include:

- A permission ticket issued by the resource server.
- Client authentication, according to the registered client configuration.
- An existing RPT when requesting additional permissions (optional).

If the authorization policies are satisfied, Janssen Server issues a new RPT or updates the existing RPT with the newly granted permissions. If additional claims are required before access can be granted, the server returns the appropriate UMA response so that the client can continue the Claims Gathering flow.

For more information about RPT usage, permission upgrades, and UMA-related configuration properties, see the [UMA RPT Token](../../auth-server/tokens/uma-rpt-token.md).

## Configure Using Jans CLI

For information about configuring UMA resources using the Jans CLI, see [Using Command Line](../../config-guide/auth-server-config/oauth-umaresources-config.md#using-command-line).
