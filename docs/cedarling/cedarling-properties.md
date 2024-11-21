---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - properties
---

# Cedarling Properties

These Bootstrap Properties control default application level behavior.

* **`CEDARLING_APPLICATION_NAME`** : Human friendly identifier for this application
* **`CEDARLING_POLICY_STORE_URI`** : Location of policy store JSON, used if policy store is not local, or retreived from Lock Server.
* **`CEDARLING_POLICY_STORE_ID`** (`"off"` | `"memory"`, `"std_out"` | `"lock"`): A unique identifier for the policy store.
* **`CEDARLING_LOG_TYPE`** : How the logs will be represented. Could be 
* **`CEDARLING_LOG_TTL`** : If the `CEDARLING_LOG_TYPE` is set to `"memory"`, this will set the TTL (Time to Live) of log entities in seconds.
* **`CEDARLING_LOCAL_JWKS`** : Path to a JWKS file with public keys.
* **`CEDARLING_LOCAL_POLICY_STORE`** : JSON object containing a [Policy Store](./cedarling-policy-store.md).
* **`CEDARLING_POLICY_STORE_LOCAL_FN`** : Path to a JSON file containing a [Policy Store](./cedarling-policy-store.md).
* **`CEDARLING_JWT_SIG_VALIDATION`** (`"Enabled"` | `"Disabled"`): Whether to enable the signature validation for Json Web Tokens (JWT). This requires an `iss` (Issuer) claim is present in the token.
* **`CEDARLING_JWT_STATUS_VALIDATION`** (`"Enabled"` | `"Disabled"`): Whether to check the status of JWTs. This requires an `iss` (Issuer) claim is present in the token.
* **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`**: Cedarling will only accept tokens signed with these algorithms.
* **`CEDARLING_AT_ISS_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `iss` (Issuer) claim must be present in the Access Token and the scheme must be `https`.
* **`CEDARLING_AT_JTI_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `jti` (JWT ID) claim must be present in the Access Token.
* **`CEDARLING_AT_NBF_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `nbf` (Not Before) claim must be present in the Access Token and Cedarling will verify that the current date is after the `nbf`.
* **`CEDARLING_AT_EXP_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `exp` (Expiration) claim must be present in the Access Token and not past the date specified.
* **`CEDARLING_IDT_ISS_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `iss` (Issuer) claim must be present in the ID Token and the scheme must be `https`.
* **`CEDARLING_IDT_SUB_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `sub` (Subject) claim must be present in the ID Token.
* **`CEDARLING_IDT_EXP_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `exp` (Expiration) claim must be present in the ID Token and not past the date specified.
* **`CEDARLING_IDT_IAT_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `iat` (Issued at) claim must be present in the ID Token.
* **`CEDARLING_IDT_AUD_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `aud` ( Audience) claim must be present in the ID Token.
* **`CEDARLING_USERINFO_ISS_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `iss` (Issuer) claim must be present in the Userinfo Token and the scheme must be https.
* **`CEDARLING_USERINFO_SUB_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `sub` (Subject) claim must be present in the Userinfo Token.
* **`CEDARLING_USERINFO_AUD_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `aud` (Audience) claim must be present in the Userinfo Token.
* **`CEDARLING_USERINFO_EXP_VALIDATION`** (`"Enabled"` | `"Disabled"`): When enabled, the `exp` (Expiration) claim must be present in the Userinfo Token and not past the date specified.
* **`CEDARLING_ID_TOKEN_TRUST_MODE`** (`"None"` | `"Strict"`): Varying levels of validations for JWTs. See [Validation Levels](#id-token-trust-mode).
* **`CEDARLING_LOCK`** (`"Enabled"` | `"Disabled"`): When Enabled, the Cedarling will connect to the Lock Master for policies, and subscribe for SSE events.
* **`CEDARLING_LOCK_MASTER_CONFIGURATION_URI`**: URI where Cedarling can get JSON file with all required metadata about Lock Master, i.e. .well-known/lock-master-configuration. This is ***Required if `"LOCK"` == `"Enabled"`***.
* **`CEDARLING_DYNAMIC_CONFIGURATION`** (`"Enabled"` | `"Disabled"`): Controls whether Cedarling should listen for SSE config updates.
* **`CEDARLING_LOCK_SSA_JWT`**: SSA for DCR in a Lock Master deployment. The Cedarling will validate this JWT prior to DCR.
* **`CEDARLING_AUDIT_LOG_INTERVAL`**: How often to send log messages to Lock Master in seconds (set this to `0` to turn off trasmission).
* **`CEDARLING_AUDIT_HEALTH_INTERVAL`**: How often to send health messages to Lock Master in seconds (set this to `0` to turn off trasmission).
* **`CEDARLING_AUDIT_TELEMETRY_INTERVAL`**: How often to send telemetry messages to Lock Master in seconds (set this to `0` to turn off trasmission).
* **`CEDARLING_LISTEN_SSE`** (`"Enabled"` | `"Disabled"`): Controls whether Cedarling should listen for updates from the Lock Server.

## ID Token Trust Mode

The level of validation for the ID Token JWT can be set to either `None` or `Strict`.

### `None` Mode

Setting the validation level to `None` will not check for the conditions outlined in [Strict Mode](#strict-mode).

### `Strict` Mode

Strict mode requires:

1. The `id_token`'s `aud` matches the `access_token`'s `client_id`;
2. if a Userinfo token is present, the `sub` matches the `id_token`, and that the `aud` matches the access token's `client_id`.
