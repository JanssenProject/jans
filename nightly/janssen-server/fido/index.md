# FIDO & Passkeys Administration Guide

Janssen’s FIDO2 server enables relying parties (RPs) to register and authenticate users via hardware security keys, platform authenticators (e.g., Apple Touch ID, Windows Hello), and synced passkeys.

## Ecosystem Component Map

Janssen’s FIDO2 architecture is comprised of several interacting components:

1. **Janssen FIDO2 Server**: A standalone component in the Janssen Project that hosts the REST endpoints for WebAuthn attestation (registration) and assertion (authentication) processes. It verifies FIDO credentials against trust roots and caches metadata.
1. **FIDO Interception Scripts**: Flexible [custom scripts](#interception-scripts) that manage the user journey, linking standard username/password authentication with FIDO-based multi-factor authentication (MFA) or passwordless/usernameless flows.
1. **Agama Lab Passkey & Security Key Projects**: Pre-packaged Agama projects that enable developers to implement native [security key](https://github.com/GluuFederation/agama-securitykey) and [passkeys flow](https://github.com/GluuFederation/agama-passkey) using out-of-the-box low-code approach.
1. **Admin Tools**:
   - **Jans TUI**: Text-based User Interface for quick configuration updates.
   - **Jans CLI**: Command-line interface for dynamic properties management.
   - **Jans Config API**: RESTful programmatic endpoints to manage FIDO2 server settings.
1. **Casa**: [Casa](https://docs.jans.io/nightly/casa/index.md) is a self-service user credentials management portal, providing an interface for end-users to register, view, and delete their own security keys and passkeys.

For details on the technical architecture of the FIDO ecosystem, see the [FIDO2 server design documentation](https://docs.jans.io/nightly/contribute/implementation-design/jans-fido2-design/index.md).

## Feature Highlights

### Passkeys Support

Janssen provides native, out-of-the-box support for passkeys, offering users seamless cross-device synchronization and platform-level biometrics. To get started with passkey deployment, refer to the [Passkeys Implementation Guide](https://docs.jans.io/nightly/janssen-server/recipes/passkey-impl-guide/index.md).

### FIDO Metric API

Janssen server provides [FIDO Metric API](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-fido2/docs/jansFido2Swagger.yaml). These APIs enable collection of vital operational metrics such as:

- Number of active registrations
- Registration requests
- Assertion completions

This information helps system administrators to monitor the health and adoption rate of MFA methods. See [Passkey Telemetry & Metrics](https://docs.jans.io/nightly/janssen-server/fido/passkey-telemetry/index.md) for what is collected, how aggregation and retention work, and how to consume the data.

### Interception Scripts

Custom scripts drive Janssen's extensibility. Developers can hook into attestation and assertion cycles (start and finish hooks) to perform custom user validations, query risk engines, modify returned assertion/attestation parameters, or integrate external authorization rules during WebAuthn sessions.

- **[FIDO2 External Authenticator Script](https://docs.jans.io/nightly/script-catalog/person_authentication/fido2-external-authenticator/index.md)**: Custom authentication interception logic for MFA and passwordless flows.
- **[FIDO2 Extension Script](https://docs.jans.io/nightly/script-catalog/fido2_extension/fido2-extension/index.md)**: Customize WebAuthn registration/authentication assertions directly within server cycles.

### Fido Metadata Service (MDS)

Janssen FIDO2 server uses [Metadata Service (MDS)](https://fidoalliance.org/metadata/) to dynamically fetch, verify, and cache attestation statements for certified authenticators. This allows organizations to enforce policies (e.g., rejecting uncertified devices or locking down authentication to FIPS-compliant security keys). See [FIDO Vendor Metadata Management](https://docs.jans.io/nightly/janssen-server/fido/vendor-metadata/index.md) for more details.

## Related Documentation

- **[FIDO2 Server Configuration](https://docs.jans.io/nightly/janssen-server/fido/fido2-server-properties-config/index.md)**: Dynamic and static configuration reference schema.
- **[Conditional UI & Fallback Strategies](https://docs.jans.io/nightly/janssen-server/fido/conditional-ui-and-fallback/index.md)**: Implementing usernameless autofill passkeys and handling exceptions gracefully.
