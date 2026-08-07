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

When additional claims are required, Janssen Server directs the requesting party to the Claims Gathering endpoint to provide the information needed to satisfy the authorization policies.

After the required claims have been collected, the client continues the UMA authorization flow by sending the permission ticket to the token endpoint. Janssen Server then evaluates the authorization request and, if the policy requirements are satisfied, issues or updates the Requesting Party Token (RPT).

For protocol details, see the UMA 2.0 Grant specification:

- [Interactive Claims Gathering Flow (Section 3.3.2)](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html#rfc.section.3.3.2)
- [Returning Authorization Data to the Client (Section 3.3.3)](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html#rfc.section.3.3.3)

## Configure Claims Gathering

Claims Gathering is driven by the UMA authorization policies configured for a protected resource. When additional claims are required, Janssen Server invokes the configured UMA Claims Gathering script to collect the required information from the requesting party.

For information about implementing and configuring a Claims Gathering script, see
[UMA Claims Gathering (Web Flow)](../../../script-catalog/uma_claims_gathering/uma-claims-web.md).

For information about configuring UMA resources and authorization policies using the Jans CLI, see [Using Command Line](../../config-guide/auth-server-config/oauth-umaresources-config.md#using-command-line).
