# Claims Gathering Endpoint

## Overview

The Claims Gathering endpoint is part of the User-Managed Access (UMA) authorization flow. It is used when the Authorization Server determines that the claims already presented by the requesting party are not sufficient to satisfy the authorization policies protecting a resource.

When additional claims are required, Janssen Server returns a response instructing the client to redirect the requesting party to the Claims Gathering endpoint. The client redirects the requesting party to the endpoint together with the required parameters, including `client_id`, the current permission ticket, and `claims_redirect_uri`.

After the requested claims have been collected, Janssen Server redirects the requesting party to the registered `claims_redirect_uri` with a new permission ticket. The client then presents the new permission ticket to the token endpoint for authorization assessment. The token endpoint returns an RPT or an UMA error based on the authorization result.

If the client also presents an existing RPT, Janssen Server may upgrade it. An RPT should be considered upgraded only when the token response contains `"upgraded": true`.

For protocol details, see the UMA 2.0 Grant specification:

- [Interactive Claims Gathering Flow (Section 3.3.2)](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html#rfc.section.3.3.2)
- [Returning Authorization Data to the Client (Section 3.3.3)](https://docs.kantarainitiative.org/uma/wg/rec-oauth-uma-grant-2.0.html#rfc.section.3.3.3)

## Configure Claims Gathering

Claims Gathering is driven by the UMA authorization policies configured for a protected resource and the associated Claims Gathering script.

- To enable or disable the UMA feature, see the [UMA Feature Flag](https://docs.jans.io/head/janssen-server/reference/json/feature-flags/janssenauthserver-feature-flags/#uma).
- To configure UMA authorization policies, see [UMA RPT Policy](https://docs.jans.io/head/script-catalog/uma_rpt_policy/uma-rpt/index.md).
- To implement and configure a Claims Gathering script, see [UMA Claims Gathering (Web Flow)](https://docs.jans.io/head/script-catalog/uma_claims_gathering/uma-claims-web/index.md).
- For UMA-related Authorization Server configuration properties, see the [Janssen Authorization Server Configuration Properties](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/index.md). For example, the `umaTicketLifetime` property is documented [here](https://docs.jans.io/head/janssen-server/reference/json/properties/janssenauthserver-properties/#umaticketlifetime).
- For information about configuring UMA resources using the Jans CLI, see [Using Command Line](https://docs.jans.io/head/janssen-server/config-guide/auth-server-config/oauth-umaresources-config/#using-command-line).

### Register the Claims Gathering Redirect URI

The Claims Gathering redirect URI is configured as part of the client registration. In Janssen Server, set the `claimRedirectUris` property on the client to specify authorized target URIs for the Claims Gathering flow. You can configure this via Jans TUI or Jans CLI:

For instructions on configuring client properties using the Jans TUI, see [Client Management - TUI](https://docs.jans.io/head/janssen-server/auth-server/client-management/#c-tui).

For instructions on configuring client properties using the Jans CLI, see [Client Management - Jans CLI](https://docs.jans.io/head/janssen-server/auth-server/client-management/#b-jans-cli).
