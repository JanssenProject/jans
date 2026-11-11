---
tags:
  - administration
  - fido2
  - passkeys
  - audit
  - trace
---

# TRACE Producer

The TRACE producer makes jans-fido2 emit one record for each passkey registration and assertion outcome to the
Lock Server. Each record is signed and hash-chained, so a reader can detect a missing or altered record. This
page covers the configuration properties and what a record contains.

!!! note "Status"
    The configuration properties are available now. Record emission, signing, and transport are being built
    in the follow-up work tracked from [#15275](https://github.com/JanssenProject/jans/issues/15275). Until
    that work lands, setting `fido2TraceEnabled` to `true` does not change behavior.

## Configuration

All properties are set in the `fido2Configuration` object, the same place as the Lock audit properties in
[FIDO2 Configuration Parameters](fido2-server-properties-config.md#fido2-configuration-object-fido2configuration).

| Property | Type | Default | Description |
|---|---|---|---|
| `fido2TraceEnabled` | Boolean | `false` | Master switch. When `false`, nothing is emitted. |
| `fido2TraceProducerId` | String | derived | Stable producer ID. Leave empty to derive it from the server ID and version. |
| `fido2TraceSigningKey` | String | — | Key-store reference to the Ed25519 producer signing key. Never place the key inline. |
| `fido2TraceChainGenesis` | String | `first_observed` | `pre_registered` if Lock knows each node's chain genesis in advance, otherwise `first_observed`. |
| `fido2TraceIncludeCredentialIdHash` | Boolean | `true` | Include `credential_id_hash`, a hash of the credential ID, rather than the raw credential ID. |
| `fido2TraceIncludeAttestationDetail` | Boolean | `false` | Include optional signed attestation and MDS trust detail in each record. |
| `fido2TraceEndpoint` | String | discovered | Lock Server ingestion endpoint, `/api/v1/audit/trace`. |

The TRACE properties are separate from the `lockAudit*` properties. Enabling one does not enable the other.

The client that sends records uses the `trace.write` scope, requested when the FIDO Server registers as a client
through the SSA-to-DCR client-credentials flow.

## What a record contains

Each record is a `FIDO_CEREMONY` event.

- **Event kind:** `trace.event_kind` is `FIDO_CEREMONY`.
- **Ceremony:** `ceremony_type` is `REGISTRATION` or `ASSERTION`.
- **Outcome:** `ceremony_outcome` is `SUCCESS`, `FAILURE`, or `ABANDONED`.
- **Authenticator:** `authenticator_attachment` is `platform` or `cross-platform`.
- **Correlation:** `trace.session_id` is required. Ceremonies with no session do not emit a record by default.
- **Integrity:** `record_id`, `sequence_number`, `prev_record_hash`, and an Ed25519 `signature` over the
  RFC 8785 (JCS) canonical form of the record.

Records do not contain policy decisions, invocations, or capability IDs. TRACE records describe what happened
in a ceremony, and they do not affect how the ceremony is decided.

## Delivery

Records are sent asynchronously and in batches, never one HTTP call per ceremony, and they are kept off the
ceremony's hot path. Delivery is retried on transport failure.
