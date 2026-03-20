---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - logging
  - audit
---


# Cedarling Logs

## Cedarling Audit Logs

There are three different log records produced by the Cedarling:

* `Decision` - The result and diagnostics of an authz decision
* `System` - Startup, debug and other Cedarling messages not related to authz
* `Metric`- Performance and usage data

!!! note "Logging execution model"
All platforms (native, WASM, and tests) perform JSON serialization and writing synchronously on the calling thread. Thread-safety for output streams is provided by Mutex and Send+Sync traits, not background-thread serialization. Regardless of the runtime, the log contents and retrieval APIs remain the same.

The Cedarling has four logging options, which are configurable via the `CEDARLING_LOG_TYPE`
bootstrap property:

* `off` - no logging
* `memory` - logs stored in Cedarling in-memory KV store, fetched by client via logging interface. This
  is ideal for batching logs without impeding authz performance
* `std_out` - write logs synchronously to std_out
* `lock` - periodically POST logs to Jans Lock Server `/audit` endpoint for central archiving.


### System Log Levels

Set with property `CEDARLING_LOG_LEVEL`

* `FATAL`: Indicates very severe error events that will likely lead the application to abort. These are the most critical issues.
* `ERROR`: Designates error events that might still allow the application to continue running but indicate a significant problem.
* `WARN`: Designates potentially harmful situations that should be addressed to prevent future issues.
* `INFO`: Provides informational messages that highlight the progress of the application at a coarse-grained level.
* `DEBUG`: Designates fine-grained informational events useful for debugging the application.
* `TRACE`: Provides finer-grained informational events than DEBUG. It is often used for detailed tracing of program execution.

## Memory Log interface

This interface is used to interact with the memory log storage. It provides methods for getting logs and removing them from the storage. This interface is made available to other languages through bindings.

Tags are used to filter logs. It can be `log_kind` and `log_level` values from log entry data.

You can obtain the `request_id` from the result structure of the `authorize_unsigned` or `authorize_multi_issuer` method call.

```rust
/// Log Storage
/// interface for getting log entries from the storage
pub trait LogStorage {
    /// Return logs and remove them from the storage
    fn pop_logs(&self) -> Vec<serde_json::Value>;

    /// Get specific log entry
    fn get_log_by_id(&self, id: &str) -> Option<serde_json::Value>;

    /// Returns a list of all log ids
    fn get_log_ids(&self) -> Vec<String>;

    /// Get logs by tag, like `log_kind` or `log level`.
    /// Tag can be `log_kind`, `log_level`.
    fn get_logs_by_tag(&self, tag: &str) -> Vec<serde_json::Value>;

    /// Get logs by request_id.
    /// Return log entries that match the given request_id.
    fn get_logs_by_request_id(&self, request_id: &str) -> Vec<serde_json::Value>;

    /// Get log by request_id and tag, like composite key `request_id` + `log_kind`.
    /// Tag can be `log_kind`, `log_level`.
    /// Return log entries that match the given request_id and tag.
    fn get_logs_by_request_id_and_tag(&self, request_id: &str, tag: &str)
    -> Vec<serde_json::Value>;
}
```

## Jans Lock Server

In enterprise deployments, [Janssen Lock Server](../../janssen-server/lock/lock-server.md) collects Cedarling
logs and can stream to a database or S3 bucket. The Cedarling decision logs provide compliance
evidence of usage of the domain's externalized policies. The logs are also useful for forensic
analysis to show everything the attacker attempted, both allowed and denied.

## Sample logs

The JSON in this document is formatted for readability but is not prettified in the actual implementation.

### Startup Message

```json
{
    "id": "0193b8a8-efc0-77ce-bd90-4a62a2998462",
    "timestamp": "2024-12-12T04:18:19.456Z",
    "log_kind": "System",
    "level": "DEBUG",
    "pdp_id": "d47e245e-beaa-4ea4-b899-b8184cd3eb7e",
    "msg": "configuration parsed successfully"
}
{
    "id": "0193b8a8-efc1-7e42-9678-b2480268b91f",
    "timestamp": "2024-12-12T04:18:19.457Z",
    "log_kind": "System",
    "level": "INFO",
    "pdp_id": "d47e245e-beaa-4ea4-b899-b8184cd3eb7e",
    "application_id": "My App",
    "msg": "Cedarling Authz initialized successfully",
    "cedar_lang_version": "4.1.0",
    "cedar_sdk_version": "4.2.2"
}
```

### Decision Log

#### `authorize_unsigned` example

In the unsigned flow, `principal` contains the Cedar entity type names of each principal. The `tokens` field is omitted since no JWTs are involved.

```json
{
    "id": "019394db-f52b-7b06-88b8-a288670a32c1",
    "request_id": "019394db-f52b-7b06-88b8-a288670a32c2",
    "timestamp": "2024-12-05T05:27:43.403Z",
    "log_kind": "Decision",
    "pdp_id": "9e189c4b-96ae-4818-8e7f-75a42186af15",
    "policystore_id": "a1bf93115de86de760ee0bea1d529b521489e5a11747",
    "policystore_version": "undefined",
    "principal": [
        "Jans::User"
    ],
    "diagnostics": {
        "reason": [
            {
                "id": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
                "description": "policy for user"
            }
        ],
        "errors": []
    },
    "action": "Jans::Action::\"Update\"",
    "resource": "Jans::Issue::\"random_id\"",
    "decision": "ALLOW",
    "decision_time_micro_sec": 3
}
```

#### `authorize_multi_issuer` example

In the multi-issuer flow, `principal` is empty (no principal entities are created). The `tokens` field contains JWT claim information for each validated token.

```json
{
    "id": "019394db-f52b-7b06-88b8-a288670a32c3",
    "request_id": "019394db-f52b-7b06-88b8-a288670a32c4",
    "timestamp": "2024-12-05T05:27:43.403Z",
    "log_kind": "Decision",
    "pdp_id": "9e189c4b-96ae-4818-8e7f-75a42186af15",
    "policystore_id": "a1bf93115de86de760ee0bea1d529b521489e5a11747",
    "policystore_version": "undefined",
    "principal": [],
    "diagnostics": {
        "reason": [
            {
                "id": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
                "description": "policy for token access"
            }
        ],
        "errors": []
    },
    "action": "Acme::Action::\"GetFood\"",
    "resource": "Acme::Resource::\"approved_foods\"",
    "decision": "ALLOW",
    "tokens": {
        "Acme::Access_Token": {
            "jti": "token_abc"
        }
    },
    "decision_time_micro_sec": 3
}
```

#### Field Definitions

* `id`: unique identifier for this log entry
* `request_id`: unique identifier for the decision request
* `timestamp`: Derived if possible from the system or context--may be empty in cases where WASM can't access the system clock, and the time wasn't sent in the context.
* `log_kind`: type of log entry (`Decision`)
* `pdp_id`: unique identifier for the Cedarling
* `policystore_id`: What policystore this Cedarling instance is using
* `policystore_version`: What version of the policystore the Cedarling is using
* `principal`: List of principal entity type names used in the authorization request (e.g. `["Jans::User"]` for unsigned; empty `[]` for multi-issuer)
* `diagnostics`: Summary of policies that contributed to the decision and any evaluation errors
* `lock_client_id`: If this Cedarling has registered with a Lock Server, what is the client_id it received (omitted if not registered)
* `action`: From the request
* `resource`: From the request
* `decision`: `ALLOW` or `DENY`
* `tokens`: Dictionary with the token type and claims which should be included in the log (omitted if empty)
* `decision_time_micro_sec`: how long the decision took
* `pushed_data`: Information about pushed data injected into the authorization context (omitted if none)

Whenever a request ends with a `DENY` and Cedar diagnostics contain errors, Cedarling also emits a separate `ERROR`-level Decision log that summarizes those diagnostics. This makes policy failures immediately visible even if the full debug log is filtered out.

### Debug Log Sample

The result of the authorization is quite extensive because we log all `cedar-policy` entity information for forensic analysis. We cannot truncate the data, as it may contain critical information.

Below is an example from `authorize_unsigned` with a single User principal:

```json
{
    "id": "01937015-4649-7aad-8df8-4976e4bd8565",
    "request_id": "01937015-4649-7aad-8df8-4976e4bd8566",
    "timestamp": "2024-11-27T10:10:50.654Z",
    "log_kind": "System",
    "level": "DEBUG",
    "pdp_id": "75f0dc93-0a90-4076-95fa-dc16d3f00375",
    "application_id": "TestApp",
    "msg": "Result of authorize.",
    "action": "Jans::Action::\"Read\"",
    "resource": "Jans::Application::\"some_id\"",
    "context": {
        "user_agent": "Linux",
        "operating_system": "Linux",
        "network_type": "Local",
        "network": "127.0.0.1",
        "geolocation": [
            "America"
        ],
        "fraud_indicators": [
            "Allowed"
        ],
        "device_health": [
            "Healthy"
        ],
        "current_time": 1732752262
    },
    "entities": [
        {
            "uid": {
                "type": "Jans::User",
                "id": "user_123"
            },
            "attrs": {
                "sub": "user_123",
                "role": [
                    "CasaAdmin"
                ],
                "email": {
                    "domain": "jans.test",
                    "uid": "admin"
                }
            },
            "parents": [
                {
                    "type": "Jans::Role",
                    "id": "CasaAdmin"
                }
            ]
        },
        {
            "uid": {
                "type": "Jans::Role",
                "id": "CasaAdmin"
            },
            "attrs": {},
            "parents": []
        },

        ...

        {
            "uid": {
                "type": "Jans::Action",
                "id": "Tag"
            },
            "attrs": {},
            "parents": []
        }
    ],
    "authorize_info": [
        {
            "principal": "Jans::User::\"user_123\"",
            "diagnostics": {
                "reason": [
                    {
                        "id": "840da5d85403f35ea76519ed1a18a33989f855bf1cf8",
                        "description": "simple policy example for principal user"
                    }
                ],
                "errors": []
            },
            "decision": "ALLOW"
        }
    ],
    "authorized": true
}
```
