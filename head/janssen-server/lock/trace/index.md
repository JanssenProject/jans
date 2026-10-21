# Lock Server TRACE Evidence

TRACE support in the Lock Server accepts signed evidence records from authenticated producers, verifies who signed them, stores them unchanged, and lets authorized clients retrieve them by record or by governed execution. This page describes what the feature does and does not establish, how to configure and operate it, and its API.

## Purpose

TRACE helps answer four practical questions about a governed agent action:

1. What capability was authorized?
2. What enforcement point attempted or performed the invocation?
3. What did each evidence producer sign?
4. Which records belong to the same governed execution?

The Lock Server provides attributable, tamper-evident records and basic correlation. The core evidence path is:

`authorization decision → capability invocation → optional runtime effect`

A policy decision point (PDP) records what was authorized (`AUTHORIZATION_DECISION`). Cedarling support for producing TRACE records is in progress. The enforcement point, agent gateway, or tool gateway that mediates the action records `CAPABILITY_INVOKED`. An agent's own statement that it invoked a capability is not equivalent to enforcement-point evidence. An observer can record `RUNTIME_EFFECT`.

### What TRACE does not claim

- **No completeness.** TRACE does not claim that an execution's evidence is complete or that a producer reported every event.
- **No truth.** A valid signature shows who signed a record and that the signed fields have not changed. It does not show that the content is true, independently observed, or that enforcement succeeded.
- **No causation.** TRACE does not claim that a recorded action caused a claimed real-world effect. Lock does not infer that an authorization caused an invocation because their timestamps are close. Producers state causal relationships explicitly in `parent_record_ids`.
- **No settlement or anchoring.** Records are not labeled "settled", "complete", or externally anchored, and the Lock receipt chain has no signed checkpoints or external anchor (see [Receipt chain](#receipt-chain)).

Each record kind establishes one narrow claim and nothing stronger:

| Record | Establishes | Does not establish |
|---|---|---|
| `AUTHORIZATION_DECISION` | The PDP signed that it returned this decision under the referenced policy. | That the action occurred or was appropriate. |
| `CAPABILITY_INVOKED` | The enforcement point asserts it mediated the invocation. | That the target produced the claimed effect. |
| `RUNTIME_EFFECT` | The observer asserts that it observed the effect. | That the effect was authorized. |
| Lock receipt | Lock accepted these bytes at this position in its receipt chain. | That the producer's assertion is true or complete. |

## Roles

| Party | Responsibility |
|---|---|
| Policy decision point (PDP), for example Cedarling (TRACE support in progress) | Decides whether a capability is allowed and signs an `AUTHORIZATION_DECISION` record. |
| Enforcement point (PEP, agent gateway, tool gateway) | Mediates the action and signs a `CAPABILITY_INVOKED` record. |
| Evidence producer | Signs the record it creates. A PDP or enforcement point is the producer of its own records. |
| Submitting client | The OAuth client that sends a signed record to Lock. Usually the producer. An approved forwarder may submit for a producer if its binding allows that producer. |
| Lock Server | Verifies, stores, correlates, and returns evidence. Lock does not decide whether the governed capability should run. |

Three independent checks apply. Do not confuse them:

| Check | Question answered | Mechanism |
|---|---|---|
| Capability authorization | May the agent perform this action on this resource? | Cedarling or another PDP. The result is evidence that TRACE records. |
| TRACE API access control | May this client submit or read evidence through Lock? | OAuth access token, TRACE scope, and Lock endpoint policy. |
| Record verification | Who signed this record, and have the signed fields changed? | Producer key lookup and Ed25519 signature verification. |

### Scopes

| Scope | Endpoints |
|---|---|
| `https://jans.io/oauth/lock/trace.write` | `POST /audit/trace`, `POST /audit/trace/bulk` |
| `https://jans.io/oauth/lock/trace.readonly` | `GET /audit/trace/records`, `GET /audit/trace/records/{record_id}`, `GET /audit/trace/executions/{trace_execution_id}` |
| `https://jans.io/oauth/lock/trace.admin` | `/audit/trace/admin/*` (producer keys and chains) |

The client created by the Lock setup script receives scopes from the config-api Lock plugin, not from the Lock Server's own OpenAPI file. A client that uses TRACE needs these three scopes granted to it explicitly.

### Evidence domains

An evidence domain is the isolation boundary inside one Lock deployment. Lock derives it from the authenticated OAuth client and that client's configured binding. A producer never chooses the domain in the record.

- Producer keys, record identity, execution correlation, indexes, and retrieval are all scoped to the domain.
- A key registered in one domain gives no authority in another. A producer that serves several domains needs an explicit registration in each.
- Readers and administrators see only the domain their client is bound to. Lock never searches other domains.
- A client with no `clientDomainBindings` entry falls back to `defaultEvidenceDomainId` only for its domain. It gets no forwarding permission: see "Default-domain fallback" under [Configuration](#configuration).
- A signed `evidence_domain_id` in the body is ignored and the record is flagged `reserved_member_flag` (see [Single record](#single-record)). Lock never uses it for routing or storage.

## Configuration

TRACE is configured in the `traceConfiguration` block of the Lock Server dynamic configuration. The full property table is in [Lock Server Configuration and Operation](./lock-server.md#trace-configuration). The values that matter most:

```json
"traceConfiguration": {
  "enabled": true,
  "defaultEvidenceDomainId": "default",
  "defaultAllowedProducerIds": [],
  "clientDomainBindings": [
    {
      "clientId": "2200.00000000-0000-0000-0000-000000000000",
      "evidenceDomainId": "default",
      "allowedProducerIds": ["cedarling-fleet-1", "gateway-fleet-1"]
    }
  ],
  "latenessThresholdSeconds": 300,
  "policyMismatchGraceSeconds": 300,
  "policyReleaseRetentionDays": 30,
  "maxFutureSkewSeconds": 300,
  "maxRequestBytes": 262144,
  "maxBulkRecords": 100,
  "maxBulkRequestBytes": 4194304,
  "correlationRetryLimit": 3,
  "chainRegistrationMode": "PRE_REGISTERED",
  "cryptoProvider": "JAVA",
  "receiptAllocationRetryLimit": 8,
  "receiptRepairIntervalSeconds": 300,
  "pendingReceiptTimeoutSeconds": 120
}
```

**Client-to-domain bindings.** Lock resolves the domain for an authenticated `client_id` in this order: an explicit entry in `clientDomainBindings`, then `defaultEvidenceDomainId` if set, otherwise the request fails with `403 client_not_bound`. The same rule applies to submitters, readers, and administrators.

**Default-domain fallback.** A client that falls back to `defaultEvidenceDomainId` has no forwarding permission of its own. Its allowlist is `defaultAllowedProducerIds` (default: empty), never `"*"`. With the default empty list, an unbound client can read and administer the default domain but cannot submit any record: every submission fails with `403 producer_not_allowed`. To let unbound clients submit for specific producers, list those producers in `defaultAllowedProducerIds`. To let a client forward for a producer other than itself, give the client an explicit `clientDomainBindings` entry.

**Forwarding.** A client may submit a record only if the binding's `allowedProducerIds` contains the signed `producer` string exactly, or contains `"*"`. Otherwise the request fails with `403 producer_not_allowed`.

**Limits.** Lock applies the request limits (`maxRequestBytes`, `maxJsonDepth`, `maxStringLength`, `maxArrayLength`, `maxObjectMembers`) before any cryptographic work. A request over a limit fails with `400 invalid_request`. For bulk requests, `maxBulkRecords` bounds the array length and `maxBulkRequestBytes` bounds the whole body (always clamped to 16 MiB). `maxRequestBytes` still bounds each record in the batch.

**Lateness.** A record is flagged `late_flag: true` when Lock receives it more than `latenessThresholdSeconds` after its `signed_at`. Lateness alone never rejects a record. A `signed_at` that is more than `maxFutureSkewSeconds` ahead of Lock's clock is rejected with `invalid_assertion` (`signed_at_in_future`). The two properties are independent.

**Neighbor-update retries.** After a record is stored, Lock updates derived flags on neighboring records. Each update is a compare-and-swap write against the neighbor's version. On a version conflict, Lock re-reads the neighbor, recomputes the flag from its current state and retries, up to `correlationRetryLimit` times. When the budget is exhausted Lock logs a warning and gives up on that update. It never fails the request.

**Chain registration mode.** `chainRegistrationMode` is `PRE_REGISTERED` (default) or `FIRST_OBSERVED`. See [Chain registration modes](#chain-registration-modes). A value that is not one of the two names is treated as `PRE_REGISTERED` and logged once at `WARN`.

**Crypto provider.** `cryptoProvider` selects the implementation that parses producer keys, canonicalizes assertions, verifies signatures and computes receipt hashes. Values are matched case-insensitively:

- `JAVA` (default): BouncyCastle, in the Lock process. This is the implementation Lock shipped with.
- `NATIVE`: `jans-trace-core`, the Rust TRACE library intended for producers, reached through the `jans-trace-java` binding.

Both providers produce the same `content_digest`, key thumbprint, receipt hash and storage keys. Switching the provider does not invalidate the stored receipt chain.

Lock fails closed. When the selected provider is not available, because the native library is missing or its self-test fails, every TRACE submission returns `500 crypto_unavailable`. Lock never falls back to the other provider. Look for the startup log line `TRACE crypto provider: type=<type> available=<true|false> detail=<detail>`. The FIPS WAR ships without the native provider, so `NATIVE` fails closed there.

`NATIVE` rejects small-order Ed25519 public keys that BouncyCastle accepts. After you switch to `NATIVE`, a registered key of that kind fails with `unknown_producer_key` or `stored_key_unusable`. Check registered keys before you switch.

A value that is not one of the two names is treated as `JAVA` and logged once at `WARN`: `Unknown traceConfiguration.cryptoProvider '<value>', using JAVA`.

**Policy store release check.** `policyMismatchGraceSeconds` (default `300`) is how long after the last node stops serving a policy store release records judged under it still count as matching; see the acceptance window below. `policyReleaseRetentionDays` (default `30`) is how long a release no node serves any more is kept before it is deleted. A mismatch only sets `policy_mismatch_flag`; it never rejects a record. See [Policy store release check](#policy-store-release-check).

**Timers.** The receipt repair timer runs every `receiptRepairIntervalSeconds`. It settles receipt claims that stayed `PENDING` longer than `pendingReceiptTimeoutSeconds` (see [Deployment constraints](#deployment-constraints)). Setting `enabled` to `false` makes every TRACE endpoint return a structured `invalid_request` error with reason `trace_disabled` and stops the timer.

## Registering producer keys and chains

Lock accepts a producer key only through the admin API. It never accepts a public key because it accompanied a record. Possession of a signing key does not let the holder define its own producer identity. Registration, in the administrator's evidence domain, grants the authority.

The examples below use a token with the `trace.admin` scope:

```bash
HOST=https://lock.example.org
API="$HOST/jans-lock/api/v1/audit/trace"
TOKEN=$(curl -s -u "$CLIENT_ID:$CLIENT_SECRET" "$HOST/jans-auth/restv1/token" \
  -d grant_type=client_credentials \
  -d scope=https://jans.io/oauth/lock/trace.admin | jq -r .access_token)
```

`producer_id` can contain `/`, so the admin API carries it in request bodies and query parameters, never in path segments.

### Register a key

```bash
curl -s -X POST "$API/admin/producer-keys" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
    "producer_id": "cedarling-fleet-1",
    "kid": "cedarling-fleet-1-2026-01",
    "public_key_jwk": { "kty": "OKP", "crv": "Ed25519", "x": "<base64url public key, 32 bytes>" },
    "authorized_event_kinds": ["AUTHORIZATION_DECISION"],
    "valid_from": "2026-01-01T00:00:00Z",
    "valid_until": null
  }'
```

`producer_id` is the stable logical producer identifier, for example `cedarling-fleet-1`. It is not a `name/semver` string. Software version and build travel as separate signed metadata (see [Single record](#single-record)), so a software upgrade does not force a new producer identity, key or chain. The identifier is 1 to 255 characters, starts with a letter or digit, and then uses letters, digits and `. _ : / -`. Lock compares it byte for byte and never normalizes it.

`authorized_event_kinds` is required. It is a non-empty list of `AUTHORIZATION_DECISION`, `CAPABILITY_INVOKED` and `RUNTIME_EFFECT` without duplicates. A missing or empty list, an unknown kind or a duplicate returns `400 invalid_key` with reason `authorized_event_kinds`. The scope is immutable. To change it, register a new `kid`.

The response is `201` with the stored key, including `evidence_domain_id`, `authorized_event_kinds` and `revoked_at: null`. Registering the same `(producer_id, kid)` again returns `409 key_already_exists`. Keys are create-only: rotate by registering a new `kid`, and retire a key by revoking it. A JWK that is not `{kty: OKP, crv: Ed25519}` with a 32-byte `x`, or dates that do not parse, return `400 invalid_key`.

A key verifies a record only when, at verification time:

- `valid_from` has passed, `valid_until` is unset or in the future, and `revoked_at` is unset or in the future. Otherwise Lock rejects the record with `400 key_not_valid`.
- The record's `trace.event_kind` is in the key's `authorized_event_kinds`. Otherwise Lock rejects the record with `400 unknown_producer_key` and reason `event_kind_not_authorized`, exactly as for an unknown key. Nothing is stored or indexed. This check runs before any signature work.

A key row that was stored before `authorized_event_kinds` existed authorizes no event kinds. Such a producer must register a new `kid`.

The verification result stored with each record names the exact `kid`, the key thumbprint and the time of verification (see [Retrieval](#retrieval)). A later revocation, expiry or change to the key never changes it.

### Revoke a key

```bash
curl -s -X POST "$API/admin/producer-keys/revoke" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{ "producer_id": "cedarling-fleet-1", "kid": "cedarling-fleet-1-2026-01" }'
```

The response is `200` with the key and its `revoked_at`. Revoking an already revoked key is idempotent. An unknown key returns `404 record_not_found`.

### Register a producer chain

Every record belongs to a producer chain. With the default `PRE_REGISTERED` mode the chain must be registered before the producer's first record. The model is `producer_id` → `producer_instance_id` → one or more `producer_chain_id` values.

```bash
curl -s -X POST "$API/admin/producer-chains" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
    "producer_id": "cedarling-fleet-1",
    "producer_instance_id": "cedarling-001",
    "producer_chain_id": "chain-01JABC9Z0K"
  }'
```

The response is `201` and includes `"genesis_kind": "pre_registered_genesis"`. A duplicate returns `409 chain_already_exists`. In `PRE_REGISTERED` mode a record whose chain is not registered is rejected with `400 chain_not_registered`, at every sequence position.

One producer instance can own several chains at once: register each `producer_chain_id` separately. Chains are independent. Each has its own sequence numbers, its own gap, link and equivocation findings, and its own `prev_record_hash` links. Records of different chains can be interleaved in any order, in single or bulk submissions, and a batch can span several chains of the same producer. The Lock receipt sequence still increases per evidence domain across all chains.

### Chain registration modes

| Mode | Behavior |
|---|---|
| `PRE_REGISTERED` (default) | A chain exists only after an administrator registers it. A record on an unregistered chain is rejected with `400 chain_not_registered`. |
| `FIRST_OBSERVED` | A genesis record (`sequence_number` 1 with `prev_record_hash` set to the all-zero sentinel) on an unregistered chain registers the chain. Registration happens after the key, event-kind scope and signature checks succeeded, and before the record is stored. `registered_by` is the submitting client. Chains registered through the admin API keep working as before. |

In `FIRST_OBSERVED` mode:

- A record that fails any admission check (schema, forwarding, key, key validity, event-kind scope, signature) never registers a chain.
- Any other record on an unregistered chain is still rejected with `400 chain_not_registered`. This includes a producer that restarted mid-chain, or records that arrive out of order before the genesis record. Lock does not register a chain from a non-genesis record.
- A genesis position without the sentinel, or the sentinel at a non-genesis position, is still `400 invalid_genesis`.
- If two genesis records for one chain arrive at the same time, both are accepted: one registers the chain and the other finds it registered.
- Changing the mode affects only later submissions. Lock does not change existing chain registrations.

The chain list shows how each chain came to exist in `genesis_kind`: `pre_registered_genesis` for chains registered through the admin API (and for chains stored before this field existed), or `first_observed_genesis` for chains opened by a genesis record. A `first_observed_genesis` chain only says that the first genesis record Lock saw for that chain opened it. It says nothing about earlier producer history, and Lock does not claim to hold a complete history of the chain.

**Producer deployment patterns.** Keys identify trusted producers, chains identify independently sequenced record streams, and versions describe software. They are different identifiers. How a producer holds its key and chain state varies:

| Pattern | Key and chain state |
|---|---|
| Embedded or server Cedarling | The host supplies the key and keeps durable chain state. |
| Sidecar Cedarling | The sidecar owns the key and one or more chain writers. |
| Ephemeral pod | Uses a fleet or workload-provisioned key and opens a `first_observed_genesis` chain for that pod or writer. |
| WASM Cedarling | Should return the assertion to a trusted host for signing. A key held in a browser gives weaker producer assurance. |

Restarting a producer does not by itself mean a new chain. A producer that durably retained its `producer_chain_id`, last `sequence_number` and previous record hash should continue its existing chain. A new chain is needed when that state is unavailable, when a new independently sequenced writer is created, or when the producer rotates deliberately. Lock only checks the records it receives. The signed `chain_link` member can reference the previous chain head. Lock checks its shape and that only a genesis record carries it. Lock does not validate the link against the prior chain.

### List keys and chains

```bash
curl -s "$API/admin/producer-keys?producer_id=cedarling-fleet-1" -H "Authorization: Bearer $TOKEN"
curl -s "$API/admin/producer-chains" -H "Authorization: Bearer $TOKEN"
```

Both return `200`, with `{ "keys": [...] }` and `{ "chains": [...] }`. The `producer_id` filter is optional. Each chain entry carries `evidence_domain_id`, `producer_id`, `producer_instance_id`, `producer_chain_id`, `registered_by`, `genesis_kind` and `created_at`.

## Submitting records

### Single record

```http
POST /jans-lock/api/v1/audit/trace
Authorization: Bearer <access token with trace.write>
Content-Type: application/json
```

The body is the complete signed assertion. For an `AUTHORIZATION_DECISION` it looks like this:

```json
{
  "producer": "cedarling-fleet-1",
  "producer_version": "1.4.2",
  "kid": "cedarling-fleet-1-2026-01",
  "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
  "trace": {
    "eat_profile": "tag:jans.io,2026:trace-v1",
    "event_kind": "AUTHORIZATION_DECISION",
    "signed_at": 1781138542,
    "trace_execution_id": "exec-01JABCXYZQK8P5N9F2C7R3T4V6",
    "execution_authority": "spiffe://example.org/agent/planner",
    "subject": {
      "workload_id": "spiffe://example.org/agent/planner",
      "workload_instance_id": "04bbe9ef-a853-417c-a2b8-5328b62936e2"
    },
    "event": {
      "decision": {
        "action": "Acme::Action::\"Pay\"",
        "resource_type": "Acme::Payment",
        "outcome": "ALLOW"
      },
      "tokens": [
        { "issuer": "https://accounts.example.org", "token_type": "access_token", "jti": "9c9f2e77-9a3e-4b62-9a2a-2f6e6e2a9f3e" }
      ]
    },
    "policy": {
      "policy_store_id": "https://example.org/policy-stores/payments",
      "policy_store_version": "1.2.3",
      "policy_language": "cedar",
      "policy_language_version": "4.4.0",
      "bundle_hash": "sha256:<64 hex characters>"
    },
    "runtime": { "pdp_id": "cedarling-001" }
  },
  "producer_chain": {
    "producer_id": "cedarling-fleet-1",
    "producer_instance_id": "cedarling-001",
    "producer_chain_id": "chain-01JABC9Z0K",
    "sequence_number": 1,
    "prev_record_hash": "sha256:0000000000000000000000000000000000000000000000000000000000000000"
  },
  "parent_record_ids": [],
  "signature": "<base64url Ed25519 signature>"
}
```

Key points:

- `producer` is the stable logical producer identifier. It must equal `producer_chain.producer_id` byte for byte. `kid` is part of the signed content. `producer_version`, when present, is a string of 1 to 255 characters. It is signed and stored unchanged, and Lock does not interpret it.
- `record_id` is a producer-generated UUID or ULID. Record identity is `(evidence domain, producer, record_id)`, so two producers can use the same `record_id`.
- `trace.policy.policy_store_id`, `policy_store_version` and `bundle_hash` are at most 255 characters each (`length:<path>`). Lock indexes them for [search by policy store release](#search-by-policy-store-release).
- `trace_execution_id` and `execution_authority` are required and travel together. Lock correlates records by them. Lock neither creates them nor authorizes the execution. They are different from `subject.workload_id`: the authority scopes the execution identifier, and the workload is the subject that takes part in it.
- `trace.subject.workload_id` is required (non-empty string, at most 255 characters) for `AUTHORIZATION_DECISION` and `CAPABILITY_INVOKED`. `workload_instance_id` is optional. For `RUNTIME_EFFECT` the subject is optional and is validated when present.
- Producers sign capability *facts*, not a governance label. An `AUTHORIZATION_DECISION` carries a singular `decision` and a `CAPABILITY_INVOKED` carries a singular `invocation`. Each has `action` and `resource_type` (the Cedar action and resource type, never a resource id) and an `outcome`. See [Event kinds](#event-kinds). Lock does not resolve capability ids.
- `tokens[]` holds references only: `issuer`, `token_type`, and either `jti` or `fingerprint`. Never send a raw bearer token. Lock rejects material that looks like a JWT. Token identity is `(domain, issuer, jti)`, never a bare `jti`.
- A producer should not sign a member that Lock derives or assigns itself: `evidence_domain_id`, `cnf`, `content_digest`, `receipt_sequence`, `received_at`, `prev_receipt_hash`, `coverage_gap_flag`, `chain_link_failure_flag`, `equivocation_flag`, `late_flag`, `policy_mismatch_flag`, `reserved_member_flag`, `unattributed_flag`, `candidate_flag`, `key_resolved`, `key_temporally_valid`, `key_not_revoked`, `key_authorized_for_producer`, `producer_authorized_for_claim`, `key_thumbprint`, `key_authorization_ref`, `verified_at`, `trust_tier`, `capability_resolution`, `capability_id` and `capability_ids`. Lock checks the top level, `trace`, `trace.policy`, `trace.event` and each `decision` or `invocation` fact. A record that carries one is not rejected: the member is ignored, Lock uses its own derived values, and the record is accepted with `reserved_member_flag: true` (GovOps profile §12). The member stays in the stored and returned `assertion`, because the signature covers it; its value there is producer-supplied and ignored, and Lock's own values are in `ingestion` and `verification`. The paths are stored and returned as `reserved_members`, for example `["trace.trust_tier"]`. Only the first 32 are kept; Lock logs the total count. Lock resolves the verification key only from the producer key registry, never from a key in the body.
- `producer_chain` can carry an optional `chain_link` member that references the previous chain head. It must be a JSON object, and only a genesis record (`sequence_number` 1) can carry it. On any other record Lock returns `400 invalid_genesis` with reason `chain_link_on_non_genesis`. Lock checks that `prev_producer_chain_id`, when present, is a non-empty string. It keeps the member unchanged inside the signed assertion, returns it unchanged on retrieval, and does not validate the link against the prior chain.
- The first record of a chain has `sequence_number` 1 and `prev_record_hash` set to `sha256:` followed by 64 zeros. Any other record needs the previous record's `content_digest` in `prev_record_hash`.

### Optional profile members

Lock checks the JSON type of each optional member below when the producer signs it. A member of the wrong type returns `400 invalid_assertion` with reason `type:<path>`, `empty:<path>`, `length:<path>` or `invalid:<path>`, in the same way as the required members. A `null` value counts as absent. Lock does not interpret the values and stores them only inside the signed assertion.

| Location | Member | Accepted shape |
|---|---|---|
| `trace` | `execution_epoch_id`, `previous_epoch_id`, `parent_execution_id` | Non-empty string of at most 255 characters. |
| `trace` | `operation_ids` | Array of non-empty strings. |
| `trace` | `session_id`, `session_issuer` | Non-empty strings. `session_issuer` is required when `session_id` is present (`missing:trace.session_issuer`) and follows the token issuer rule (`https`, or `http` for `localhost`). |
| `trace` | `evidence_origin`, `measurement_point` | Non-empty strings. Lock does not restrict the values. |
| `trace` | `recorder` | Object or string. |
| `trace` | `attestation_ref` | Object. |
| `trace` | `workload_authentication` | Object. The members `authentication_layer`, `method`, `authenticated_workload_id` and `proof_ref` are strings when present. |
| `trace` | `model` | Object. The members `provider`, `model_id` and `version` are strings when present. |
| `trace.subject` | `human_sponsor`, `delegating_org` | String or object. |
| `trace.policy` | `enforcement_mode` | `enforce`, `advisory`, `silent` or `declared`. Checked wherever Lock validates `trace.policy`: always for `AUTHORIZATION_DECISION`, and when present for `CAPABILITY_INVOKED`. |

### Event kinds

| Event kind | Producer | Required event data |
|---|---|---|
| `AUTHORIZATION_DECISION` | Cedarling or another PDP | Exactly one of a singular `trace.event.decision` `{action, resource_type, outcome}` or `trace.event.decisions[]`. A complete `trace.policy` block. `trace.runtime.pdp_id`. `trace.subject.workload_id`. |
| `CAPABILITY_INVOKED` | PEP, agent gateway or tool gateway | Exactly one of a singular `trace.event.invocation` `{action, resource_type, outcome}` or `trace.event.invocations[]`. `trace.event.enforcement_point_id`. `trace.subject.workload_id`. `trace.policy` is optional. When present, it must be complete, like a decision's. |
| `RUNTIME_EFFECT` | Target system or trusted observer | `trace.event.outcome`. It carries no `decision`, `decisions`, `invocation` or `invocations`. It normally has a `produced_effect` parent in `parent_record_ids`. Lock accepts it without one and notes a warning at debug level. |

Rules for the capability facts:

- `action` and `resource_type` are non-empty strings of at most 512 characters.
- For `AUTHORIZATION_DECISION`, `outcome` is `ALLOW` or `DENY`. For `CAPABILITY_INVOKED`, `outcome` is `SUCCESS` or `FAILURE`. The vocabularies are distinct, so an invocation with `ALLOW` or a decision with `SUCCESS` returns `400 invalid_assertion` with reason `invalid:trace.event.invocation.outcome` (or `invalid:trace.event.decision.outcome`, or the indexed plural path such as `invalid:trace.event.invocations[0].outcome`).
- An atomic batch evaluation can use the plural form instead. Each entry carries a `decision_id` or `invocation_id` (non-empty, at most 256 characters, unique within the array) and repeats `action`, `resource_type` and `outcome`. The record-level `trace.event.outcome` is not allowed in either form.
- Operation-binding digests are optional members of `trace.event`: `request_digest`, `input_digest` and `tool_call_digest` on `AUTHORIZATION_DECISION`, and `request_digest`, `tool_call_digest` and `result_digest` on `CAPABILITY_INVOKED` (and `result_digest` on `RUNTIME_EFFECT`). A present digest is `sha256:` followed by 64 lowercase hex characters. Any other string returns `400 invalid_assertion` with reason `invalid:trace.event.<member>`, and a non-string value returns `type:trace.event.<member>`. Lock stores them in the signed assertion and does not interpret them. `trace.policy.bundle_hash` keeps its own rule, which also accepts other `algorithm:digest` forms.
- Both the singular and the plural member in one record, and a `resource_id` inside a fact, are rejected with `400 invalid_assertion` (reason such as `forbidden:trace.event.decisions`). A signed `capability_ids` is a Lock-derived member: it is ignored and flagged with `reserved_member_flag`.

Lock validates the schema for the declared kind and rejects fields that contradict it. Lock does not infer that an authorization caused an invocation because their timestamps are close. Producers state causal relationships in `parent_record_ids`.

### Signature scope and digest scope

The two scopes are intentionally different:

| Value | Computed over |
|---|---|
| `signature` | Ed25519 signature over the RFC 8785 (JCS) canonical form of the assertion **without** its top-level `signature` member. |
| `content_digest` | `sha256:` plus lowercase hex SHA-256 over the JCS canonical form of the **complete** assertion, including `signature`. |

`content_digest` is computed by Lock. It drives replay and conflict detection and is not a signature check.

### Response

```json
{
  "accepted": true,
  "producer_id": "cedarling-fleet-1",
  "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
  "content_digest": "sha256:...",
  "receipt_sequence": 108422,
  "received_at": "2026-06-11T00:41:02.123Z",
  "idempotent_replay": false,
  "coverage_gap_flag": false,
  "chain_link_failure_flag": false,
  "equivocation_flag": false,
  "late_flag": false,
  "policy_mismatch_flag": false,
  "reserved_member_flag": false,
  "reserved_members": []
}
```

`202 Accepted` means the record was verified and durably stored. Lock verifies in a fixed order: OAuth check, domain derivation, JSON structure and limits, forwarding check, key resolution (validity window, then event-kind scope), signature verification, digest, then the idempotency, chain, and equivocation checks. A record that fails verification never enters storage or any retrieval index.

`received_at` is an RFC 3339 UTC timestamp with exactly three fractional digits and a `Z` suffix, for example `2026-06-11T00:41:02.123Z`. The receipt and verification timestamps in API output (`received_at`, `verified_at`) use this form. Timestamps in the admin API (`created_at`, `revoked_at`) are RFC 3339 UTC and may omit a zero fraction.

| Condition | Result |
|---|---|
| Same identity, same `content_digest` | `202` with the original receipt and `idempotent_replay: true`. No second receipt is created. |
| Same identity, different `content_digest` | `409 record_conflict`. The first record is never overwritten. |
| Missing predecessor in the chain | Accepted with `coverage_gap_flag: true`. |
| Predecessor present but its digest does not match `prev_record_hash` | Accepted with `chain_link_failure_flag: true`. |
| Same chain position, different `record_id` | Both are kept and flagged `equivocation_flag: true`. |

### Bulk submission

`POST /jans-lock/api/v1/audit/trace/bulk` takes a JSON array of assertions, each in the single-record shape, with the same `trace.write` scope. A high-throughput producer such as Cedarling can use it instead of one request per record.

- **One producer per batch.** Every element must carry the same signed `producer`. A batch that mixes producers is rejected as a whole with `400 invalid_request` and reason `bulk_mixed_producers`, and nothing is ingested. Elements without a readable `producer` are not counted and fail on their own.
- **Per record, not all or nothing.** Every element is processed as an independent single submission, in array order. One element's failure never rejects the others. Replay, conflict, gap and equivocation behavior is per record and identical to the single-record path.
- **Several chains in one batch.** Records in one batch can advance several `producer_chain_id` chains of the producer. Each chain is checked on its own.
- Resubmitting a whole array is safe: records that were already committed replay idempotently.

A processed batch returns `207 Multi-Status`, whatever the per-item results. Accepted entries carry the single-record acceptance members inline. Rejected entries carry the error:

```json
{
  "results": [
    {
      "index": 0, "accepted": true,
      "producer_id": "cedarling-fleet-1", "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
      "content_digest": "sha256:...", "receipt_sequence": 108422, "received_at": "2026-06-11T00:41:02.123Z",
      "idempotent_replay": false, "coverage_gap_flag": false, "chain_link_failure_flag": false,
      "equivocation_flag": false, "late_flag": false, "policy_mismatch_flag": false,
      "reserved_member_flag": false, "reserved_members": []
    },
    {
      "index": 1, "accepted": false,
      "producer_id": "cedarling-fleet-1", "record_id": "1b2c3d4e-0000-4000-8000-000000000000",
      "error": { "error": "invalid_signature", "error_description": "...", "reason": "signature_mismatch" }
    }
  ]
}
```

`index` is the position in the submitted array, and `results` keeps request order. `record_id` and `producer_id` are present on a rejected entry when they could be read. The error ids are the ones in the [error catalog](#error-catalog).

Request-level failures return an ordinary structured error with a real HTTP status and no `results`: malformed JSON, a body that is not an array (`not_array`), an empty array (`empty_bulk_records`), more than `maxBulkRecords` elements (`max_bulk_records_exceeded`), a body over `maxBulkRequestBytes` (`bulk_body_too_large`), mixed producers (`bulk_mixed_producers`), TRACE disabled, and `client_not_bound`. Receipt sequences are allocated in array order but are not guaranteed to be contiguous within a batch, because a rejected item claims no receipt.

### Error catalog

Errors use the structured body `{"error": "<id>", "error_description": "<text>", "reason": "<detail>"}`. The `reason` member appears when error reasons are enabled and names the offending field or check. OAuth failures (`401`, `403` from the protection layer) come from Lock's standard filters and are not part of this table.

| Error id | HTTP | When |
|---|---|---|
| `invalid_request` | 400 | Malformed JSON, a limit exceeded, an unreadable body, bad query parameters, TRACE disabled (`trace_disabled`), or a bulk envelope problem such as `bulk_mixed_producers` (see [Bulk submission](#bulk-submission)). |
| `invalid_assertion` | 400 | A schema violation in the assertion. `reason` names the field. |
| `unsupported_profile` | 400 | `trace.eat_profile` is not `tag:jans.io,2026:trace-v1`. |
| `producer_mismatch` | 400 | `producer` does not equal `producer_chain.producer_id`. |
| `unknown_producer_key` | 400 | No registered key for the domain, producer, and `kid`, or the record's `event_kind` is outside the key's `authorized_event_kinds` (reason `event_kind_not_authorized`). |
| `key_not_valid` | 400 | The key is outside its validity window or revoked. |
| `invalid_signature` | 400 | Ed25519 verification failed, or the signature is not valid base64url. |
| `chain_not_registered` | 400 | The producer chain is not registered. |
| `invalid_genesis` | 400 | The genesis position and `prev_record_hash` are inconsistent. |
| `client_not_bound` | 403 | The client resolves to no evidence domain. |
| `producer_not_allowed` | 403 | The client's binding does not allow the signed producer. |
| `record_not_found` | 404 | No matching record in the domain. Also returned by the admin revoke call for an unknown key. |
| `execution_not_found` | 404 | No records for the execution in the domain. |
| `record_conflict` | 409 | The same identity exists with a different `content_digest`. |
| `ambiguous_identifier` | 409 | A bare `record_id` or `trace_execution_id` matches more than one producer or authority in the domain. |
| `key_already_exists` | 409 | Admin: duplicate key registration. |
| `chain_already_exists` | 409 | Admin: duplicate chain registration. |
| `invalid_key` | 400 | Admin: the JWK is not a 32-byte Ed25519 OKP key, the dates are invalid, or `authorized_event_kinds` is missing, empty, unknown or duplicated. |
| `storage_failure` | 500 | Persistence failed, or receipt allocation ran out of retries. |
| `crypto_unavailable` | 500 | The selected crypto provider (`cryptoProvider`) is not available or has no Ed25519 support. Lock fails closed and does not fall back to the other provider. |

### Canonical propagation wire profile

This table is informational. It fixes one mapping for the base correlation fields so that independently written producers stay correlatable. **Lock does not read these headers, baggage keys or token claims.** Lock only reads the signed `trace_execution_id`, `execution_authority` and `subject` members of the assertion. A host, sidecar, PEP or gateway translates the carrier into the producer's input.

| TRACE field | HTTP header | OTel baggage key | Token claim |
|---|---|---|---|
| `trace_execution_id` | `Audit-Trace-ID` | `audit.trace_id` | `https://jans.io/trace/execution_id` |
| `execution_authority` | `Audit-Execution-Authority` | `audit.execution_authority` | `https://jans.io/trace/execution_authority` |
| `workload_id` | `Audit-Workload-ID` | `audit.workload_id` | `https://jans.io/trace/workload_id` |
| `workload_instance_id` | `Audit-Workload-Instance-ID` | `audit.workload_instance_id` | `https://jans.io/trace/workload_instance_id` |

- `trace_execution_id` and `execution_authority` always travel together.
- Cedarling receives normalized values through its authorization API. It does not parse headers, tokens or baggage.
- Unsigned headers and baggage are correlation hints, not authoritative claims. A trusted ingress component should validate or replace externally supplied values instead of forwarding them.
- Signed token claims bind more strongly, but their issuer and signature must still be validated.
- When carriers disagree, the precedence is: validated token claim, then trusted ingress or host input, then unsigned header, then OTel baggage. The conflict should be recorded and not silently resolved.

The component that creates the governed unit of work (an agent runtime, a gateway, a PEP, an application host or a scheduler) creates the identifier pair and propagates it. Neither Cedarling nor Lock generates it.

## Retrieval

All retrieval endpoints need the `trace.readonly` scope and read only the caller's evidence domain.

```bash
# One record. producer_id should be supplied.
curl -s "$API/records/9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41?producer_id=cedarling-fleet-1" \
  -H "Authorization: Bearer $READ_TOKEN"

# The direct records of one execution. execution_authority should be supplied.
curl -s "$API/executions/exec-01JABCXYZQK8P5N9F2C7R3T4V6?execution_authority=spiffe%3A%2F%2Fexample.org%2Fagent%2Fplanner&start=0&count=100" \
  -H "Authorization: Bearer $READ_TOKEN"
```

A record response has three separate parts: the assertion (in a parsed form and as exact text), the verification result and the ingestion metadata.

```json
{
  "producer_id": "cedarling-fleet-1",
  "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
  "assertion": { "...": "the producer-signed record" },
  "assertion_raw": "{\"...\": \"the producer-signed record, exact text\"}",
  "content_digest": "sha256:...",
  "verification": {
    "signature_valid": true,
    "key_id": "cedarling-fleet-1-2026-01",
    "key_thumbprint": "sha256:...",
    "verified_at": "2026-06-11T00:41:02.123Z",
    "algorithm": "Ed25519",
    "key_authorized": true
  },
  "ingestion": {
    "receipt_sequence": 108422,
    "received_at": "2026-06-11T00:41:02.123Z",
    "prev_receipt_hash": "sha256:...",
    "receipt_hash": "sha256:...",
    "receipt_state": "COMMITTED",
    "coverage_gap_flag": false,
    "chain_link_failure_flag": false,
    "equivocation_flag": false,
    "late_flag": false,
    "policy_mismatch_flag": false,
    "reserved_member_flag": false,
    "reserved_members": []
  }
}
```

- `assertion_raw` is the exact accepted text: Lock stores the request body as received and returns it unchanged, byte for byte. Lock never adds resolved claims, status, or receipt data to it.
- `assertion` is the same text parsed as a JSON value. It is a re-emission, so member order, whitespace and number formatting can differ from the signed bytes. Use it when you want structure and `assertion_raw` when you need the original text.
- Both forms verify. The signature and `content_digest` are defined over the JCS (RFC 8785) form, and the JCS form of `assertion` and of `assertion_raw` is identical.
- `verification` records the admission decision Lock made at `verified_at`. It is written once at ingestion and never recomputed from the current key registry. `key_thumbprint` pins the exact key, independently of the non-unique `kid` selector: it is `sha256:` plus the lowercase hex SHA-256 of the RFC 7638 required-members JSON of the key (`{"crv":"Ed25519","kty":"OKP","x":"<x>"}`). `key_authorized` is a single boolean. It states that the key existed, was inside its validity window, was not revoked, was registered for the `producer_id` and was authorized for the record's `event_kind`. Every stored record has `key_authorized: true`, because a record that fails any admission check is rejected and never stored. Later revocation, expiry or re-scoping of the key never rewrites these values. Records stored before these two members existed omit them.
- `ingestion` is Lock-derived metadata.

If `producer_id` (or `execution_authority`) is omitted and the bare identifier matches more than one producer (or authority) in the domain, Lock returns `409 ambiguous_identifier`. It never guesses.

### Search by policy store release

`GET /audit/trace/records` (no `record_id`) returns the records whose `trace.policy` names a policy store release. Use it to answer "which decisions were made with release X".

```bash
curl -s "$API/records?policy_store_id=https%3A%2F%2Fexample.org%2Fpolicy-stores%2Fpayments&policy_store_version=1.2.3&received_from=2026-06-01T00:00:00Z&start=0&count=100" \
  -H "Authorization: Bearer $READ_TOKEN"
```

| Parameter | Meaning |
|---|---|
| `policy_store_id` | Exact `trace.policy.policy_store_id`. |
| `policy_store_version` | Exact `trace.policy.policy_store_version`. |
| `bundle_hash` | Exact `trace.policy.bundle_hash`. |
| `received_from`, `received_to` | Inclusive bounds on `received_at`, RFC 3339. |
| `start`, `count` | Paging, as for executions. |

At least one of `policy_store_id` or `bundle_hash` is required; otherwise Lock returns `400 invalid_request` with reason `missing:policy_store_id`. The response is `{"ordering": "receipt_sequence", "records": [...]}` with record envelopes in receipt order. No match returns `200` with an empty list. Only records that carry `trace.policy` (`AUTHORIZATION_DECISION`, and `CAPABILITY_INVOKED` when present) are indexed. Records stored before this index existed are not returned.

The execution response carries `execution_authority`, `trace_execution_id`, `"ordering": "receipt_sequence"`, `"completeness": "not_assessed"`, and `records`, ordered by Lock receipt sequence. `start` (default 0) and `count` (default 100, maximum 1000) page the list. The execution view contains the directly associated records only. It claims no global event order, does not traverse child executions, and does not assess completeness.

## Flags explained

Flags are Lock-derived findings about a record. Lock flags a condition instead of silently dropping or rejecting the record.

| Flag | Meaning | Cleared later? |
|---|---|---|
| `coverage_gap_flag` | The record's predecessor in its producer chain (`sequence_number - 1`) has not been seen. Records can arrive out of order. | Yes. When the missing position arrives and links correctly, the flag on its successor clears. |
| `chain_link_failure_flag` | The predecessor is present but its `content_digest` does not match this record's `prev_record_hash`. A genuine break in the chain. | No. The conflicting predecessor already exists, so later arrivals cannot resolve it. It stays visible as an integrity finding. |
| `equivocation_flag` | Two or more records with different `record_id` values claim the same chain position. Lock stores and flags all of them and never selects a winner. | No. |
| `late_flag` | Lock received the record more than `latenessThresholdSeconds` after its `signed_at`. Fixed at ingestion. | No. |
| `policy_mismatch_flag` | The record's `trace.policy` names a policy store Lock serves, with a release this node did not serve at `signed_at` or within `policyMismatchGraceSeconds` before it. Fixed at ingestion. See [Policy store release check](#policy-store-release-check). | No. |
| `reserved_member_flag` | The producer signed a member Lock derives itself, such as `evidence_domain_id` or `trust_tier`. Lock ignored it and used its own values; `reserved_members` lists the paths. Fixed at ingestion. | No. |

Lock updates the flags on neighboring records after it stores a new record. These updates are best-effort and eventually consistent. If one fails, Lock logs it and still accepts the new record. A reader can see a flag change shortly after a record is accepted.

A producer-chain break or gap is a statement about the records Lock has received. It is not a statement about what the producer did.

## Policy store release check

Lock serves policy store archives to producers (`GET /policy`, `GET /policy/stores`) and computes each archive's `bundle_hash` as `sha256:` plus the lowercase hex SHA-256 of the archive bytes. Lock reloads policy stores every 30 seconds. On every reload each node records the releases it serves in the shared TRACE store (`jansTracePolicyRelease`, one row per `policy_store_id` and `bundle_hash`, not scoped to an evidence domain): the first time a release is served it is inserted, and after that its last-served time moves forward at most once a minute. Updates use optimistic locking and never move the last-served time back or the first-served time forward, so a replica with a lagging clock cannot shorten a release's history. A release that is no longer served stops moving forward. Because the history is in the database, every replica sees the same releases, and it survives restarts and rolling updates.

When an accepted record's `trace.policy.policy_store_id` is a store this node serves as an archive, Lock judges the record's release at its `received_at`, or at its `signed_at` when the record is flagged `late_flag`. Using the receipt time means a producer cannot backdate `signed_at` to slip a retired release past the check; it can shift the time by at most `latenessThresholdSeconds`, and beyond that the record is flagged late. The release is accepted when:

- this node serves that `bundle_hash` now; or
- its stored last-served time is within the acceptance window before that time. The window is the larger of `policyMismatchGraceSeconds` and `latenessThresholdSeconds`, plus 130 seconds: a stored last-served time can trail the real one by the one-minute update interval, two 30-second reloads and the 10-second cache. With the defaults the window is 430 seconds, so a record signed just before a rollout and received up to five minutes later still matches, and a release another replica still serves never looks retired.

Otherwise, and also when a matching release has a known `policy_store_version` that differs from the record's, the record is flagged `policy_mismatch_flag`. A record signed under the previous release during a rollout, or uploaded shortly after it, is not flagged. The record is always accepted.

Limits:

- Before the first policy store load, Lock serves no archive and flags nothing.
- A store this node does not serve is never compared, because producers may load stores from other sources.
- Lock caches found history rows for 10 seconds (at most 1000, least recently used evicted). A release it has not found is remembered as missing for one second, or until this node publishes a reload, so a release another replica starts serving is seen within a second and an unknown hash repeated by a producer costs at most one store read per second.
- Releases no node has served for `policyReleaseRetentionDays` are deleted by the receipt repair timer, 500 per run, except a release this node serves. Each delete only removes the row if it is still that old, so a release another replica refreshed in the meantime is kept, and replicas pruning at the same time do not interfere. A record later naming a deleted release is flagged.
- The check is best-effort: if it or the store fails, Lock logs a warning and accepts the record without the flag.

The flag is a statement about what Lock served. It does not show that the producer used a tampered policy.

## Receipt chain

For every accepted record Lock assigns, within the evidence domain:

- `receipt_sequence`: an increasing sequence number.
- `received_at`: Lock's receipt time.
- `prev_receipt_hash`: the hash of the preceding receipt entry.
- `receipt_hash`: the hash of this entry.

Every receipt entry has a state: `PENDING` while Lock stores the record, `COMMITTED` once the record is stored, and `VOID` if the record was never stored. The retrieval response carries the state of a record's receipt entry as `ingestion.receipt_state`. A stored record shows `COMMITTED`, or `PENDING` during the short window between storing the record and settling its claim. It never shows `VOID`.

The receipt hash binds the receipt profile tag `receipt_profile` (`tag:jans.io,2026:lock-trace-receipt-v1`), `evidence_domain_id`, `receipt_sequence`, `received_at`, `producer_id`, `record_id`, `content_digest`, and `prev_receipt_hash`. The hash input is the JCS canonical form of those eight members. `receipt_profile` is a string constant in every hash input. A change to this member set needs a new profile tag. `received_at` enters the hash as a JSON string in the RFC 3339 form with three fractional digits and `Z` (`2026-06-11T00:41:02.123Z`), and `receipt_sequence` as a JSON integer. A value shown without milliseconds would not reproduce the receipt hash. The first entry in a domain uses the all-zero sentinel as its `prev_receipt_hash`.

A verifier can walk the receipts by sequence, recompute each hash, and check each committed entry against the stored record. This makes local insertion, deletion, and rewriting detectable.

A verifier must accept a `VOID` entry whose hash and `prev_receipt_hash` linkage verify and which has no stored record. A `VOID` entry is a chain position that Lock allocated and then abandoned. It is not a missing entry, and a verifier must not report it as a gap. A `VOID` entry that has a stored record is an inconsistency. The receipt state is not part of the receipt hash, so settling an entry never changes the hashes of the chain.

!!! warning "What the receipt chain does not protect against"

    The receipt chain is local tamper evidence only. It has no signed checkpoints and no independent external anchor. It does not protect against an operator who can rewrite the whole chain, including every hash. Do not describe the receipt chain as proof of integrity against such an operator, and do not describe TRACE evidence as settled, complete, or externally anchored.

Producer order, Lock receipt order, and causal relationships are separate things. Producer order is what one producer signed in sequence. Receipt order is when Lock accepted records. `parent_record_ids` carries the causal relationships a producer asserted. None of them is a global event clock.

## Deployment constraints

**Multi-node deployments.** The database ORM provides no transactions or sequences, so Lock allocates receipt positions using only primary-key uniqueness:

1. A node reads the highest receipt and proposes the next sequence number.
2. It inserts a `PENDING` receipt claim at that position. If another node claimed it first, the node re-reads and retries, up to `receiptAllocationRetryLimit` times, then fails with `500 storage_failure`.
3. It stores the record, then marks the claim `COMMITTED`. If storing fails, it marks the claim `VOID`.

A node that stops between steps leaves a `PENDING` claim. The repair timer settles claims older than `pendingReceiptTimeoutSeconds`: `COMMITTED` if the record exists, otherwise `VOID`. `VOID` entries keep the chain dense and verifiable, because the receipt state is not part of the receipt hash. A claim without a record is never returned by any API.

The database ORM has no transactions, so storing the record and appending its receipt entry is not a single atomic write. A failed request leaves no stored record, no index entry, and no `COMMITTED` receipt entry. It can leave a `VOID` receipt entry at the allocated position, and a `PENDING` entry is visible between the claim and its settlement. After a node stops, a `PENDING` entry stays until the repair timer settles it. That takes up to `pendingReceiptTimeoutSeconds` plus `receiptRepairIntervalSeconds`. A `VOID` entry stays in the chain permanently, because the `prev_receipt_hash` of the next entry binds its hash.

**Request size and column size.** `maxRequestBytes` limits the size of one request body. The stored assertion is kept as text in a `MEDIUMTEXT` column on MySQL and a `TEXT` column on PostgreSQL. Keep `maxRequestBytes` and the per-record limit inside the bulk path well below the size of that column type for your database, and raise the limits only after checking it. Many TRACE identifiers are stored in `VARCHAR` columns of fixed width, which Lock's validation keeps the values within.

**Upgrading.** Search by policy store release, `policy_mismatch_flag` and `reserved_member_flag` use columns added in this release. An existing VM install must add them once; see [Upgrading an Existing Deployment](./lock-server.md#upgrading-an-existing-deployment).

**Time.** Lock uses `received_at` for receipt order and treats `signed_at` as a producer claim. Keep node clocks synchronized.

## Deferred features

The current release does not implement:

- gRPC transport for TRACE (REST only);
- a quarantine store for rejected records: rejected records are not stored;
- Lock-side management through the config-api Lock plugin and the Jans CLI/TUI;
- settlement assessments or Lock-signed settlement receipts;
- execution completeness or assurance profiles;
- lineage checkpoints, `chain_link` validation, or claims of complete producer history;
- signed receipt checkpoints or external transparency anchoring;
- Evidence Packet export;
- delegation non-expansion analysis and authorization-transition reconstruction;
- user-intent and approval records;
- operation-binding commitments, including proof-of-possession binding;
- capability resolution (deriving a `capability_id` from the signed action and resource type) and a capability index. The signed facts stay in the stored assertion, so the index can be added later without re-emitting records;
- the Producer-Key Authorization Statement and per-measurement-point key scoping (Lock enforces `authorized_event_kinds` against its live registry);
- the WIMSE agent identifier profile, credential metadata on `tokens[]` and the `workload_authentication` record;
- token-claim enrichment, current-state resolution, and token-exchange lineage;
- attestation assessment or trust tiers;
- cross-domain correlation or import;
- Lock emitting TRACE records for access to its own endpoints;
- graph traversal beyond direct execution membership.

Later features are meant to be additive. They may assess, correlate, checkpoint, or package an accepted assertion, but they never rewrite it.
