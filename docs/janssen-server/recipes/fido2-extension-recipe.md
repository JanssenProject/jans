---
tags:
  - administration
  - developer
  - recipe
  - fido2
  - passkeys
---

# Enforce a Passkey Policy with a FIDO2 Extension Script

This recipe shows how to apply **custom policy to passkey registration and
authentication** using a FIDO2 Extension script — for example, blocking specific users,
requiring a value on the request, or rejecting requests that violate an organizational
rule. The script runs **inside the FIDO2 server**, at the moment an
attestation/assertion endpoint is called, so the policy is enforced for every relying
party that uses the server.

!!! note "Two different FIDO2 scripts — don't mix them up"
    - **FIDO2 Extension script** (`fido2_extension`, this recipe): runs inside the FIDO2
      server and intercepts the attestation/assertion **API calls** to validate, modify,
      or reject them. Policy at the API level.
    - **[FIDO2 External Authenticator script](../../script-catalog/person_authentication/fido2-external-authenticator/README.md)**
      (`person_authentication`): runs in the auth server and drives the **login flow/UI**
      (the ACR).

    This recipe is about the first one. For the full interface reference, see the
    [Fido2 Extension script catalog page](../../script-catalog/fido2_extension/fido2-extension.md).

## When to use this

The extension script hooks into the four FIDO2 operations — each with a *start* and
*finish* hook (8 in total) — where you can validate the request, modify the incoming
parameters, or reject the call with a `BadRequest`:

| Operation | Endpoint | Hooks |
|---|---|---|
| Register (attestation) | `attestation/options`, `attestation/result` | `registerAttestationStart` / `Finish`, `verifyAttestationStart` / `Finish` |
| Authenticate (assertion) | `assertion/options`, `assertion/result` | `authenticateAssertionStart` / `Finish`, `verifyAssertionStart` / `Finish` |

Typical policies:

- **Block or allow specific users** from registering or authenticating a passkey.
- **Require an attribute** on the request (e.g. reject an assertion with a missing username).
- **Gate registration** behind an external check (risk engine, entitlement service).
- **Adjust request parameters** before the server processes them.

## The recipe: block disallowed users

The goal: reject any passkey authentication for a user on a configurable deny-list, and
reject requests that omit the username. The deny-list is supplied as a **script
configuration property** so it can be changed without editing code.

### Step 1 — Write the script

Save the following as `/opt/jans/fido2-policy-extension.py`. It builds on the shipped
[`Fido2ExtensionSample.py`](https://github.com/JanssenProject/jans/blob/main/docs/script-catalog/fido2_extension/Fido2ExtensionSample.py),
reading the deny-list from a configuration property named `blocked_users`.

```python
# Copyright (c) 2026, Janssen Project — Apache 2.0 License
from io.jans.service.cdi.util import CdiUtil
from io.jans.model.custom.script.type.fido2 import Fido2ExtensionType
from io.jans.util import StringHelper
from org.apache.logging.log4j import ThreadContext
from io.jans.fido2.model.u2f.error import Fido2ErrorResponseFactory
from io.jans.fido2.model.u2f.error import Fido2ErrorResponseType
from io.jans.as.model.config import Constants


class Fido2Extension(Fido2ExtensionType):

    def __init__(self, currentTimeMillis):
        self.currentTimeMillis = currentTimeMillis

    def init(self, customScript, configurationAttributes):
        # Read a comma-separated deny-list from the script's configuration property.
        self.blockedUsers = []
        if configurationAttributes.containsKey("blocked_users"):
            raw = configurationAttributes.get("blocked_users").getValue2()
            self.blockedUsers = [u.strip() for u in raw.split(",") if u.strip()]
        # Log only a count — never the deny-list itself, which contains user identifiers.
        print "Fido2Extension. Initialized. Deny-list entries: %s" % len(self.blockedUsers)
        return True

    def destroy(self, configurationAttributes):
        print "Fido2Extension. Destroyed"
        return True

    def getApiVersion(self):
        return 11

    def throwBadRequestException(self, title, message, context):
        errorClaimException = Fido2ErrorResponseFactory.createBadRequestException(
            Fido2ErrorResponseType.BAD_REQUEST_INTERCEPTION, title, message,
            ThreadContext.get(Constants.CORRELATION_ID_HEADER))
        context.setWebApplicationException(errorClaimException)

    # Returns the trimmed username, or None when absent/blank (usernameless flow).
    def getUsername(self, paramAsJsonNode):
        if not paramAsJsonNode.hasNonNull("username"):
            return None
        value = paramAsJsonNode.get("username").asText()
        if value is None or value.strip() == "":
            return None
        return value.strip()

    # ---- Authentication: enforce the deny-list before the assertion starts ----
    def authenticateAssertionStart(self, paramAsJsonNode, context):
        username = self.getUsername(paramAsJsonNode)
        if username is None:
            # Usernameless / discoverable-credential flow: no username is supplied at this
            # stage, so the deny-list cannot be evaluated here. Let the request proceed and
            # enforce policy once the user is resolved (see verifyAssertionFinish).
            return True

        if username in self.blockedUsers:
            # Do not log the username — it would land in centralized logs.
            print "Fido2Extension. Blocking authentication (deny-list match)"
            self.throwBadRequestException(
                "Fido2Extension: user blocked",
                "Authentication is not allowed for this user.", context)
        return True

    def authenticateAssertionFinish(self, paramAsJsonNode, context):
        return True

    # ---- Registration: also keep blocked users from enrolling new passkeys ----
    def registerAttestationStart(self, paramAsJsonNode, context):
        username = self.getUsername(paramAsJsonNode)
        if username is not None and username in self.blockedUsers:
            self.throwBadRequestException(
                "Fido2Extension: user blocked",
                "Passkey registration is not allowed for this user.", context)
        return True

    def registerAttestationFinish(self, paramAsJsonNode, context):
        return True

    def verifyAttestationStart(self, paramAsJsonNode, context):
        return True

    def verifyAttestationFinish(self, paramAsJsonNode, context):
        return True

    def verifyAssertionStart(self, paramAsJsonNode, context):
        return True

    def verifyAssertionFinish(self, paramAsJsonNode, context):
        return True
```

!!! tip "Modifying, not just rejecting"
    A `*Start` hook receives `paramAsJsonNode` — the incoming request parameters. Besides
    rejecting with `throwBadRequestException`, you can mutate this JSON node to change the
    request before the server processes it (for example, forcing a value). Returning
    without setting an exception lets the operation proceed.

### Step 2 — Register and enable the script

The script is a custom script of type **`fido2_extension`**. Register it once, attach the
`blocked_users` property, and enable it.

=== "Text-based UI (TUI)"

    1. Start the TUI: `jans tui`.
    2. Go to **Scripts** → **Add** (or edit an existing one).
    3. Set **Script Type** to `fido2_extension`, give it a name (e.g. `fido2_policy`), and
       paste the script from Step 1.
    4. Under **Custom properties**, add `blocked_users` with a value such as
       `test_user,contractor1`.
    5. Toggle **Enabled** on and save.

=== "Command line (jans CLI)"

    1. Fetch the current FIDO2 extension scripts to use as a template:

       ```bash
       jans cli --operation-id get-config-scripts-by-type --url-suffix type:FIDO2_EXTENSION
       ```

    2. Prepare a script JSON (`/tmp/fido2_policy.json`) with `scriptType` set to
       `fido2_extension`, `enabled` set to `true`, the script body in `script`, and the
       deny-list under `configurationProperties`:

       ```json
       {
         "name": "fido2_policy",
         "scriptType": "fido2_extension",
         "programmingLanguage": "python",
         "location_type": "db",
         "enabled": true,
         "configurationProperties": [
           { "value1": "blocked_users", "value2": "test_user,contractor1", "hide": false }
         ],
         "script": "<contents of the Python file>"
       }
       ```

    3. Create it:

       ```bash
       jans cli --operation-id post-config-scripts --data /tmp/fido2_policy.json
       ```

       To update an existing script, use `put-config-scripts` with the script's `inum` in
       the payload.

### Step 3 — Verify

With the script enabled, call the assertion options endpoint for a blocked user. The
FIDO2 server invokes `authenticateAssertionStart`, which rejects the request:

```bash
curl -X POST "https://<your-jans-server>/jans-fido2/restv1/assertion/options" \
  -H "Content-Type: application/json" \
  -d '{"username": "test_user"}'
```

Expected result: an HTTP **400 Bad Request** carrying the title/description from the
script. A username **not** on the deny-list returns normal
`PublicKeyCredentialRequestOptions`, and a request with no `username` (usernameless flow)
is allowed through. You can confirm the hook ran by tailing the FIDO2 server log for the
`Fido2Extension. Blocking authentication (deny-list match)` line (see
[FIDO Logs](../fido/logs.md)).

## Notes and gotchas

- **`getApiVersion` must return `11`** so the extra `customScript` argument is passed to
  `init` — matching the interface used by the sample.
- **`*Start` vs `*Finish`.** Reject early in a `*Start` hook (before the server does its
  work). `*Finish` hooks run after and are better for logging or post-processing.
- **The username is not always present.** On usernameless/discoverable-credential flows the
  assertion carries no `username` at `authenticateAssertionStart`. The sample therefore
  **lets those requests through** rather than rejecting them — blocking on a missing
  username would break passkey sign-in entirely. If you must cover usernameless flows,
  enforce the policy in `verifyAssertionFinish`, once the server has resolved the user.
- **Don't log user identifiers.** The sample deliberately logs only a deny-list *count* and
  a "deny-list match" marker. Printing usernames or the deny-list itself pushes user
  identifiers into centralized logs; if you need an audit trail, record it deliberately
  through your audit pipeline instead.
- **Scope.** This runs for every relying party pointed at the FIDO2 server. If you need
  per-application rules, branch on a request attribute (e.g. `applicationType`) inside the
  hook.

## Related documentation

- [Fido2 Extension — script catalog reference](../../script-catalog/fido2_extension/fido2-extension.md) — full interface, objects, and Java sample
- [Passkeys Implementation Guide](passkey-impl-guide.md) — the end-to-end passkey deployment
- [Custom Scripts overview](../developer/scripts/README.md) — how Jans custom scripts work
