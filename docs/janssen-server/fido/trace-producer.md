---
tags:
  - administration
  - fido2
  - passkeys
  - audit
  - trace
---

# TRACE Producer

The planned TRACE producer makes jans-fido2 emit one record for each passkey registration and assertion outcome
that has a session, to the Lock Server. Each record is signed and hash-chained, so a reader can detect a missing
or altered record. This page covers the planned configuration properties and what a record contains.

!!! note "Status"
    This is planned behavior, not yet available. The properties are added in
    [#15680](https://github.com/JanssenProject/jans/pull/15680) and take effect only after that PR and the
    follow-up work tracked in [#15275](https://github.com/JanssenProject/jans/issues/15275) (record emission,
    signing, and transport) are merged. Until then, setting `fido2TraceEnabled` to `true` does not change behavior.

## Configuration

All properties are set in the `fido2Configuration` object, the same place as the Lock audit properties in
[FIDO2 Configuration Parameters](fido2-server-properties-config.md#fido2-configuration-object-fido2configuration).

| Property | Type | Default | Description |
|---|---|---|---|
| `fido2TraceEnabled` | Boolean | `false` | Planned. Master switch. When `false`, nothing is emitted. |
| `fido2TraceProducerId` | String | derived | Planned. Stable producer ID. Leave empty to derive it from the server ID and version. |
| `fido2TraceChainGenesis` | String | `first_observed` | Planned. `pre_registered` if Lock knows each node's chain genesis in advance, otherwise `first_observed`. |
| `fido2TraceIncludeCredentialIdHash` | Boolean | `true` | Planned. Include `credential_id_hash`, a hash of the credential ID, rather than the raw credential ID. |
| `fido2TraceIncludeAttestationDetail` | Boolean | `false` | Planned. Include optional signed attestation and MDS trust detail in each record. |
| `fido2TraceEndpoint` | String | discovered | Planned. Lock Server ingestion endpoint, `/api/v1/audit/trace`. |

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
