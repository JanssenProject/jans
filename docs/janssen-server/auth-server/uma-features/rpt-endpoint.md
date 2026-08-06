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

The Requesting Party Token (RPT) endpoint is used to obtain or upgrade a Requesting Party Token (RPT) during the User-Managed Access (UMA) authorization flow. An RPT is an OAuth 2.0 access token that represents the permissions granted to a requesting party for one or more protected resources.

When a client attempts to access a protected resource without sufficient authorization, the resource server returns a permission ticket. The client presents this ticket to the RPT endpoint, where Janssen Server evaluates the applicable authorization policies. If the request is authorized, the server issues a new RPT or updates the existing RPT with the granted permissions.

For complete protocol details, see the
[UMA 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html).

## Configure Using Jans CLI

For information about configuring UMA resources using the Jans CLI, see [Using Command Line](../../config-guide/auth-server-config/oauth-umaresources-config.md#using-command-line).
