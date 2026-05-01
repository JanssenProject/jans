---
tags:
  - administration
  - auth-server
  - openidc
  - feature
  - consent
---
# Customize

Janssen supports customizing consent beyond the default scope approval page. The implementation uses a Consent Gathering extension point during authorization, and can run either script-based logic or Agama-based consent flows.

The detailed customization guide already exists in script catalog docs:

- [Consent Gathering](../../../../script-catalog/consent_gathering/consent-gathering.md)

This page summarizes where customization hooks into the auth server and which properties are most relevant.

## Customization Options

- **Consent Gathering scripts**: implement custom pages, multistep consent, extra business validations, and allow/deny decisions.
- **Agama consent flows**: use Agama for richer orchestration, branching, and reusable flow logic.
- **Client-level script assignment**: assign consent scripts directly in client configuration.
- **ACR-based routing**: map `acr_values` to specific consent scripts and/or Agama consent flows.

## Key Configuration Properties

Consent script and flow selection is controlled by auth server configuration:

- `consentGatheringScriptBackwardCompatibility`
- `acrToConsentScriptNameMapping`
- `acrToAgamaConsentFlowMapping`

For property details, see:

- [Auth Server JSON Properties](../../../reference/json/properties/janssenauthserver-properties.md)

## Related OpenID Connect Behavior

- `prompt=consent` requests explicit consent interaction in the authorization flow.
- `prompt=none` does not allow interactive consent and returns an error if consent is required.

See also:

- [Authorization Endpoint](../../endpoints/authorization.md)
- [Prompt Parameter](../prompt-parameter.md)
