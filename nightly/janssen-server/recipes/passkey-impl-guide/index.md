# Passkeys Implementation Guide

This guide provides a comprehensive, hands-on recipe for implementing and deploying Passkeys in the Janssen Server. Passkeys represent a passwordless, secure authentication mechanism using public-key cryptography, offering seamless platform-level biometrics and cross-device credentials.

## Passkey Credentials

Before beginning implementation, it is important to understand the terminology and how the browser guides authenticators:

### Types of Credentials

- **Passkeys**: A broad, consumer-friendly term encompassing various credential forms.
- **Synced Passkeys**: Credentials synced across a user's cloud account (Apple iCloud, Google Password Manager) allowing logins across multiple devices.
- **Device-Bound Passkeys**: Credentials bound to a single physical device (such as a hardware security key or TPM).
- **Discoverable Credentials / WebAuthn Resident Keys**: Credentials stored directly on the authenticator device containing user metadata, enabling usernameless logins.

### WebAuthn Hints

WebAuthn hints (configured in the [FIDO2 Server Configuration](https://docs.jans.io/nightly/janssen-server/config-guide/fido2-config/janssen-fido2-configuration/index.md)) guide the Relying Party (RP) interface during registration or authentication:

- `security-key`: Suggests using external hardware tokens (USB, NFC, Bluetooth) such as YubiKeys.
- `client-device`: Suggests using internal platform authenticators built into the device (Touch ID, Face ID, Windows Hello).
- `hybrid`: Suggests using a smartphone or secondary device via QR codes or bluetooth pairing.

## Passkey Flow

[Conditional UI](https://docs.jans.io/nightly/janssen-server/fido/conditional-ui-and-fallback/index.md) allows browsers to autofill username fields with available passkeys. The flow works as follows:

```
sequenceDiagram
    participant User
    participant RP as Relying Party (RP)
    participant Server as Janssen FIDO2 Server

    User->>RP: Page Loaded (autofill input)
    RP->>Server: Get options with cookie (allowList if present)
    Server-->>RP: Returns PublicKeyCredential Request Options
    Note over User: Selects passkey from autofill & authenticates (FaceID/TouchID)
    RP->>Server: POST Authenticator Response (verify result)
    Server-->>RP: Returns Session & User info
    RP-->>User: Successful Login
```

The Janssen server offers two different ways to implement passkeys. Both achieve the same result; the Agama-based implementation follows a low-code approach. Internally, the Agama-based implementation uses the same scripts and allows for the same level of customization as the script-based implementation.

- [Script-based Passkey Implementation](#script-based-passkey-implementation)
- [Agama-based Passkey Implementation](#agama-based-passkey-implementation)

### Script-based Passkey Implementation

This path uses the built-in FIDO2 external authenticator script to orchestrate multi-factor or passwordless authentication.

#### Step 1: Enable the FIDO2 Custom Script

1. Open the Jans TUI or execute the following CLI command to fetch the custom script details:

   ```
   jans cli --operation-id get-config-scripts-by-type --url-suffix type:PERSON_AUTHENTICATION
   ```

1. Locate the custom script with the display name `fido2` and set its `"enabled"` attribute to `true`.

1. Save or update the script using:

   ```
   jans cli --operation-id put-config-scripts --data /tmp/updated_fido2_script.json
   ```

#### Step 2: Configure the Relying Party (Cookie Guidelines)

To support usernameless login, the Relying Party must write a cookie named `allowList` upon successful registration. * **Cookie Structure**:

```
[{"id": "<credential-id-base64url>", "type": "public-key", "transports": ["internal", "usb"]}]
```

- **Cookie Security**: Ensure the cookie is written with secure attributes: `Secure=true`, `HttpOnly=true`, and `SameSite=Strict`.
- **Script Integration**: Refer to the Python code inside [Fido2ExternalAuthenticator.py](https://docs.jans.io/nightly/script-catalog/person_authentication/fido2-external-authenticator/Fido2ExternalAuthenticator.py) which handles reading and parsing the `allowList` cookie during authorization.

#### Step 3: Bind WebAuthn to the UI Form

Add the `autocomplete` hint to your login XHTML input fields to trigger the browser's Conditional UI passkey prompt:

```
<h:inputText id="username" autocomplete="username webauthn" value="#{credentials.username}" />
```

______________________________________________________________________

### Agama-based Passkey Implementation

Agama provides a pre-packaged, graphical orchestration flow designed to deploy passkeys with zero scripting and minimal configuration.

#### Step 1: Obtain the Agama Passkey Project

Add the Agama passkey project to your Janssen server using [TUI](https://docs.jans.io/nightly/janssen-server/config-guide/auth-server-config/agama-project-configuration/#using-text-based-ui).

#### Step 2: Configure and Test the Agama Flow

Use the [instructions](https://github.com/GluuFederation/agama-passkey/blob/main/README.md) to configure and test the passkey Flow.

## End-User Management via Casa

Janssen provides [Casa](https://docs.jans.io/nightly/casa/index.md) as a self-service portal, empowering users to self-administer their credentials. After installation of the passkey project, Casa allows the following:

1. **Accessing Casa**: Users navigate to the Casa portal and authenticate using their primary credentials.
1. **Registering a Passkey**:
1. Navigate to the **Passkeys** or **Security Keys** section.
1. Click **Add New**.
1. Confirm the browser-prompted biometrics or key tap. Casa registers the credential directly to the user record.
1. **Removing a Passkey**:
1. Users can view their registered keys, labeled by date and type.
1. Click **Delete** next to any compromised or lost passkey to instantly revoke the public key server-side.

______________________________________________________________________

## Admin Operations

System administrators can monitor and manage passkeys using:

### Text-Based UI (TUI)

- Navigate to the **Users** section.
- Search for a user inum.
- Manage, inspect, or delete FIDO2 registration entries associated with the user profile.

### Command Line (Jans CLI)

- To get FIDO2 configuration properties:

  ```
  jans cli --operation-id get-properties-fido2
  ```

- To update FIDO2 configuration properties:

  ```
  jans cli --operation-id put-properties-fido2 --data /tmp/new_fido2_config.json
  ```

Refer to [FIDO2 configuration documentation](https://docs.jans.io/nightly/janssen-server/config-guide/fido2-config/janssen-fido2-configuration/index.md) for more details.

### Config API

Use the standard JSON configuration properties endpoint for programmatic automation:

```
PUT /jans-config-api/fido2/configuration
```

For API specification mappings, refer to the [Config-API OpenAPI Document](https://docs.jans.io/nightly/janssen-server/reference/openapi/index.md).
