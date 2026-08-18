---
tags:
  - administration
  - planning
  - passwordless
  - passkeys
  - webauthn
  - fido2
---

Auth Server is not proscriptive about how an organization authenticates a
person. If you want to avoid passwords, that's great. Everyone knows
passwords are terrible.

Modern authentication flows use a series of web pages to mitigate sufficient
risk, enabling them to issue an assertion about the subject's identity. In
order to perform authentication, identification is a given. This implies that
all authentication flows normally start with the subject asserting some kind of
identifier: username, email address, phone number, or any other identifier that
uniquely identifies a person. Auth Server is not opinionated about how you do
this. You could prompt the user to enter their identifier. Or for example, you
could ask the user to scan a QR code, and identify a phone identified with a
person.

With identification done, Auth Server can present any number of additional
web pages to establish that identity. These pages can ask for any "factors".
For example, if you want to perform two factor authentication in one step,
you could use a FIDO 2 credential, which combines possession with either
knowledge or biometric. But in practice, you could ask for any one or more
combinations of credentials--none of which must include a password.

Net-net, "passwordless" is really just marketing jargon. Normally it implies
some kind of risk assessment to optimize user experience. If you can imagine
any such authentication flow, you can implement it in Auth Server.

## Passkeys with Janssen

The most widely deployed passwordless credential today is the **passkey** — a
phishing-resistant, cross-device FIDO2/WebAuthn credential backed by platform
biometrics (Touch ID, Face ID, Windows Hello) or a hardware security key. Janssen
ships a complete passkey stack, so this is a first-class capability of a Janssen
deployment, not an add-on:

- **FIDO2 server** — the [FIDO2 component](components.md) hosts the WebAuthn
  attestation (registration) and assertion (authentication) endpoints, validates
  credentials against FIDO metadata, and records [passkey telemetry](../fido/passkey-telemetry.md).
- **Login experience** — either the built-in FIDO2 person-authentication script or the
  low-code [Agama passkey project](https://github.com/GluuFederation/agama-passkey).
- **Self-service** — [Casa](../../casa/index.md) lets end users register, rename, and
  delete their own passkeys.

When planning a deployment, decide which of the two integration paths you need:

| Path | What you get | Best for |
|---|---|---|
| **Script-based** (FIDO2 APIs + interception script) | Full control; you wire the flow into your own UI | Custom or existing front-ends |
| **Out-of-the-box** (Agama passkey flow + Casa) | A ready-made login and self-service experience | Fast, fully-supported end-to-end rollout |

### Enabling the FIDO2 component

The FIDO2 component is installed and enabled as part of the Janssen setup. To turn
passkeys into a working experience:

1. **Install / enable FIDO2** — ensure the FIDO2 component is selected during
   [installation](../install/README.md); it exposes the WebAuthn endpoints described above.
2. **Enable a passkey flow** — enable the FIDO2 person-authentication script, or deploy the
   Agama passkey project.
3. **Verify** — follow the [Passkeys Implementation Guide](../recipes/passkey-impl-guide.md)
   for the end-to-end steps, and confirm operations are flowing via the
   [telemetry health check](../fido/passkey-telemetry.md#quick-start-is-telemetry-healthy).

For the full, actionable walkthrough of both paths, see the
[Passkeys Implementation Guide](../recipes/passkey-impl-guide.md).
