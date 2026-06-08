# UMA Claims Gathering Endpoint Documentation

## Overview
The UMA Claims Gathering Endpoint provides an interactive web flow or API interface where the authorization server interacts with the requesting party to gather claims required by the relevant UMA scopes and policies.

For the complete architectural specification, refer to the [UMA 2.0 Grant Specification - Claims Gathering Section](https://docs.oasis-open.org/uma/wg/oauth-uma-grant-2.0/v1.0/oauth-uma-grant-2.0-v1.0.html#claims-gathering).

## Janssen Server Interception Scripts
Claims gathering is highly customizable in Janssen Server using interception scripts.

* **Script Type:** `UMA Claims Gathering (Web Flow)`
* **Description:** Intercepts authorization flows to invoke custom steps, multi-factor challenges, or direct attribute requests from a user before returning permission tokens.
* **Configuration Property:** Ensure the property `umaClaimsGatheringEnabled` is set to `true` within your global feature flags.

## How to Configure via TUI / Command Line
1. Launch the Janssen Text UI (`jans-tui`).
2. Access the **Custom Scripts** or **Interception Scripts** section.
3. Select **UMA Claims Gathering** from the script type dropdown list.
4. Toggle the script state to **Enabled** and define any custom parameters required for your custom business logic workflow.
5. Save changes to update the underlying storage layer immediately.
