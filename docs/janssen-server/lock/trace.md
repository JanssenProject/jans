---
tags:
  - administration
  - lock
  - audit
  - evidence
  - TRACE
---

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

Cedarling, or another policy decision point (PDP), records what was authorized (`AUTHORIZATION_DECISION`). The enforcement point, agent gateway, or tool gateway that mediates the action records `CAPABILITY_INVOKED`. An agent's own statement that it invoked a capability is not equivalent to enforcement-point evidence. An observer can record `RUNTIME_EFFECT`.

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
| Policy decision point (PDP), for example Cedarling | Decides whether a capability is allowed and signs an `AUTHORIZATION_DECISION` record. |
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
| `https://jans.io/oauth/lock/trace.readonly` | `GET /audit/trace/records/{record_id}`, `GET /audit/trace/executions/{trace_execution_id}` |
| `https://jans.io/oauth/lock/trace.admin` | `/audit/trace/admin/*` (producer keys and chains) |

The client created by the Lock setup script receives scopes from the config-api Lock plugin, not from the Lock Server's own OpenAPI file. A client that uses TRACE needs these three scopes granted to it explicitly.

### Evidence domains

An evidence domain is the isolation boundary inside one Lock deployment. Lock derives it from the authenticated OAuth client and that client's configured binding. A producer never chooses the domain in the record.

- Producer keys, record identity, execution correlation, indexes, and retrieval are all scoped to the domain.
- A key registered in one domain gives no authority in another. A producer that serves several domains needs an explicit registration in each.
- Readers and administrators see only the domain their client is bound to. Lock never searches other domains.
- A request body that contains `evidence_domain_id` (top level or inside `trace`) is rejected with `domain_field_forbidden`.

## Configuration

TRACE is configured in the `traceConfiguration` block of the Lock Server dynamic configuration. The full property table is in [Lock Server Configuration and Operation](./lock-server.md#trace-configuration). The values that matter most:

```json
"traceConfiguration": {
  "enabled": true,
  "defaultEvidenceDomainId": "default",
  "clientDomainBindings": [
    {
      "clientId": "2200.00000000-0000-0000-0000-000000000000",
      "evidenceDomainId": "default",
      "allowedProducerIds": ["cedarling-fleet-1/1.0.0", "gateway-fleet-1/2.0.0"]
    }
  ],
  "latenessThresholdSeconds": 300,
  "maxRequestBytes": 262144,
  "maxBulkRecords": 100,
  "maxBulkRequestBytes": 4194304,
  "chainRegistrationMode": "PRE_REGISTERED",
  "receiptAllocationRetryLimit": 8,
  "receiptRepairIntervalSeconds": 300,
  "pendingReceiptTimeoutSeconds": 120
}
```

**Client-to-domain bindings.** Lock resolves the domain for an authenticated `client_id` in this order: an explicit entry in `clientDomainBindings`, then `defaultEvidenceDomainId` if set, otherwise the request fails with `403 client_not_bound`. The same rule applies to submitters, readers, and administrators.

**Forwarding.** A client may submit a record only if the binding's `allowedProducerIds` contains the signed `producer` string exactly, or contains `"*"`. Otherwise the request fails with `403 producer_not_allowed`.

**Limits.** Lock applies the request limits (`maxRequestBytes`, `maxJsonDepth`, `maxStringLength`, `maxArrayLength`, `maxObjectMembers`) before any cryptographic work. A request over a limit fails with `400 invalid_request`. For bulk requests, `maxBulkRecords` bounds the array length and `maxBulkRequestBytes` bounds the whole body (always clamped to 16 MiB). `maxRequestBytes` still bounds each record in the batch.

**Lateness.** A record is flagged `late_flag: true` when Lock receives it more than `latenessThresholdSeconds` after its `signed_at`. Lateness alone never rejects a record. A `signed_at` that is more than `latenessThresholdSeconds` in the future is rejected with `invalid_assertion` (`signed_at_in_future`).

**Chain registration mode.** `chainRegistrationMode` is `PRE_REGISTERED` (default) or `FIRST_OBSERVED`. See [Chain registration modes](#chain-registration-modes). A value that is not one of the two names is treated as `PRE_REGISTERED` and logged once at `WARN`.

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
    "producer_id": "cedarling-fleet-1/1.0.0",
    "kid": "cedarling-fleet-1-2026-01",
    "public_key_jwk": { "kty": "OKP", "crv": "Ed25519", "x": "<base64url public key, 32 bytes>" },
    "valid_from": "2026-01-01T00:00:00Z",
    "valid_until": null
  }'
```

The response is `201` with the stored key, including `evidence_domain_id` and `revoked_at: null`. Registering the same `(producer_id, kid)` again returns `409 key_already_exists`. Keys are create-only: rotate by registering a new `kid`, and retire a key by revoking it. A JWK that is not `{kty: OKP, crv: Ed25519}` with a 32-byte `x`, or dates that do not parse, return `400 invalid_key`.

A key verifies a record only when, at verification time, `valid_from` has passed, `valid_until` is unset or in the future, and `revoked_at` is unset or in the future. Otherwise the record is rejected with `400 key_not_valid`. The verification result stored with each record names the exact `kid` used and the time of verification. A later revocation never changes it.

### Revoke a key

```bash
curl -s -X POST "$API/admin/producer-keys/revoke" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{ "producer_id": "cedarling-fleet-1/1.0.0", "kid": "cedarling-fleet-1-2026-01" }'
```

The response is `200` with the key and its `revoked_at`. Revoking an already revoked key is idempotent. An unknown key returns `404 record_not_found`.

### Register a producer chain

Every record belongs to a producer chain. With the default `PRE_REGISTERED` mode the chain must be registered before the producer's first record. The model is `producer_id` → `producer_instance_id` → one or more `producer_chain_id` values.

```bash
curl -s -X POST "$API/admin/producer-chains" \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{
    "producer_id": "cedarling-fleet-1/1.0.0",
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

Restarting a producer does not by itself mean a new chain. Whether a restarted producer continues its chain or opens a new one is the producer's decision. Lock only checks the records it receives.
The response is `201`. A duplicate returns `409 chain_already_exists`. A record whose chain is not registered is rejected with `400 chain_not_registered`, at every sequence position.

### List keys and chains

```bash
curl -s "$API/admin/producer-keys?producer_id=cedarling-fleet-1%2F1.0.0" -H "Authorization: Bearer $TOKEN"
curl -s "$API/admin/producer-chains" -H "Authorization: Bearer $TOKEN"
```

Both return `200`, with `{ "keys": [...] }` and `{ "chains": [...] }`. The `producer_id` filter is optional. Each chain entry carries `evidence_domain_id`, `producer_id`, `producer_instance_id`, `producer_chain_id`, `registered_by`, `genesis_kind` and `created_at`.

## Submitting records

### Single record

```http
POST /jans-lock/api/v1/audit/trace
Authorization: Bearer <access-token with trace.write>
Content-Type: application/json
```

The body is the complete signed assertion. This release accepts this shape:

```json
{
  "producer": "cedarling-fleet-1/1.0.0",
  "kid": "cedarling-fleet-1-2026-01",
  "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
  "trace": {
    "eat_profile": "tag:jans.io,2026:trace-v1",
    "event_kind": "AUTHORIZATION_DECISION",
    "signed_at": 1781138542,
    "trace_execution_id": "exec-01JABCXYZQK8P5N9F2C7R3T4V6",
    "execution_authority": "spiffe://example.org/agent/planner",
    "subject": { "workload_id": "spiffe://example.org/agent/planner" },
    "event": {
      "outcome": "ALLOW",
      "capability_ids": [
        { "capability_id": "invoke:payment-authorization", "outcome": "ALLOW" }
      ],
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
    "producer_id": "cedarling-fleet-1/1.0.0",
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

- `producer` must equal `producer_chain.producer_id` byte for byte, and `kid` is part of the signed content.
- `record_id` is a producer-generated UUID or ULID. Record identity is `(evidence domain, producer, record_id)`, so two producers can use the same `record_id`.
- `trace_execution_id` and `execution_authority` are required and travel together. Lock correlates records by them. Lock neither creates them nor authorizes the execution.
- `tokens[]` holds references only: `issuer`, `token_type`, and either `jti` or `fingerprint`. Never send a raw bearer token. Lock rejects material that looks like a JWT. Token identity is `(domain, issuer, jti)`, never a bare `jti`.
- `event_kind` is one of `AUTHORIZATION_DECISION`, `CAPABILITY_INVOKED`, `RUNTIME_EFFECT`. Each kind has its own required `trace.event` fields, and fields that contradict the kind are rejected.
- `producer_chain` can carry an optional `chain_link` member. Lock keeps it unchanged inside the signed assertion, returns it unchanged on retrieval, and does not validate or interpret it.
- The first record of a chain has `sequence_number` 1 and `prev_record_hash` set to `sha256:` followed by 64 zeros. Any other record needs the previous record's `content_digest` in `prev_record_hash`.

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
  "producer_id": "cedarling-fleet-1/1.0.0",
  "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
  "content_digest": "sha256:...",
  "receipt_sequence": 108422,
  "received_at": "2026-06-11T00:41:02.123Z",
  "idempotent_replay": false,
  "coverage_gap_flag": false,
  "chain_link_failure_flag": false,
  "equivocation_flag": false,
  "late_flag": false
}
```

`202 Accepted` means the record was verified and durably stored. Lock verifies in a fixed order: OAuth check, domain derivation, JSON structure and limits, forwarding check, key resolution, signature verification, digest, then the idempotency, chain, and equivocation checks. A record that fails verification never enters storage or any retrieval index.

| Condition | Result |
|---|---|
| Same identity, same `content_digest` | `202` with the original receipt and `idempotent_replay: true`. No second receipt is created. |
| Same identity, different `content_digest` | `409 record_conflict`. The first record is never overwritten. |
| Missing predecessor in the chain | Accepted with `coverage_gap_flag: true`. |
| Predecessor present but its digest does not match `prev_record_hash` | Accepted with `chain_link_failure_flag: true`. |
| Same chain position, different `record_id` | Both are kept and flagged `equivocation_flag: true`. |

### Bulk submission

`POST /jans-lock/api/v1/audit/trace/bulk` takes a JSON array of assertions, each in the single-record shape, with the same `trace.write` scope. Every element is processed as an independent single submission, in array order, and one element's failure never rejects the others. Resubmitting a whole array is safe: records that were already committed replay idempotently.

The response is `202` once the request-level checks pass, whatever the per-item results:

```json
{
  "results": [
    { "index": 0, "accepted": true, "record": { "accepted": true, "receipt_sequence": 108422, "...": "same body as the single-record response" } },
    { "index": 1, "accepted": false, "error": { "error": "invalid_signature", "error_description": "...", "reason": "signature_mismatch" } }
  ]
}
```

Request-level failures return an ordinary structured error and no `results`: malformed JSON, a body that is not an array, an empty array, more than `maxBulkRecords` elements, or a body over `maxBulkRequestBytes`. Receipt sequences are allocated in array order but are not guaranteed to be contiguous within a batch, because a rejected item claims no receipt.

### Error catalog

Errors use the structured body `{"error": "<id>", "error_description": "<text>", "reason": "<detail>"}`. The `reason` member appears when error reasons are enabled and names the offending field or check. OAuth failures (`401`, `403` from the protection layer) come from Lock's standard filters and are not part of this table.

| Error id | HTTP | When |
|---|---|---|
| `invalid_request` | 400 | Malformed JSON, a limit exceeded, an unreadable body, bad query parameters, or TRACE disabled (`trace_disabled`). |
| `invalid_assertion` | 400 | A schema violation in the assertion. `reason` names the field. |
| `unsupported_profile` | 400 | `trace.eat_profile` is not `tag:jans.io,2026:trace-v1`. |
| `domain_field_forbidden` | 400 | The body contains `evidence_domain_id`. |
| `producer_mismatch` | 400 | `producer` does not equal `producer_chain.producer_id`. |
| `unknown_producer_key` | 400 | No registered key for the domain, producer, and `kid`. |
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
| `invalid_key` | 400 | Admin: the JWK is not a 32-byte Ed25519 OKP key, or the dates are invalid. |
| `storage_failure` | 500 | Persistence failed, or receipt allocation ran out of retries. |
| `crypto_unavailable` | 500 | The security provider has no Ed25519 support. Lock fails closed. |

## Retrieval

Both endpoints need the `trace.readonly` scope and read only the caller's evidence domain.

```bash
# One record. producer_id should be supplied.
curl -s "$API/records/9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41?producer_id=cedarling-fleet-1%2F1.0.0" \
  -H "Authorization: Bearer $READ_TOKEN"

# The direct records of one execution. execution_authority should be supplied.
curl -s "$API/executions/exec-01JABCXYZQK8P5N9F2C7R3T4V6?execution_authority=spiffe%3A%2F%2Fexample.org%2Fagent%2Fplanner&start=0&count=100" \
  -H "Authorization: Bearer $READ_TOKEN"
```

A record response has three separate parts:

```json
{
  "producer_id": "cedarling-fleet-1/1.0.0",
  "record_id": "9f3e9e2a-6b0e-4b2c-9f6e-3a2f7b0c9d41",
  "assertion": { "...": "the producer-signed record" },
  "content_digest": "sha256:...",
  "verification": {
    "signature_valid": true,
    "key_id": "cedarling-fleet-1-2026-01",
    "verified_at": "2026-06-11T00:41:02Z",
    "algorithm": "Ed25519"
  },
  "ingestion": {
    "receipt_sequence": 108422,
    "received_at": "2026-06-11T00:41:02.123Z",
    "prev_receipt_hash": "sha256:...",
    "receipt_hash": "sha256:...",
    "coverage_gap_flag": false,
    "chain_link_failure_flag": false,
    "equivocation_flag": false,
    "late_flag": false
  }
}
```

- `assertion` is stored as the text received and never modified. The API returns it as a JSON value that is semantically identical to what the producer sent. Lock never adds resolved claims, status, or receipt data to it.
- `verification` records the admission decision Lock made at `verified_at`. It is not recomputed from the current key registry.
- `ingestion` is Lock-derived metadata.

If `producer_id` (or `execution_authority`) is omitted and the bare identifier matches more than one producer (or authority) in the domain, Lock returns `409 ambiguous_identifier`. It never guesses.

The execution response carries `execution_authority`, `trace_execution_id`, `"ordering": "receipt_sequence"`, `"completeness": "not_assessed"`, and `records`, ordered by Lock receipt sequence. `start` (default 0) and `count` (default 100, maximum 1000) page the list. The execution view contains the directly associated records only. It claims no global event order, does not traverse child executions, and does not assess completeness.

## Flags explained

Flags are Lock-derived findings about a record. Lock flags a condition instead of silently dropping or rejecting the record.

| Flag | Meaning | Cleared later? |
|---|---|---|
| `coverage_gap_flag` | The record's predecessor in its producer chain (`sequence_number - 1`) has not been seen. Records can arrive out of order. | Yes. When the missing position arrives and links correctly, the flag on its successor clears. |
| `chain_link_failure_flag` | The predecessor is present but its `content_digest` does not match this record's `prev_record_hash`. A genuine break in the chain. | No. The conflicting predecessor already exists, so later arrivals cannot resolve it. It stays visible as an integrity finding. |
| `equivocation_flag` | Two or more records with different `record_id` values claim the same chain position. Lock stores and flags all of them and never selects a winner. | No. |
| `late_flag` | Lock received the record more than `latenessThresholdSeconds` after its `signed_at`. Fixed at ingestion. | No. |

Lock updates the flags on neighboring records after it stores a new record. These updates are best-effort and eventually consistent. If one fails, Lock logs it and still accepts the new record. A reader can see a flag change shortly after a record is accepted.

A producer-chain break or gap is a statement about the records Lock has received. It is not a statement about what the producer did.

## Receipt chain

For every accepted record Lock assigns, within the evidence domain:

- `receipt_sequence`: an increasing sequence number.
- `received_at`: Lock's receipt time.
- `prev_receipt_hash`: the hash of the preceding receipt entry.
- `receipt_hash`: the hash of this entry.

The receipt hash binds `evidence_domain_id`, `receipt_sequence`, `received_at`, `producer_id`, `record_id`, `content_digest`, and `prev_receipt_hash`. The hash input is the JCS canonical form of those seven members, and `received_at` enters the hash as an integer number of epoch milliseconds. The first entry in a domain uses the all-zero sentinel as its `prev_receipt_hash`.

A verifier can walk the receipts by sequence, recompute each hash, and check each committed entry against the stored record. This makes local insertion, deletion, and rewriting detectable.

!!! warning "What the receipt chain does not protect against"

    The receipt chain is local tamper evidence only. It has no signed checkpoints and no independent external anchor. It does not protect against an operator who can rewrite the whole chain, including every hash. Do not describe the receipt chain as proof of integrity against such an operator, and do not describe TRACE evidence as settled, complete, or externally anchored.

Producer order, Lock receipt order, and causal relationships are separate things. Producer order is what one producer signed in sequence. Receipt order is when Lock accepted records. `parent_record_ids` carries the causal relationships a producer asserted. None of them is a global event clock.

## Deployment constraints

**Multi-node deployments.** The database ORM provides no transactions or sequences, so Lock allocates receipt positions using only primary-key uniqueness:

1. A node reads the highest receipt and proposes the next sequence number.
2. It inserts a `PENDING` receipt claim at that position. If another node claimed it first, the node re-reads and retries, up to `receiptAllocationRetryLimit` times, then fails with `500 storage_failure`.
3. It stores the record, then marks the claim `COMMITTED`. If storing fails, it marks the claim `VOID`.

A node that stops between steps leaves a `PENDING` claim. The repair timer settles claims older than `pendingReceiptTimeoutSeconds`: `COMMITTED` if the record exists, otherwise `VOID`. `VOID` entries keep the chain dense and verifiable, because the receipt state is not part of the receipt hash. A claim without a record is never returned by any API.

**Request size and column size.** `maxRequestBytes` limits the size of one request body. The stored assertion is kept as text in a `MEDIUMTEXT` column on MySQL and a `TEXT` column on PostgreSQL. Keep `maxRequestBytes` and the per-record limit inside the bulk path well below the size of that column type for your database, and raise the limits only after checking it. Many TRACE identifiers are stored in `VARCHAR` columns of fixed width, which Lock's validation keeps the values within.

**Time.** Lock uses `received_at` for receipt order and treats `signed_at` as a producer claim. Keep node clocks synchronized.

## Deferred features

The current release does not implement:

- gRPC transport for TRACE (REST only);
- a quarantine store for rejected records: rejected records are not stored;
- Lock-side management through the config-api Lock plugin and the Jans CLI/TUI;
- settlement assessments or Lock-signed settlement receipts;
- execution completeness or assurance profiles;
- lineage checkpoints or claims of complete producer history;
- signed receipt checkpoints or external transparency anchoring;
- Evidence Packet export;
- delegation non-expansion analysis and authorization-transition reconstruction;
- user-intent and approval records;
- operation-binding commitments, including proof-of-possession binding;
- token-claim enrichment, current-state resolution, and token-exchange lineage;
- attestation assessment or trust tiers;
- cross-domain correlation or import;
- Lock emitting TRACE records for access to its own endpoints;
- graph traversal beyond direct execution membership.

Later features are meant to be additive. They may assess, correlate, checkpoint, or package an accepted assertion, but they never rewrite it.
