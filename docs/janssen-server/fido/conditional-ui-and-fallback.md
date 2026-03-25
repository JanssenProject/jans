---
tags:
  - administration
  - fido
  - passkeys
  - conditional-ui
---

# Conditional UI and Fallback Authentication

This guide provides a complete reference for Conditional UI (usernameless passkey authentication)
and all fallback strategies in Janssen's FIDO2 implementation. It consolidates information from
the authentication scripts, login UI, and server-side services into one place.

## Overview

Conditional UI lets users authenticate with a passkey directly from the browser's autofill
without typing a username or password. The browser detects registered passkeys and shows them
as autofill suggestions on the username field. If Conditional UI is unavailable or fails, the
flow falls back gracefully to traditional authentication methods.

```text
┌──────────────────────────────────────────────────────────────────────┐
│                     Authentication Flow                              │
│                                                                      │
│  ┌─────────────┐    Browser supports    ┌──────────────────────┐    │
│  │  Login Page │──  Conditional UI? ──▶│  Conditional UI Flow │    │
│  └─────────────┘         Yes            └──────────────────────┘    │
│         │                                          │                 │
│         │ No                               Success │ Failure         │
│         ▼                                          ▼                 │
│  ┌─────────────────────┐            ┌─────────────────────────┐     │
│  │ Username + Password │            │   Fallback Strategies   │     │
│  └─────────────────────┘            └─────────────────────────┘     │
└──────────────────────────────────────────────────────────────────────┘
```

---

## How Conditional UI Works

### Authentication Sequence

```text
User   ──▶  Browser: Page loads with username field (autocomplete="username webauthn")
Browser ──▶  JS: isConditionalMediationAvailable() check
JS      ──▶  FIDO Server: POST /assertion/options (with allowCredentials from cookie)
FIDO Server ──▶ JS: PublicKeyCredentialRequestOptions
JS      ──▶  Browser: credentials.get({ mediation: "conditional" })
Browser ──▶  User: Show passkey autofill suggestions
User    ──▶  Browser: Select passkey, authenticate (Face ID / Touch ID / PIN)
Browser ──▶  FIDO Server: POST /assertion/result (authenticator response)
FIDO Server ──▶  RP: Logged in (returns username from credential)
```

### Step 1 — Browser Capability Check

Before initiating Conditional UI, the login page checks whether the browser supports it:

```javascript
// login.xhtml — window.onload
window.onload = async function () {
    if (assertion_request != null) {
        if (window.PublicKeyCredential && await PublicKeyCredential.isConditionalMediationAvailable()) {
            startAssertion();
        } else {
            // Browser does not support Conditional UI — standard form remains active
            console.log("PublicKeyCredential not ConditionalMediationAvailable");
        }
    }
};
```

The check requires two conditions:
1. `window.PublicKeyCredential` — WebAuthn API is available
2. `await PublicKeyCredential.isConditionalMediationAvailable()` — the browser specifically
   supports conditional mediation (autofill-driven passkey prompts). This is a static method
   returning `Promise<boolean>` per the WebAuthn Level 3 spec; it must be awaited, not just
   checked for existence.

### Step 2 — Preparing Assertion Options

`Fido2ExternalAuthenticator.py` prepares the assertion request in `prepareForStep` (step 1).
It reads the `allowList` cookie to populate `allowCredentials`, which tells the browser which
passkeys to surface in autofill:

```python
# Fido2ExternalAuthenticator.py — prepareForStep, step 1
assertionRequest = AssertionOptions()
assertionRequest.setRpId(domain)
assertionRequest.setAllowCredentials(Arrays.asList(allowList))  # from cookie
assertionResponse = assertionService.authenticate(assertionRequest).readEntity(java.lang.String)
identity.setWorkingParameter("fido2_assertion_request", ServerUtil.asJson(assertionResponse))
```

When `allowList` is empty (no registered passkeys found in the cookie), `allowCredentials`
will be an empty list, which is acceptable — the browser may still show discoverable
credentials it knows about.

### Step 3 — Conditional Mediation Request

`webauthn.js` makes the WebAuthn API call with `mediation: "conditional"` and an
`AbortController` signal:

```javascript
// webauthn.js — getAssertionConditional
function getAssertionConditional(request) {
    const authAbortSignal = authAbortController.signal;
    return navigator.credentials.get({
        publicKey: decodePublicKeyCredentialRequestOptions(request),
        mediation: "conditional",
        signal: authAbortSignal,
    });
}
```

The `mediation: "conditional"` flag is what makes this Conditional UI. Without it, the browser
would show a modal dialog instead of using autofill.

### Step 4 — Username Field Binding

The username input must carry the `autocomplete="username webauthn"` attribute so the browser
knows to use it as the anchor for passkey autofill suggestions:

```xml
<!-- login.xhtml -->
<h:inputText placeholder="#{msgs['login.username']}"
    id="username" name="username" required="true"
    value="#{credentials.username}" styleClass="form-control">
    <f:passThroughAttribute name="autocomplete" value="username webauthn"/>
</h:inputText>
```

### Step 5 — Server-Side Verification

When the user selects a passkey, the browser returns a credential response. The login page
submits this to the server, and `Fido2ExternalAuthenticator.py` verifies it:

```python
# Fido2ExternalAuthenticator.py — authenticate, step 1
if token_response is not None:
    identity.setWorkingParameter("conditionalUI", "true")
    assertionService = Fido2ClientFactory.instance().createAssertionService(self.metaDataConfiguration)
    assertionResult = mapper.readValue(token_response, AssertionResult)
    assertionStatus = assertionService.verify(assertionResult)
    # ...
    username = assertionResponse.get("username")
    logged_in = authenticationService.authenticate(username)
```

### Authentication Steps: Conditional UI vs. Standard Flow

`getCountAuthenticationSteps` returns 1 for Conditional UI (single step, no separate FIDO
challenge page needed) and 2 for the standard username/password + FIDO flow:

```python
# Fido2ExternalAuthenticator.py
def getCountAuthenticationSteps(self, configurationAttributes):
    identity = CdiUtil.bean(Identity)
    conditionalUI = identity.getWorkingParameter("conditionalUI")
    if conditionalUI == "true":
        return 1   # Passkey directly authenticated — done
    return 2       # Username/password step, then FIDO step
```

---

## The allowList Cookie

The `allowList` cookie is the mechanism that makes usernameless authentication possible.
It stores credential descriptors on the client without linking them to a username or email.

### Structure

```json
[
  {
    "id": "<base64url-encoded credential id>",
    "type": "public-key",
    "transports": ["internal"]
  },
  {
    "id": "<base64url-encoded credential id>",
    "type": "public-key",
    "transports": ["usb", "ble", "nfc"]
  }
]
```

### Security Properties

The cookie is set with the following protections:

```python
# Fido2ExternalAuthenticator.py — persistCookie
coo = Cookie("allowList", value)
coo.setSecure(True)    # HTTPS-only transmission
coo.setHttpOnly(True)  # Not accessible via JavaScript
coo.setMaxAge(7 * 24 * 60 * 60)  # 7-day expiry
coo.setSameSite("Strict")  # CSRF protection — use "Lax" if cross-site navigation is needed
```

- **No PII stored**: the cookie contains only credential IDs and transport hints — no
  usernames, email addresses, or other identifying data
- **Base64url encoded**: the JSON array is base64url-encoded before storage
- **Deduplication**: `add_credential_if_not_exists` prevents duplicate entries when
  registering new credentials

### Writing the Cookie (Registration)

After a successful registration (attestation), the new credential is added to the cookie:

```python
# Fido2ExternalAuthenticator.py — authenticate, step 2 (enroll path)
attestationResponse = json.loads(attestationStatusEntity)
new_credential = attestationResponse.get("credential")
self.persistCookie(new_credential)
```

### Reading the Cookie (Authentication)

The cookie is read at the start of each session to build the `allowCredentials` list:

```python
# Fido2ExternalAuthenticator.py — getCookieValue
for cookie in httpRequest.getCookies():
    if cookie.getName() == "allowList":
        value = Base64Util.base64urldecodeToString(cookie.getValue())
        value = json.loads(value)
```

---

## Browser Detection

Janssen's FIDO2 server detects the user's browser and device at the server level using the
HTTP `User-Agent` header. This data is used for metrics, risk scoring, and fallback
strategy analysis — not for blocking authentication attempts.

### Detected Properties

| Property | Examples |
|---|---|
| Browser | Chrome, Firefox, Safari, Edge, Opera |
| Operating System | Windows, macOS, Linux, Android, iOS |
| Device Type | DESKTOP, MOBILE, TABLET |

Detection is performed in `DeviceInfoExtractor.java` using User-Agent string pattern matching.

### Client-Side Capability Check

On the client, `PublicKeyCredential.isConditionalMediationAvailable()` is the definitive
signal for Conditional UI support — it is a static async method that resolves to `true`
when the browser supports conditional mediation. Currently supported browsers:

| Browser | Minimum Version |
|---|---|
| Chrome / Chromium | 108+ |
| Safari | 16+ |
| Edge | 108+ |
| Firefox | 122+ |

> **Last validated:** March 2026. Browser support evolves quickly — verify against
> [passkeys.dev/device-support/](https://passkeys.dev/device-support/) for the latest
> up-to-date browser and device compatibility matrix.

---

## Fallback Strategies

When Conditional UI is unavailable or fails, Janssen provides a layered fallback hierarchy.

### Fallback 1 — Browser Does Not Support Conditional UI

**Trigger**: `window.PublicKeyCredential` is `undefined`, or
`await PublicKeyCredential.isConditionalMediationAvailable()` resolves to `false`.

**Behavior**: The `startAssertion()` call is never made. The login page stays on the
standard username/password form. The user logs in with credentials and proceeds to
FIDO2 step 2 (if they have a registered device).

**User experience**: The user sees only the username and password fields. There is no
passkey autofill in the browser's UI. No error is shown.

```javascript
// login.xhtml — conditional check
window.onload = async function () {
    if (window.PublicKeyCredential && await PublicKeyCredential.isConditionalMediationAvailable()) {
        startAssertion();  // Conditional UI path
    } else {
        // Silent fallback — standard form is used
        console.log("PublicKeyCredential not ConditionalMediationAvailable");
    }
};
```

**Recommendation**: Display a subtle informational message such as
*"Sign in faster with a passkey — upgrade your browser for the best experience."*

---

### Fallback 2 — No Passkeys Registered (Empty allowList Cookie)

**Trigger**: The `allowList` cookie is absent or empty because the user has never
registered a passkey on this device/browser.

**Behavior**: `allowCredentials` is set to an empty list. The FIDO server may return
an error when no credentials are found. The authentication script logs the event and
falls back to username/password.

**What the script does**:

```python
# Fido2ExternalAuthenticator.py — prepareForStep, step 1
allowList = self.getCookieValue()  # Returns [] if no cookie
assertionRequest.setAllowCredentials(Arrays.asList(allowList))  # Empty list
```

**User experience**: The browser may not show any passkey suggestions. The user falls
back to typing their username and password normally. In step 2, if the user has
registered FIDO devices on the server (not in the cookie), they are prompted on
`passkeys.xhtml` to use a security key or platform authenticator.

**Recommendation**: After a successful username/password login, prompt the user to
register a passkey: *"Add a passkey to sign in faster next time."*

---

### Fallback 3 — User Cancels the Conditional UI Prompt

**Trigger**: The user clicks the "Remember Me" checkbox or otherwise cancels the
in-progress Conditional UI operation.

**Behavior**: The `AbortController` signals cancellation to the WebAuthn API, which
rejects the promise with an `AbortError`. The catch handler logs the cancellation and
allows the standard form to remain interactive.

```javascript
// login.xhtml — checkRemembeMe
function checkRemembeMe() {
    if ($('#rememberme').is(':checked')) {
        localStorage.usrname = document.getElementById("loginForm:username").value;
        localStorage.chkbx = $('#rememberme').val();
    } else {
        localStorage.usrname = '';
        localStorage.chkbx = '';
    }
    console.log("Aborting this Conditional UI");
    authAbortController.abort();
}

// Error handler
} else if (err.name === 'AbortError') {
    console.error('This operation is canceled by user.');
}
```

**User experience**: No visible error. The user simply types their username and password
in the form as usual.

---

### Fallback 4 — Authenticator Not Registered (`InvalidStateError`)

**Trigger**: The user selects a passkey from autofill, but that credential ID is not
registered in the FIDO server's database (e.g., the credential was deleted server-side
or the cookie is stale).

**Behavior**: The WebAuthn API returns an `InvalidStateError`. The error is caught and
logged. The login form remains active.

```javascript
// login.xhtml — error handler
if (err.name === 'InvalidStateError') {
    console.error("This authenticator is not registered for the account. " +
                  "Please try again with a registered authenticator.");
}
```

**User experience**: The autofill attempt silently fails. The user should be shown a
message such as: *"That passkey is no longer valid. Please sign in with your password,
then re-register your passkey."*

**Recommended resolution**:
1. Guide the user to sign in with username/password
2. Navigate them to device management to remove the stale passkey
3. Re-register a new passkey on the current device

---

### Fallback 5 — No Platform Authenticator Available

**Trigger**: The device has no biometric sensor or secure enclave, or the operating
system does not expose a platform authenticator to the browser.

**Behavior**: The browser will not show platform passkeys in autofill. Cross-platform
authenticators (security keys) may still appear if their credential IDs are in the
`allowList` with transport hints `["usb", "ble", "nfc"]`.

**Detection**: Check `authenticatorAttachment` in the credential response —
`"platform"` indicates a built-in authenticator was used, `"cross-platform"` indicates
an external security key.

**User experience**: The passkey autofill may show only security key options, or may
not appear at all. The standard form is the fallback.

**Recommendation**: Offer cross-platform authenticators as an alternative:
*"Your device doesn't support passkeys. Use a security key or sign in with your
password instead."*

---

### Fallback 6 — Biometric Unavailable (Biometric Locked or Not Enrolled)

**Trigger**: The user's device has a biometric sensor, but biometric authentication is
not enrolled, or the sensor is temporarily locked (e.g., too many failed attempts).

**Behavior**: The OS may prompt the user to enter a PIN or pattern as an alternative
to biometrics. If no fallback PIN is configured, the authentication will fail.

**User experience**: The user may see a system-level PIN prompt. If that also fails,
the WebAuthn operation returns `NotAllowedError`.

```javascript
// login.xhtml — generic error handler
} else if (err.message) {
    console.error(err.name + ' : ' + err.message);
}
```

**Recommendation**: Guide users to set up a PIN on their device as a backup:
*"Set up a device PIN to use passkeys even when biometrics aren't available."*

---

### Fallback 7 — Security Key Issues

**Trigger**: A hardware security key (USB/NFC/BLE) fails to respond, is not present,
or returns an error during assertion.

**Behavior**: The browser may time out waiting for the security key, or the user may
dismiss the security key prompt. The WebAuthn API rejects with `NotAllowedError` or
a timeout error.

**Authenticator transports** in the `allowList` guide the browser on how to contact
the key:

```json
{ "id": "...", "type": "public-key", "transports": ["usb", "nfc", "ble"] }
```

**User experience**: The browser prompt disappears after timeout. The login form
remains available.

**Recommendation**: Provide a visible "Use password instead" link below the passkey
prompt so users are never stuck. For security keys, advise:
*"Make sure your security key is plugged in and press its button when prompted."*

---

### Fallback Summary Table

| Scenario | Trigger | Fallback |
|---|---|---|
| Browser unsupported | No WebAuthn / No Conditional UI API | Standard username/password form |
| No passkeys registered | Empty `allowList` cookie | Username/password → optional FIDO step |
| User cancels | AbortController / checkbox click | Standard form (no error shown) |
| Stale credential | `InvalidStateError` | Password login + re-register passkey |
| No platform authenticator | Device lacks biometrics/secure enclave | Security key or password |
| Biometric unavailable | Not enrolled or locked | Device PIN prompt or password |
| Security key failure | Key absent, timeout, or error | Password fallback |

---

## Security Considerations

### Conditional UI-Specific

- **No username enumeration**: The `allowList` cookie contains only credential IDs —
  never usernames or email addresses. An attacker reading the cookie cannot determine
  the account owner.
- **Cookie integrity**: If the `allowList` cookie is tampered with, the FIDO server will
  reject credentials that do not match stored public keys. The cookie does not bypass
  server-side verification.
- **Replay protection**: Each WebAuthn assertion includes a server-generated challenge
  that is verified server-side, preventing replay attacks.
- **Origin binding**: Credentials are bound to the Relying Party ID (the domain). A
  credential registered on `login.example.com` cannot be used on `attacker.example.net`.

### Fallback Path Security

Each fallback reduces the security level. Consider the following mitigations:

| Fallback | Risk | Mitigation |
|---|---|---|
| Password login | Credential stuffing, phishing | Rate limiting, MFA, breach detection |
| Stale credential | Orphaned credentials accumulating | Periodic cleanup, notify user |
| Security key absent | Physical key loss | Require backup MFA method at enrollment |
| Biometric bypass to PIN | PIN guessing | Lock after N attempts (OS-level) |
| AbortError / cancel | None (user-initiated) | None required |

### Attestation and Authenticator Trust

Janssen supports three attestation modes (configured in `attestationMode`):

- `disabled`: No attestation validation — accept any authenticator
- `monitor`: Validate if available, but do not block if attestation is absent
- `enforced`: Require valid attestation from FIDO-certified authenticators

For high-security deployments, use `enforced` mode with the FIDO Metadata Service (MDS3)
to restrict authentication to certified authenticators. See the
[Vendor Metadata guide](vendor-metadata.md) for details.

### AbortController Security

The global `authAbortController` in `webauthn.js` is initialized once per page load.
Aborting it cancels the in-progress Conditional UI request cleanly. This prevents
the browser from holding open a long-running passkey request that could interfere with
the standard form submission.

---

## User Experience Guidelines

### Messaging

| Situation | Recommended Message |
|---|---|
| Passkey autofill shown | *(No message needed — browser handles it)* |
| Browser unsupported | "For faster sign-in, upgrade to a browser that supports passkeys." |
| No passkey on device | "Sign in with your password, then add a passkey for next time." |
| Stale passkey | "That passkey is no longer valid. Please use your password." |
| After successful registration | "Passkey added! You'll be able to sign in faster next time." |
| Security key prompt | "Insert your security key and press its button." |

### Flow Design Principles

1. **Never block**: Always offer a non-passkey alternative. The standard form must
   remain functional regardless of Conditional UI state.
2. **Silent fallback**: Browser-level failures (unsupported, cancelled) should not
   show error banners. The form simply continues to work normally.
3. **Visible escape hatch**: For security key prompts or biometric dialogs, provide
   a clear "Use password instead" option.
4. **Progressive registration**: After a password login, invite users to register a
   passkey — do not require it.
5. **Consistent UI**: The username field should always be visible. Conditional UI
   works alongside it, not in place of it.

---

## Implementation Guide for Relying Parties

### Step 1 — Registration (Writing the allowList Cookie)

When a user registers a new passkey, persist the credential to the `allowList` cookie.
Use the `Fido2ExternalAuthenticator.py` as a reference. The key points:

```python
# After successful attestation:
new_credential = attestationResponse.get("credential")
# credential contains: { "id": "...", "type": "public-key", "transports": [...] }
self.persistCookie(new_credential)
```

Cookie requirements:
- `Secure=true` (HTTPS only)
- `HttpOnly=true` (no JavaScript access)
- `SameSite=Strict` or `Lax` (CSRF protection)
- 7-day expiry is a reasonable default; adjust based on your security policy

### Step 2 — Authentication (Starting Conditional UI)

On the login page:

1. Add `autocomplete="username webauthn"` to the username input
2. On page load, `await PublicKeyCredential.isConditionalMediationAvailable()` — this is
   a static async method (WebAuthn Level 3); it must be called and awaited, not just checked
   for existence
3. If supported, call `webauthn.getAssertionConditional(assertionRequest)` with
   `mediation: "conditional"` and an `AbortController` signal
4. Handle the promise result by submitting `tokenResponse` and `authMethod` to the server

### Step 3 — Server-Side Verification

The FIDO server verifies the authenticator response and returns the `username` associated
with the credential. Log the user in using that username — do not rely on any
client-provided username for Conditional UI flows.

### Step 4 — Configure assertionRequest Correctly

For Conditional UI, the assertion request:
- **Must** set `rpId` to your domain
- **Should** set `allowCredentials` from the `allowList` cookie (can be empty)
- **Should** set `userVerification` to `"preferred"` or `"required"`
- **Must not** set a `username` (Conditional UI is usernameless)

For standard FIDO authentication (step 2 of the non-Conditional UI flow):
- **Must** set `username` to identify which credentials to allow
- **Must** set `rpId`

---

## Troubleshooting

### Passkey Autofill Does Not Appear

| Check | Action |
|---|---|
| Browser version | Verify Chrome 108+, Safari 16+, Edge 108+, or Firefox 122+ |
| `autocomplete` attribute | Confirm `autocomplete="username webauthn"` on the input |
| `allowList` cookie | Open DevTools → Application → Cookies; verify `allowList` exists |
| `assertion_request` | Check the page source; `fido2_assertion_request` must not be `null` |
| HTTPS | Conditional UI only works on HTTPS (or `localhost`) |
| `isConditionalMediationAvailable` | Run `await PublicKeyCredential.isConditionalMediationAvailable()` in console — must resolve to `true` |

### Authentication Fails After Passkey Selection

| Check | Action |
|---|---|
| Credential exists server-side | Verify via FIDO admin tools or logs |
| Cookie is current | Old cookie entries cause `InvalidStateError`; clear and re-register |
| RP ID mismatch | `rpId` in assertion request must match the registered domain exactly |
| Clock skew | Server clock skew > 5 minutes causes challenge validation failure |
| Challenge reuse | Each assertion options call generates a fresh challenge — do not cache |

### Standard Login Form Broken After Conditional UI Abort

The `AbortController` is a global singleton per page load. If it has already been
aborted, a new `credentials.get()` call using the same signal will reject immediately.

**Resolution**: After aborting, do not attempt to restart Conditional UI in the same
page session. The standard form submit path does not use the `AbortController` and will
work normally.

### `token_response` Is Null on the Server

This means the `fido2_form` was not submitted via the Conditional UI path. Check:
1. That `document.getElementById('tokenResponse').value` is being set before submit
2. That the form action points to the correct `postlogin.htm` endpoint
3. Browser console for JavaScript errors in `startAssertion()`

### High Fallback Rate in Metrics

If the Fido2 metrics service reports a high fallback rate, investigate:
1. Browser compatibility — are users on unsupported browsers?
2. Cookie persistence — are cookies being blocked or cleared?
3. Registration success rate — are enrollments completing successfully?
4. Server-side errors in FIDO2 logs (see [Logs guide](logs.md))

---

## Related Documentation

- [Passwordless Login Experience](passwordlessLoginExperience.md) — sequence diagrams
  and quick-start guide for usernameless login
- [FIDO2 Configuration](config.md) — server-side configuration parameters including
  attestation mode, hints, and algorithm support
- [Vendor Metadata](vendor-metadata.md) — FIDO MDS3 integration and attestation
  validation
- [Types of Credentials](types-of-creds.md) — authenticator hints and credential types
- [FIDO Logs](logs.md) — logging configuration for FIDO2 server diagnostics
- [Fido2ExternalAuthenticator.py](../../../script-catalog/person_authentication/fido2-external-authenticator/Fido2ExternalAuthenticator.py) — reference implementation
- [passkeys.dev Device Support](https://passkeys.dev/device-support/) — live browser and
  OS compatibility matrix
