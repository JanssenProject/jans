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
* **`CEDARLING_POLICY_STORE_URI`** : Location of policy store JSON, used if policy store is not local, or retreived from Lock Master.
* **`CEDARLING_POLICY_STORE_ID`** : The identifier of the policy store in case there is more then one policy_store_id in the policy store.
* **`CEDARLING_USER_AUTHZ`** : When `enabled`, Cedar engine authorization is queried for a User principal.
* **`CEDARLING_WORKLOAD_AUTHZ`** : When `enabled`, Cedar engine authorization is queried for a Workload principal.
* **`CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION`** :  `AND`, `OR`
* **`CEDARLING_MAPPING_USER`** : Name of Cedar User schema entity
* **`CEDARLING_MAPPING_WORKLOAD`** : Name of Cedar Workload schema entity
* **`CEDARLING_MAPPING_ID_TOKEN`** : Name of Cedar id_token schema entity
* **`CEDARLING_MAPPING_ACCESS_TOKEN`** : Name of Cedar access_token schema entity
* **`CEDARLING_MAPPING_USERINFO_TOKEN`** : Name of Cedar userinfo schema entity
* **`CEDARLING_MAPPING_CONTEXT`** : Name of Cedar Context schema entity

**The following bootstrap properties are needed to configure log behavior:**

* **`CEDARLING_LOG_STORAGE`** : `off`, `memory`, `std_out`
* **`CEDARLING_LOG_LEVEL`** : System Log Level [See here](./cedarling-logs.md). Default to `WARN`
* **`CEDARLING_LOG_STDOUT_TYPE`** : Either `System`, `Metric`, or `Decision`. Default to System. 
* **`CEDARLING_DECISION_LOG_USER_CLAIMS`** : List of claims to map from user entity, such as ["sub", "email", "username", ...]
* **`CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS`** : List of claims to map from user entity, such as ["client_id", "rp_id", ...]
* **`CEDARLING_DECISION_LOG_DEFAULT_JWT_ID`** : Default is `jti`, but perhaps some other claim is needed. 
* **`CEDARLING_LOG_TTL`** : in case of `memory` store, TTL (time to live) of log entities in seconds.

**The following bootstrap properties are needed to configure JWT and cryptographic behavior:**

* **`CEDARLING_LOCAL_JWKS`** : JWKS file with public keys
* **`CEDARLING_LOCAL_POLICY_STORE`** : JSON object with policy store
* **`CEDARLING_POLICY_STORE_LOCAL_FN`** : Local file with JSON object with policy store
* **`CEDARLING_JWT_SIG_VALIDATION`** : `Enabled` | `Disabled` -- Whether to check the signature  of all JWT tokens. This requires an `iss` is present.
* **`CEDARLING_JWT_STATUS_VALIDATION`** : `Enabled` | `Disabled` -- Whether to check the status of the JWT. On startup, the Cedarling should fetch and retreive the latest Status List JWT from the `.well-known/openid-configuration` via the `status_list_endpoint` claim and cache it. See the [IETF Draft](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) for more info. 
* **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`** : Only tokens signed with these algorithms are acceptable to the Cedarling.
* **`CEDARLING_AT_ISS_VALIDATION`** : When enabled, the `iss` claim must be present in access token and the scheme must be `https`.
* **`CEDARLING_AT_JTI_VALIDATION`** : When enabled, the `jti` claim must be present in access token.
* **`CEDARLING_AT_NBF_VALIDATION`** : When enabled, the `nbf` claim must be present in access token and the Cedarling should verify that the current date is after the `nbf`. 
* **`CEDARLING_AT_EXP_VALIDATION`** : When enabled, the `exp` claim must be present and not past the date specified.
* **`CEDARLING_IDT_ISS_VALIDATION`** : When enabled, the `iss` claim must be present in id_token and the scheme must be `https`.
* **`CEDARLING_IDT_SUB_VALIDATION`** : When enabled, the `sub` claim must be present in id_token.
* **`CEDARLING_IDT_EXP_VALIDATION`** : When enabled, the `exp` claim must be present and not past the date specified.
* **`CEDARLING_IDT_IAT_VALIDATION`** : When enabled, the `iat` claim must be present in id_token.
* **`CEDARLING_IDT_AUD_VALIDATION`** : When enabled, the `aud` claim must be present in id_token.
* **`CEDARLING_USERINFO_ISS_VALIDATION`** : When enabled, the `iss` claim must be present and the scheme must be `https`.
* **`CEDARLING_USERINFO_SUB_VALIDATION`** : When enabled, the `sub` claim must be present in Userinfo JWT.
* **`CEDARLING_USERINFO_AUD_VALIDATION`** : When enabled, the `aud` claim must be present in Userinfo JWT.
* **`CEDARLING_USERINFO_EXP_VALIDATION`** : When enabled, the `exp` claim must be present and not past the date specified.
* **`CEDARLING_ID_TOKEN_TRUST_MODE`** :  `Strict` | `None`. Varying levels of validations based on the preference of the developer. 
`Strict` mode requires (1) id_token `aud` matches the access_token `client_id`; (2) if a Userinfo token is present, the `sub` matches the id_token, and that the `aud` matches the access token client_id.

**The following bootstrap properties are only needed for enterprise deployments.**

* **`CEDARLING_LOCK`** : Enabled | Disabled. If Enabled, the Cedarling will connect to the Lock Master for policies, and subscribe for SSE events. 
* **`CEDARLING_LOCK_MASTER_CONFIGURATION_URI`** : Required if `LOCK` == `Enabled`. URI where Cedarling can get JSON file with all required metadata about Lock Master, i.e. `.well-known/lock-master-configuration`.
* **`CEDARLING_LOCK_DYNAMIC_CONFIGURATION`** : Enabled | Disabled, controls whether Cedarling should listen for SSE config updates.
* **`CEDARLING_LOCK_SSA_JWT`** : SSA for DCR in a Lock Master deployment. The Cedarling will validate this SSA JWT prior to DCR.
* **`CEDARLING_LOCK_LOG_INTERVAL`** : How often to send log messages to Lock Master (0 to turn off trasmission).
* **`CEDARLING_LOCK_HEALTH_INTERVAL`** : How often to send health messages to Lock Master (0 to turn off transmission).
* **`CEDARLING_LOCK_TELEMETRY_INTERVAL`** : How often to send telemetry messages to Lock Master (0 to turn off transmission).
* **`CEDARLING_LOCK_LISTEN_SSE`** :  Enabled | Disabled: controls whether Cedarling should listen for updates from the Lock Server.

## User-Workload Boolean Operation

The `CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION` property specifies what boolean operation to use for the `USER` and `WORKLOAD` when making authz (authorization) decisions.

### Available Operations

- **AND**: authz will be successful if `USER` **AND** `WORKLOAD` is valid.
- **OR**: authz will be successful if `USER` **OR** `WORKLOAD` is valid.

## ID Token Trust Mode

The level of validation for the ID Token JWT can be set to either `None` or `Strict`.

### `None` Mode

Setting the validation level to `None` will not check for the conditions outlined in [Strict Mode](#strict-mode).

### `Strict` Mode

Strict mode requires:

1. The `id_token`'s `aud` matches the `access_token`'s `client_id`;
2. if a Userinfo token is present, the `sub` matches the `id_token`, and that the `aud` matches the access token's `client_id`.

## Local JWKS

A local JWKS can be used by setting the `CEDARLING_LOCAL_JWKS` bootstrap property to a path to a local JSON file. When providing a local Json Web Key Store (JWKS), the file must follow the following schema:

```json
{
    "trusted_issuer_id": [ ... ]
    "another_trusted_issuer_id": [ ... ]
}
```

- Where keys are `Trusted Issuer IDs` assigned to each key store
- and the values contains the JSON Web Keys as defined in [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517).
- The `trusted_issuers_id` is used to tag a JWKS with a unique identifier and enables using multiple key stores.


## Loading the bootstrap config

There are multiple ways to load your bootstrap config:

- [From a JSON file](#loading-from-json)
- [From a YAML file](#loading-from-yaml)

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

- Note that properties set to `"disabled"`, an empty string `""`, zero `0`, and `null` can be ommited since they are the defaults.

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

- Note that properties set to `'disabled'`, an empty string `''`, zero `0`, and `null` can be ommited since they are the defaults.
