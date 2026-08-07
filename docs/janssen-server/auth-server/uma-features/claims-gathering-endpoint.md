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

When additional claims are required, Janssen Server returns a response instructing the client to redirect the requesting party to the Claims Gathering endpoint. The client redirects the requesting party to the endpoint together with the required parameters, including `client_id`, the current permission ticket, and `claims_redirect_uri`.

After the required claims have been collected, Janssen Server redirects the requesting party back to the client with a new permission ticket. The client then submits the new permission ticket to the token endpoint, where Janssen Server evaluates the authorization request. If the policy requirements are satisfied, the server issues a Requesting Party Token (RPT). If the client explicitly presents an existing RPT, the granted permissions are added to that RPT.

For protocol details, see the UMA 2.0 Grant specification:

- [Interactive Claims Gathering Flow (Section 3.3.2)](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html#rfc.section.3.3.2)
- [Returning Authorization Data to the Client (Section 3.3.3)](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html#rfc.section.3.3.3)

## Configure Claims Gathering

Claims Gathering is driven by the UMA authorization policies configured for a protected resource and the associated Claims Gathering script.

- To enable or disable the UMA feature, see the [UMA Feature Flag](../../reference/json/feature-flags/janssenauthserver-feature-flags.md#uma).
- To configure UMA authorization policies, see [UMA RPT Policy](../../../script-catalog/uma_rpt_policy/uma-rpt.md).
- To implement and configure a Claims Gathering script, see [UMA Claims Gathering (Web Flow)](../../../script-catalog/uma_claims_gathering/uma-claims-web.md).
- For UMA-related Authorization Server configuration properties, see the [Janssen Authorization Server Configuration Properties](../../reference/json/properties/janssenauthserver-properties.md). For example, the `umaTicketLifetime` property is documented [here](../../reference/json/properties/janssenauthserver-properties.md#umaticketlifetime).
- For information about configuring UMA resources using the Jans CLI, see [Using Command Line](../../config-guide/auth-server-config/oauth-umaresources-config.md#using-command-line).
