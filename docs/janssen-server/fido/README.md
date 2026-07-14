---
tags:
  - administration
  - fido
  - passkeys
  - architecture
---

# FIDO & Passkeys Administration Guide

Janssen’s FIDO2 server enables relying parties (RPs) to register and authenticate users via hardware security keys, platform authenticators (e.g., Apple Touch ID, Windows Hello), and synced passkeys.

## Ecosystem Component Map

Janssen’s FIDO2 architecture is comprised of several interacting components:

1. **Janssen FIDO2 Server**: A standalone component in the Janssen Project that hosts the REST endpoints for WebAuthn attestation (registration) and assertion (authentication) processes. It verifies FIDO credentials against trust roots and caches metadata.
2. **FIDO Interception Scripts**: Flexible [custom scripts](#interception-scripts) that manage the user journey, linking standard username/password authentication with FIDO-based multi-factor authentication (MFA) or passwordless/usernameless flows.
3. **Agama Lab Passkey & Security Key Projects**: Pre-packaged Agama projects that enable developers to implement native [security key](https://github.com/GluuFederation/agama-securitykey) and [passkeys flow](https://github.com/GluuFederation/agama-passkey) using out-of-the-box low-code approach.
4. **Admin Tools**:
    * **Jans TUI**: Text-based User Interface for quick configuration updates.
    * **Jans CLI**: Command-line interface for dynamic properties management.
    * **Jans Config API**: RESTful programmatic endpoints to manage FIDO2 server settings.
5. **Casa**: [Casa](../../casa/index.md) is a self-service user credentials management portal, providing an interface for end-users to register, view, and delete their own security keys and passkeys.

Details on technical architecture of Fido ecosystem can be found [here](../architecture/fido2.md).

## Feature Highlights

### Passkeys Support
Janssen provides native, out-of-the-box support for passkeys, offering users seamless cross-device synchronization and platform-level biometrics. To get started with passkey deployment, refer to the [Passkeys Implementation Guide](../recipes/passkey-impl-guide.md).

### FIDO Metric API 
Janssen server provides [FIDO Metric API](https://gluu.org/swagger-ui/?url=https://raw.githubusercontent.com/JanssenProject/jans/vreplace-janssen-version/jans-fido2/docs/jansFido2Swagger.yaml). These APIs enable collection of vital operational metrics such as:

- Number of active registrations
- Registration requests
- assertion completions

This information helps system administrators to monitor the health and adoption rate of MFA methods.

### Interception Scripts
Custom scripts drive Janssen's extensibility. Developers can hook into attestation and assertion cycles (start and finish hooks) to perform custom user validations, query risk engines, modify returned assertion/attestation parameters, or integrate external authorization rules during WebAuthn sessions.

* **[FIDO2 External Authenticator Script](../../script-catalog/person_authentication/fido2-external-authenticator/README.md)**: Custom authentication interception logic for MFA and passwordless flows.
* **[FIDO2 Extension Script](../../script-catalog/fido2_extension/fido2-extension.md)**: Customize WebAuthn registration/authentication assertions directly within server cycles.

### Fido Metadata Service (MDS)
Janssen FIDO2 server uses [Metadata Service (MDS)](https://fidoalliance.org/metadata/) to dynamically fetch, verify, and cache attestation statements for certified authenticators. This allows organizations to enforce policies (e.g., rejecting uncertified devices or locking down authentication to FIPS-compliant security keys). See [FIDO Vendor Metadata Management](vendor-metadata.md) for more details.

## Related Documentation

* **[FIDO2 Server Configuration](fido2-server-config.md)**: Dynamic and static configuration reference schema.
* **[Conditional UI & Fallback Strategies](conditional-ui-and-fallback.md)**: Implementing usernameless autofill passkeys and handling exceptions gracefully.



