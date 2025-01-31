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
* **`CEDARLING_MAPPING_USER`** : Name of Cedar User schema entity if we don't want to use default. When specified cedarling try build defined entity (from schema) as user instead of default `User` entity defined in `cedar` schema. Works in namespace defined in the policy store.
* **`CEDARLING_MAPPING_WORKLOAD`** : Name of Cedar Workload schema entity
* **`CEDARLING_MAPPING_ROLE`** : Name of Cedar Role schema entity

**The following bootstrap properties are needed to configure log behavior:**

* **`CEDARLING_LOG_STORAGE`** : `off`, `memory`, `std_out`
* **`CEDARLING_LOG_LEVEL`** : System Log Level [See here](./cedarling-logs.md). Default to `WARN`
* **`CEDARLING_LOG_STDOUT_TYPE`** : Either `System`, `Metric`, or `Decision`. Default to System.
* **`CEDARLING_LOG_LEVEL`** : Log level filter for logging. Log level has only `System` log type entries. `TRACE` is lowest. `FATAL` is highest. Possible variants:
  * FATAL
  * ERROR
  * WARN
  * INFO
  * DEBUG
  * TRACE
* **`CEDARLING_DECISION_LOG_USER_CLAIMS`** : List of claims to map from user entity, such as ["sub", "email", "username", ...]
* **`CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS`** : List of claims to map from user entity, such as ["client_id", "rp_id", ...]
* **`CEDARLING_DECISION_LOG_DEFAULT_JWT_ID`** : Token claims that will be used for decision logging. Default is "jti", but perhaps some other claim is needed.
* **`CEDARLING_LOG_TTL`** : in case of `memory` store, TTL (time to live) of log entities in seconds.

**The following bootstrap properties are needed to configure JWT and cryptographic behavior:**

* **`CEDARLING_LOCAL_JWKS`** : JWKS file with public keys
* **`CEDARLING_LOCAL_POLICY_STORE`** : JSON object with policy store
* **`CEDARLING_POLICY_STORE_LOCAL_FN`** : Local file with JSON object with policy store
* **`CEDARLING_JWT_SIG_VALIDATION`** : `Enabled` | `Disabled` -- Whether to check the signature  of all JWT tokens. This requires an `iss` is present.
* **`CEDARLING_JWT_STATUS_VALIDATION`** : `Enabled` | `Disabled` -- Whether to check the status of the JWT. On startup, the Cedarling should fetch and retreive the latest Status List JWT from the `.well-known/openid-configuration` via the `status_list_endpoint` claim and cache it. See the [IETF Draft](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) for more info.
* **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`** : Only tokens signed with these algorithms are acceptable to the Cedarling.
* **`CEDARLING_TOKEN_CONFIGS`** : JSON object containing token specific configs. See: [Token Configs](#token-configs).
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

* **AND**: authz will be successful if `USER` **AND** `WORKLOAD` is valid.
* **OR**: authz will be successful if `USER` **OR** `WORKLOAD` is valid.

## Token Configs

The token configs property sets the entity type name of a token and it's validation settings. Below is an example of the `CEDARLING_TOKEN_CONFIGS`:

```js
CEDARLING_TOKEN_CONFIGS = {
  "access_token": {
    "entity_type_name": "Access_token",
    "iss": "enabled",
    "aud": "enabled",
    "sub": "enabled",
    "jti": "enabled",
    "nbf": "enabled",
    "iat": "enabled",
    "exp": "enabled",
  },
  "id_token": {
    "entity_type_name": "id_token",
    "exp": "enabled",
  },
  "userinfo_token": {
    "entity_type_name": "Userinfo_token",
    "exp": "enabled",
  },
  "custom_token1": {
    "entity_type_name": "SomeCustom_token",
    "exp": "enabled",
  },
  "custom_token2": {
    "entity_type_name": "AnotherCustom_token",
    "exp": "enabled",
  },
  // more custom tokens can be added here
}
```

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

* Where keys are `Trusted Issuer IDs` assigned to each key store
* and the values contains the JSON Web Keys as defined in [RFC 7517](https://datatracker.ietf.org/doc/html/rfc7517).
* The `trusted_issuers_id` is used to tag a JWKS with a unique identifier and enables using multiple key stores.

## Loading the bootstrap config

There are multiple ways to load your bootstrap config:

* [From a JSON file](#loading-from-json)
* [From a YAML file](#loading-from-yaml)

You can load from both file types using the following code snippet:

```rust
use cedarling::BootstrapConfig;

let config =
    BootstrapConfig::load_from_file("./path/to/your/config.json").unwrap();

// Load the bootstrap config from the environment variables. Properties that are not defined will be assigned a default value.
let config = BootstrapConfig::from_env().unwrap();

// Load the bootstrap config from the environment variables and a given config if env var is not present.
let config = BootstrapConfig::from_raw_config_and_env(Some(BootstrapConfigRaw { ... })).unwrap();
```

### Loading From JSON

Below is an example of a bootstrap config in JSON format. Not all fields should be specified, almost all have default value.

```json
{
    "CEDARLING_APPLICATION_NAME": "My App",
    "CEDARLING_POLICY_STORE_URI": "",
    "CEDARLING_POLICY_STORE_ID": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
    "CEDARLING_LOG_TYPE": "memory",
    "CEDARLING_LOG_LEVEL": "INFO",
    "CEDARLING_DECISION_LOG_USER_CLAIMS": ["sub", "email", "username"],
    "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS": ["client_id", "rp_id"],
    "CEDARLING_DECISION_LOG_DEFAULT_JWT_ID": "jti",
    "CEDARLING_LOG_TTL": 60,
    "CEDARLING_USER_AUTHZ": "enabled",
    "CEDARLING_WORKLOAD_AUTHZ": "enabled",
    "CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION": "AND",
    "CEDARLING_MAPPING_USER": "CustomUser",
    "CEDARLING_MAPPING_WORKLOAD": "CustomWorkload",
    "CEDARLING_MAPPING_ROLE": "CustomRole",
    "CEDARLING_LOCAL_JWKS": "../test_files/local_jwks.json",
    "CEDARLING_LOCAL_POLICY_STORE": null,
    "CEDARLING_POLICY_STORE_LOCAL_FN": "../test_files/policy-store_blobby.json",
    "CEDARLING_JWT_SIG_VALIDATION": "enabled",
    "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
    "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
        "HS256",
        "RS256"
    ],
    "CEDARLING_TOKEN_CONFIGS": {
        "access_token": {
            "entity_type_name": "Access_token",
            "exp": "enabled",
        },
        "id_token": {
            "entity_type_name": "id_token",
            "iss": "enabled",
            "sub": "enabled",
            "exp": "enabled",
            "iat": "enabled",
            "aud": "enabled",
        },
        "id_token": {
            "entity_type_name": "id_token",
            "iss": "enabled",
            "aud": "enabled",
            "sub": "enabled",
            "exp": "enabled",
        },
    },
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

* Note that properties set to `"disabled"`, an empty string `""`, zero `0`, and `null` can be ommited since they are the defaults.

### Loading From YAML

Below is an example of a bootstrap config in YAML format. Not all fields should be specified, almost all have default value.

```yaml
CEDARLING_APPLICATION_NAME: My App
CEDARLING_POLICY_STORE_URI: ''
CEDARLING_POLICY_STORE_ID: '840da5d85403f35ea76519ed1a18a33989f855bf1cf8'
CEDARLING_LOG_TYPE: 'memory'
CEDARLING_LOG_LEVEL: 'INFO'
CEDARLING_DECISION_LOG_USER_CLAIMS: ["sub","email"]
CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS: ["client_id", "rp_id"]
CEDARLING_LOG_TTL: 60
CEDARLING_USER_AUTHZ: 'enabled'
CEDARLING_WORKLOAD_AUTHZ: 'enabled'
CEDARLING_USER_WORKLOAD_BOOLEAN_OPERATION: 'AND'
CEDARLING_MAPPING_USER: 'CustomUser'
CEDARLING_MAPPING_WORKLOAD: 'CustomWorkload'
CEDARLING_MAPPING_ROLE: 'CustomRole'
CEDARLING_LOCAL_JWKS: '../test_files/local_jwks.json'
CEDARLING_LOCAL_POLICY_STORE: null
CEDARLING_POLICY_STORE_LOCAL_FN: '../test_files/policy-store_blobby.json'
CEDARLING_JWT_SIG_VALIDATION: 'enabled'
CEDARLING_JWT_STATUS_VALIDATION: 'disabled'
CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED:
    - 'HS256'
    - 'RS256'
CEDARLING_TOKENS_CONFIG:
    access_token: CustomAccessToken
    id_token: CustomIdToken
    userinfo_token: CustomUserinfoToken
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
