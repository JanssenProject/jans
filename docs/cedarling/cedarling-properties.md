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
* **`CEDARLING_POLICY_STORE_URI`** : Location of policy store JSON, used if policy store is not local.
* **`CEDARLING_POLICY_STORE_ID`** : The identifier of the policy store in case there is more then one policy_store_id in the policy store.
* **`CEDARLING_USER_AUTHZ`** : When `enabled`, Cedar engine authorization is queried for a User principal.
* **`CEDARLING_WORKLOAD_AUTHZ`** : When `enabled`, Cedar engine authorization is queried for a Workload principal.
* **`CEDARLING_PRINCIPAL_BOOLEAN_OPERATION`** : property specifies what boolean operation to use for the `USER` and `WORKLOAD` when making authz (authorization) decisions. [See here](#user-workload-boolean-operation).
* **`CEDARLING_MAPPING_USER`** : Name of Cedar User schema entity if we don't want to use default. When specified cedarling try build defined entity (from schema) as user instead of default `User` entity defined in `cedar` schema. Works in namespace defined in the policy store.
* **`CEDARLING_MAPPING_WORKLOAD`** : Name of Cedar Workload schema entity
* **`CEDARLING_MAPPING_ROLE`** : Name of Cedar Role schema entity

**The following bootstrap properties are needed to configure log behavior:**

* **`CEDARLING_LOG_TYPE`** : `off`, `memory`, `std_out`
* **`CEDARLING_LOG_LEVEL`** : System Log Level [See here](./cedarling-logs.md). Default to `WARN`
* **`CEDARLING_LOG_STDOUT_TYPE`** : Either `System`, `Metric`, or `Decision`. Default to System.
* **`CEDARLING_DECISION_LOG_USER_CLAIMS`** : List of claims to map from user entity, such as ["sub", "email", "username", ...]
* **`CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS`** : List of claims to map from user entity, such as ["client_id", "rp_id", ...]
* **`CEDARLING_DECISION_LOG_DEFAULT_JWT_ID`** : Token claims that will be used for decision logging. Default is "jti", but perhaps some other claim is needed.
* **`CEDARLING_LOG_TTL`** : in case of `memory` store, TTL (time to live) of log entities in seconds.
* **`CEDARLING_LOG_MAX_ITEMS`** : Maximum number of log entities that can be stored using Memory logger. If used `0` value means no limit. And If missed or None, default value is applied.
* **`CEDARLING_LOG_MAX_ITEM_SIZE`** : Maximum size of a single log entity in bytes using Memory logger. If used `0` value means no limit. And If missed or None, default value is applied.

**The following bootstrap properties are needed to configure JWT and cryptographic behavior:**

* **`CEDARLING_LOCAL_JWKS`** : JWKS file with public keys
* **`CEDARLING_POLICY_STORE_LOCAL`** : JSON object as string with policy store. You can use [this](https://jsontostring.com/) converter.
* **`CEDARLING_POLICY_STORE_LOCAL_FN`** : Local file with JSON object with policy store
* **`CEDARLING_JWT_SIG_VALIDATION`** : `enabled` | `disabled` -- Whether to check the signature  of all JWT tokens. This requires an `iss` is present.
* **`CEDARLING_JWT_STATUS_VALIDATION`** : `enabled` | `disabled` -- Whether to check the status of the JWT. On startup, the Cedarling should fetch and retreive the latest Status List JWT from the `.well-known/openid-configuration` via the `status_list_endpoint` claim and cache it. See the [IETF Draft](https://datatracker.ietf.org/doc/draft-ietf-oauth-status-list/) for more info.
* **`CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED`** : Only tokens signed with these algorithms are acceptable to the Cedarling.
* **`CEDARLING_ID_TOKEN_TRUST_MODE`** :  `Strict` | `None`. Varying levels of validations based on the preference of the developer.
`Strict` mode requires (1) id_token `aud` matches the access_token `client_id`; (2) if a Userinfo token is present, the `sub` matches the id_token, and that the `aud` matches the access token client_id.

**The following bootstrap properties are only needed for enterprise deployments.**

* **`CEDARLING_LOCK`** : `enabled` | `disabled`. If `enabled`, the Cedarling will connect to the Lock Server for policies, and subscribe for SSE events.
* **`CEDARLING_LOCK_SERVER_CONFIGURATION_URI`** : Required if `LOCK` == `enabled`. URI where Cedarling can get JSON file with all required metadata about the Lock Server, i.e. `.well-known/lock-master-configuration`.
* **`CEDARLING_LOCK_DYNAMIC_CONFIGURATION`** : `enabled` | `disabled`, controls whether Cedarling should listen for SSE config updates.
* **`CEDARLING_LOCK_SSA_JWT`** : SSA for DCR in a Lock Server deployment. The Cedarling will validate this SSA JWT prior to DCR.
* **`CEDARLING_LOCK_LOG_INTERVAL`** : How often to send log messages to Lock Server (0 to turn off trasmission).
* **`CEDARLING_LOCK_HEALTH_INTERVAL`** : How often to send health messages to Lock Server (0 to turn off transmission).
* **`CEDARLING_LOCK_TELEMETRY_INTERVAL`** : How often to send telemetry messages to Lock Server (0 to turn off transmission).
* **`CEDARLING_LOCK_LISTEN_SSE`** :  `enabled` | `disabled`: controls whether Cedarling should listen for updates from the Lock Server.

## Required keys for startup

* **`CEDARLING_APPLICATION_NAME`**

To enable usage of principals at least one of the following keys must be provided:

* **`CEDARLING_WORKLOAD_AUTHZ`**
* **`CEDARLING_USER_AUTHZ`**

To load policy store one of the following keys must be provided:

* **`CEDARLING_POLICY_STORE_LOCAL`**
* **`CEDARLING_POLICY_STORE_URI`**
* **`CEDARLING_POLICY_STORE_LOCAL_FN`**

All other fields are optional and can be omitted. If a field is not provided, Cedarling will use the default value specified in the property definition.

## User-Workload Boolean Operation

The `CEDARLING_PRINCIPAL_BOOLEAN_OPERATION` property specifies what boolean operation to use when combining authorization decisions for `USER` and `WORKLOAD` principals. This JSON Logic rule determines the final authorization outcome based on individual principal decisions.

We use [JsonLogic](https://jsonlogic.com/) to define the boolean operation. The rule is evaluated against each principal decision, and the final result is determined based on the specified operation.

### Variables in the jsonlogic rule

Make sure that you use correct `var` name for `principal` types.

When referencing principals in your JSON logic rules, you must use the full Cedar principal type identifier that includes namespace, entity name and optionally the entity ID. This matches exactly how principals are defined in your Cedar policies.

**Correct Format**: `<Namespace>::<EntityType>` or `<Namespace>::<EntityType>::"<EntityID>"`  
Examples:  

* Without ID: `Jans::User`, `Jans::Workload`, `Acme::Service`
* With ID: `Jans::User::"john_doe"`, `Jans::Device::"mobile_1234"`, `Acme::Service::"api_gateway"`.  

*Notice*: Make sure to correctly escape `"` in JSON strings. For example `"Acme::Service::\"api_gateway\""`.

**Why This Matters**

* Matches Cedar's type system requirements
* Ensures proper variable resolution
* Maintains consistency with policy definitions

**Example Configuration**

```
// CORRECT - Full type with namespace
{
    "or": [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}

// INCORRECT - Missing namespace
{
    "or": [
        {"===": [{"var": "Workload"}, "ALLOW"]}, // Will not resolve
        {"===": [{"var": "User"}, "ALLOW"]}      // Will not resolve
    ]
}
```

**Consequences of Incorrect Format**  
❌ Authorization will fail with DENY  
❌ Potential evaluation errors in JSON logic  
❌ Mismatches with actual Cedar policy definitions  

### Default configuration

Default value:

```json
{
    "and" : [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}
```

Explanation:  

* The rule uses and to require both principals to be authorized
* `{"var": "Jans::Workload"}` checks the workload principal's decision
* `{"var": "Jans::User"}` checks the user principal's decision
* `"==="` performs strict equality comparison against "ALLOW"
* both conditions must be true for final authorization to be granted

### Comparison Operators

* === (Recommended): Strict equality check (type and value must match)
* ==: Loose equality check (may cause type coercion errors if variables are missing)

Note: For comparison better to use `===` instead of `==`. To avoid casting result to `Nan` if something goes wrong.

#### Operation Types

##### **AND Operation**

```js
{"and": [condition1, condition2]}
```

* Authorization succeeds only if ALL conditions are true

Example:

```json
{
    "and" : [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}
```

##### **OR Operation**

```js
{"or": [condition1, condition2]}
```

* Authorization succeeds if ANY condition is true

Example:

```json
{
    "or" : [
        {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
        {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
}
```

#### Best Practices

1. Use Strict Comparison (===)
   * Prevents unexpected type conversions
   * Returns DENY instead of errors when variables are missing
1. Explicit Principal References

    ```json
    // Good - explicit principal type
    {"===": [{"var": "Jans::Workload"}, "ALLOW"]}

    // Bad - incorrect principal type
    {"===": [{"var": "Workload"}, "ALLOW"]}
    ```

#### Error Scenarios

* Using == with missing principals:

```json
{"==": [{"var": "MissingPrincipal"}, "ALLOW"]}  // Throws error
```

* Type mismatches:

```json
{"===": [{"var": "Jans::Workload"}, true]}  // Always false
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
  "CEDARLING_USER_AUTHZ": "enabled",
  "CEDARLING_WORKLOAD_AUTHZ": "enabled",
  "CEDARLING_POLICY_STORE_URI": null,
  "CEDARLING_POLICY_STORE_LOCAL": null,
  "CEDARLING_POLICY_STORE_LOCAL_FN": "./example_files/policy-store.json",
  "CEDARLING_POLICY_STORE_ID": "gICAgcHJpbmNpcGFsIGlz",
  "CEDARLING_LOG_TYPE": "std_out",
  "CEDARLING_LOG_LEVEL": "INFO",
  "CEDARLING_LOG_TTL": null,
  "CEDARLING_DECISION_LOG_USER_CLAIMS": [
    "sub",
    "email"
  ],
  "CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS": [
    "client_id",
    "rp_id"
  ],
  "CEDARLING_PRINCIPAL_BOOLEAN_OPERATION": {
    "and" : [
      {"===": [{"var": "Jans::Workload"}, "ALLOW"]},
      {"===": [{"var": "Jans::User"}, "ALLOW"]}
    ]
  },
  "CEDARLING_LOCAL_JWKS": null,
  "CEDARLING_JWT_SIG_VALIDATION": "disabled",
  "CEDARLING_JWT_STATUS_VALIDATION": "disabled",
  "CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED": [
    "HS256",
    "RS256"
  ],
  "CEDARLING_ID_TOKEN_TRUST_MODE": "strict",
  "CEDARLING_LOCK": "disabled",
  "CEDARLING_LOCK_SERVER_CONFIGURATION_URI": null,
  "CEDARLING_LOCK_DYNAMIC_CONFIGURATION": "disabled",
  "CEDARLING_LOCK_HEALTH_INTERVAL": 0,
  "CEDARLING_LOCK_TELEMETRY_INTERVAL": 0,
  "CEDARLING_LOCK_LISTEN_SSE": "disabled"
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
CEDARLING_USER_AUTHZ: enabled
CEDARLING_WORKLOAD_AUTHZ: enabled
CEDARLING_POLICY_STORE_URI: null
CEDARLING_POLICY_STORE_LOCAL: null
CEDARLING_POLICY_STORE_LOCAL_FN: ./example_files/policy-store.json
CEDARLING_POLICY_STORE_ID: gICAgcHJpbmNpcGFsIGlz
CEDARLING_LOG_TYPE: std_out
CEDARLING_LOG_LEVEL: INFO
CEDARLING_LOG_TTL: null
CEDARLING_DECISION_LOG_USER_CLAIMS: ["sub","email"]
CEDARLING_DECISION_LOG_WORKLOAD_CLAIMS: ["client_id", "rp_id"]
CEDARLING_PRINCIPAL_BOOLEAN_OPERATION:
    and:
        - "===":
            - var: "Jans::Workload"
            - "ALLOW"
        - "===":
            - var: "Jans::User"
            - "ALLOW"
CEDARLING_LOCAL_JWKS: null
CEDARLING_JWT_SIG_VALIDATION: disabled
CEDARLING_JWT_STATUS_VALIDATION: disabled
CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED:
    - HS256
    - RS256
CEDARLING_ID_TOKEN_TRUST_MODE: strict
CEDARLING_LOCK: disabled
CEDARLING_LOCK_SERVER_CONFIGURATION_URI: null
CEDARLING_LOCK_DYNAMIC_CONFIGURATION: disabled
CEDARLING_LOCK_HEALTH_INTERVAL: 0
CEDARLING_LOCK_TELEMETRY_INTERVAL: 0
CEDARLING_LOCK_LISTEN_SSE: disabled
```

* Note that properties set to `'disabled'`, an empty string `''`, zero `0`, and `null` can be ommited since they are the defaults.
