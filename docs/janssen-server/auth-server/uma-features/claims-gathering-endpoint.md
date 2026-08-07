---
tags:
  - administration
  - auth-server
  - uma
  - feature
  - endpoint
---

# Claims Gathering Endpoint

## Overview

The Claims Gathering endpoint is part of the User-Managed Access (UMA) authorization flow. It is used when the Authorization Server determines that the claims already presented by the requesting party are not sufficient to satisfy the authorization policies protecting a resource.

If the available claims are not sufficient to satisfy the authorization policies, Janssen Server informs the client which additional claims are needed. The client submits the requested claims, together with the associated permission ticket, to the Claims Gathering endpoint.

Janssen Server evaluates the submitted claims and, if the authorization policies are satisfied, continues the authorization flow by issuing or updating the Requesting Party Token (RPT).

For complete protocol details, see the
[UMA 2.0 Grant for OAuth 2.0 Authorization](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html).

## Configure Using Jans CLI

For information about configuring UMA resources using the Jans CLI, see  [Using Command Line](../../config-guide/auth-server-config/oauth-umaresources-config.md#using-command-line).
