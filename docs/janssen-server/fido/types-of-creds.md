*Types of credentials*

Passkeys introduce a fresh identity with a new name, icon, and features, redefining authentication methods. They serve as a broad term encompassing various credential types, including:

- Discoverable Credentials
- FIDO Credentials
- WebAuthn Discoverable Credentials
- FIDO2 Credentials
- Synced Passkeys
- Device-Bound Passkeys

*WebAuthn Hints*

WebAuthn hints, defined in the server configuration, guide the authentication process by indicating the preferred type of authenticator. The three primary values are:

- Security-Key: Suggests the use of a physical security key, often employed in high-security environments.
- Client-Device: Indicates a preference for the platform authenticator integrated into the user's current device.
- Hybrid: Recommends using a general-purpose authenticator, such as a smartphone, especially when dedicated security keys are less common among users.