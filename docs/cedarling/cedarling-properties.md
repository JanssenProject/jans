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
* **`CEDARLING_POLICY_STORE_ID`** : A unique identifier for the policy store.
* **`CEDARLING_LOG_TYPE`** (`"off"` | `"memory"` | `"std_out"` | `"lock"`): Selects the logging method.
* **`CEDARLING_DECISION_LOG_USER_CLAIMS`** : List of claims to map from user entity, such as ["sub", "email", "username", ...]
* **`CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS`** : List of claims to map from user entity, such as ["client_id", "rp_id", ...]
* **`CEDARLING_DECISION_LOG_DEFAULT_JWT_ID`** : Token claims that will be used for decision logging. Default is "jti", but perhaps some other claim is needed.
* **`CEDARLING_LOG_TTL`** : If the `CEDARLING_LOG_TYPE` is set to `"memory"`, this will set the TTL (Time to Live) of log entities in seconds. Defaults to 60s.
* **`CEDARLING_USER_AUTHZ`** (`"enabled"` | `"disabled"`): When `enabled`, Cedar engine authorization is queried for a *User* principal.
* **`CEDARLING_WORKLOAD_AUTHZ`** (`"enabled"` | `"disabled"`): When `enabled`, Cedar engine authorization is queried for a *Workload* principal.
* **`CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION`** (`"AND"` | `"OR"`): Specifies what boolean operation to use for the `USER` and `WORKLOAD` when making authz (authorization) decisions. See [User-Workload Boolean Operation](#user-workload-boolean-operation)
* **`CEDARLING_LOCAL_JWKS`** : Path to a JWKS file with public keys.
* **`CEDARLING_LOCAL_POLICY_STORE`** : JSON object containing a [Policy Store](./cedarling-policy-store.md).
* **`CEDARLING_POLICY_STORE_LOCAL_FN`** : Path to a JSON file containing a [Policy Store](./cedarling-policy-store.md).
* **`CEDARLING_JWT_SIG_VALIDATION`** (`"enabled"` | `"disabled"`): Whether to enable the signature validation for Json Web Tokens (JWT). This requires an `iss` (Issuer) claim is present in the token.
* **`CEDARLING_JWT_STATUS_VALIDATION`** (`"enabled"` | `"disabled"`): Whether to check the status of JWTs. This requires an `iss` (Issuer) claim is present in the token.
* **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`**: Cedarling will only accept tokens signed with these algorithms.
* **`CEDARLING_AT_ISS_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `iss` (Issuer) claim must be present in the Access Token and the scheme must be `https`.
* **`CEDARLING_AT_JTI_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `jti` (JWT ID) claim must be present in the Access Token.
* **`CEDARLING_AT_NBF_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `nbf` (Not Before) claim must be present in the Access Token and Cedarling will verify that the current date is after the `nbf`.
* **`CEDARLING_AT_EXP_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `exp` (Expiration) claim must be present in the Access Token and not past the date specified.
* **`CEDARLING_IDT_ISS_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `iss` (Issuer) claim must be present in the ID Token and the scheme must be `https`.
* **`CEDARLING_IDT_SUB_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `sub` (Subject) claim must be present in the ID Token.
* **`CEDARLING_IDT_EXP_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `exp` (Expiration) claim must be present in the ID Token and not past the date specified.
* **`CEDARLING_IDT_IAT_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `iat` (Issued at) claim must be present in the ID Token.
* **`CEDARLING_IDT_AUD_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `aud` ( Audience) claim must be present in the ID Token.
* **`CEDARLING_USERINFO_ISS_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `iss` (Issuer) claim must be present in the Userinfo Token and the scheme must be https.
* **`CEDARLING_USERINFO_SUB_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `sub` (Subject) claim must be present in the Userinfo Token.
* **`CEDARLING_USERINFO_AUD_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `aud` (Audience) claim must be present in the Userinfo Token.
* **`CEDARLING_USERINFO_EXP_VALIDATION`** (`"enabled"` | `"disabled"`): When enabled, the `exp` (Expiration) claim must be present in the Userinfo Token and not past the date specified.
* **`CEDARLING_ID_TOKEN_TRUST_MODE`** (`"None"` | `"Strict"`): Varying levels of validations for JWTs. See [Validation Levels](#id-token-trust-mode).
* **`CEDARLING_LOCK`** (`"enabled"` | `"disabled"`): When enabled, the Cedarling will connect to the Lock Master for policies, and subscribe for SSE events.
* **`CEDARLING_LOCK_MASTER_CONFIGURATION_URI`**: URI where Cedarling can get JSON file with all required metadata about Lock Master, i.e. .well-known/lock-master-configuration. This is ***Required if `"LOCK"` == `"enabled"`***.
* **`CEDARLING_DYNAMIC_CONFIGURATION`** (`"enabled"` | `"disabled"`): Controls whether Cedarling should listen for SSE config updates.
* **`CEDARLING_LOCK_SSA_JWT`**: SSA for DCR in a Lock Master deployment. The Cedarling will validate this JWT prior to DCR.
* **`CEDARLING_AUDIT_LOG_INTERVAL`**: How often to send log messages to Lock Master in seconds (set this to `0` to turn off trasmission).
* **`CEDARLING_AUDIT_HEALTH_INTERVAL`**: How often to send health messages to Lock Master in seconds (set this to `0` to turn off trasmission).
* **`CEDARLING_AUDIT_TELEMETRY_INTERVAL`**: How often to send telemetry messages to Lock Master in seconds (set this to `0` to turn off trasmission).
* **`CEDARLING_LISTEN_SSE`** (`"enabled"` | `"disabled"`): Controls whether Cedarling should listen for updates from the Lock Server.

## User-Workload Boolean Operation

The `CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION` property specifies what boolean operation to use for the `USER` and `WORKLOAD` when making authz (authorization) decisions.

### Available Operations

* **AND**: authz will be successful if `USER` **AND** `WORKLOAD` is valid.
* **OR**: authz will be successful if `USER` **OR** `WORKLOAD` is valid.

## ID Token Trust Mode

The level of validation for the ID Token JWT can be set to either `None` or `Strict`.

### `None` Mode

Setting the validation level to `None` will not check for the conditions outlined in [Strict Mode](#strict-mode).

### `Strict` Mode

Strict mode requires:

1. The `id_token`'s `aud` matches the `access_token`'s `client_id`;
2. if a Userinfo token is present, the `sub` matches the `id_token`, and that the `aud` matches the access token's `client_id`.

## Loading The bootstrap config

There are multiple ways to load your bootstrap config:

* [From a JSON file](#loading-from-json)
* [From a YAML file](#loading-from-yaml)

You can load from both file types using the following code snippet:

```rust
use cedarling::BootstrapConfig;

let config =
    BootstrapConfig::load_from_file("./path/to/your/config.json").unwrap();
```

### Loading From JSON

Below is an example of a bootstrap config in JSON format.

```json
{
    "CEDARLING_APPLICATION_NAME": "My App",
    "CEDARLING_POLICY_STORE_URI": "",
    "CEDARLING_POLICY_STORE_ID": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
    "CEDARLING_LOG_TYPE": "memory",
    "CEDARLING_DECISION_LOG_USER_CLAIMS": ["sub", "email", "username"],
    "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS": ["client_id", "rp_id"],
    "CEDARLING_DECISION_LOG_DEFAULT_JWT_ID": "jti",
    "CEDARLING_LOG_TTL": 60,
    "CEDARLING_USER_AUTHZ": "enabled",
    "CEDARLING_WORKLOAD_AUTHZ": "enabled",
    "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION": "AND",
    "CEDARLING_LOCAL_JWKS": "../test_files/local_jwks.json",
    "CEDARLING_LOCAL_POLICY_STORE": null,
    "CEDARLING_POLICY_STORE_LOCAL_FN": "../test_files/policy-store_blobby.json",
    "CEDARLING_JWT_SIG_VALIDATION": "enabled",
    "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
    "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
        "HS256",
        "RS256"
    ],
    "CEDARLING_AT_ISS_VALIDATION": "disabled",
    "CEDARLING_AT_JTI_VALIDATION": "disabled",
    "CEDARLING_AT_NBF_VALIDATION": "disabled",
    "CEDARLING_AT_EXP_VALIDATION": "enabled",
    "CEDARLING_IDT_ISS_VALIDATION": "enabled",
    "CEDARLING_IDT_SUB_VALIDATION": "enabled",
    "CEDARLING_IDT_EXP_VALIDATION": "enabled",
    "CEDARLING_IDT_IAT_VALIDATION": "enabled",
    "CEDARLING_IDT_AUD_VALIDATION": "enabled",
    "CEDARLING_USERINFO_ISS_VALIDATION": "enabled",
    "CEDARLING_USERINFO_SUB_VALIDATION": "enabled",
    "CEDARLING_USERINFO_AUD_VALIDATION": "enabled",
    "CEDARLING_USERINFO_EXP_VALIDATION": "enabled",
    "CEDARLING_ID_TOKEN_TRUST_MODE": "Strict",
    "CEDARLING_LOCK": "disabled",
    "CEDARLING_LOCK_MASTER_CONFIGURATION_URI": null,
    "CEDARLING_DYNAMIC_CONFIGURATION": "disabled",
    "CEDARLING_LOCK_SSA_JWT": null,
    "CEDARLING_AUDIT_HEALTH_INTERVAL": 0,
    "CEDARLING_AUDIT_TELEMETRY_INTERVAL": 0,
    "CEDARLING_LISTEN_SSE": "disabled"
}
```

* Note that properties set to `"disabled"`, an empty string `""`, zero `0`, and `null` can be ommited since they are the defaults.

#### Local JWKS

A local JWKS can be used by setting the `CEDARLING_LOCAL_JWKS` bootstrap property to a path to a local JSON file. When providing a local Json Web Key Store (JWKS), the file must follow the following schema:

```json
{
    "trusted_issuer_id": [ ... ]
    "another_trusted_issuer_id": [ ... ]
}
```

* Where keys are `Trusted Issuer IDs` assigned to each key store
* and the values contains the JSON Web Keys as defined in [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517).
* The `trusted_issuers_id` is used to tag a JWKS with a unique identifier and enables using multiple key stores.

### Loading From YAML

Below is an example of a bootstrap config in YAML format.

```yaml
CEDARLING_APPLICATION_NAME: My App
CEDARLING_POLICY_STORE_URI: ''
CEDARLING_POLICY_STORE_ID: '840da5d85403f35ea76519ed1a18a33989f855bf1cf8'
CEDARLING_LOG_TYPE: 'memory'
CEDARLING_LOG_TTL: 60
CEDARLING_USER_AUTHZ: 'enabled'
CEDARLING_WORKLOAD_AUTHZ: 'enabled'
CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION: 'AND'
CEDARLING_LOCAL_JWKS: '../test_files/local_jwks.json'
CEDARLING_LOCAL_POLICY_STORE: null
CEDARLING_POLICY_STORE_LOCAL_FN: '../test_files/policy-store_blobby.json'
CEDARLING_JWT_SIG_VALIDATION: 'enabled'
CEDARLING_JWT_STATUS_VALIDATION: 'disabled'
CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED:
    - 'HS256'
    - 'RS256'
CEDARLING_AT_ISS_VALIDATION: 'disabled'
CEDARLING_AT_JTI_VALIDATION: 'disabled'
CEDARLING_AT_NBF_VALIDATION: 'disabled'
CEDARLING_AT_EXP_VALIDATION: 'enabled'
CEDARLING_IDT_ISS_VALIDATION: 'enabled'
CEDARLING_IDT_SUB_VALIDATION: 'enabled'
CEDARLING_IDT_EXP_VALIDATION: 'enabled'
CEDARLING_IDT_IAT_VALIDATION: 'enabled'
CEDARLING_IDT_AUD_VALIDATION: 'enabled'
CEDARLING_USERINFO_ISS_VALIDATION: 'enabled'
CEDARLING_USERINFO_SUB_VALIDATION: 'enabled'
CEDARLING_USERINFO_AUD_VALIDATION: 'enabled'
CEDARLING_USERINFO_EXP_VALIDATION: 'enabled'
CEDARLING_ID_TOKEN_TRUST_MODE: 'Strict'
CEDARLING_LOCK: 'disabled'
CEDARLING_LOCK_MASTER_CONFIGURATION_URI: null
CEDARLING_DYNAMIC_CONFIGURATION: 'disabled'
CEDARLING_LOCK_SSA_JWT: 0
CEDARLING_AUDIT_HEALTH_INTERVAL: 0
CEDARLING_AUDIT_TELEMETRY_INTERVAL: 0
CEDARLING_LISTEN_SSE: 'disabled'
```

* Note that properties set to `'disabled'`, an empty string `''`, zero `0`, and `null` can be ommited since they are the defaults.
