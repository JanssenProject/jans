---
tags:
  - administration
  - lock
  - Cedarling
  - audit
  - TRACE
---

# Cedarling TRACE

With TRACE enabled, Cedarling signs a TRACE record for each authorization decision that carries a TRACE context and sends it to Lock Server. TRACE never changes the decision, and the authorization call never waits for Lock Server.

This page covers what Cedarling needs to emit records and how to find out why records do not arrive. The properties are listed in [TRACE properties](./cedarling-properties.md#trace-properties). How an application passes the TRACE context is described in [Authorize options and TRACE context](./cedarling-authz.md#authorize-options-and-trace-context). What Lock Server does with the records is in [Lock Server TRACE](../../janssen-server/lock/trace.md).

## Requirements

- `CEDARLING_LOCK` set to `enabled`.
- A `.cjar` policy store: a local `.cjar` file, a `.cjar` URL, Lock Server or a policy store URI. A directory or YAML policy store cannot be used, because each record carries the SHA-256 of the `.cjar` Cedarling loaded.
- A signing key: `CEDARLING_TRACE_SIGNING_KEY` or `CEDARLING_TRACE_SIGNING_KEY_PEM`.
- A native build. TRACE is not supported on WASM.
- Auth Server and Lock Server set up as below.

If a requirement is not met or a TRACE property is invalid, Cedarling does not start, and the error names the property.

## Auth Server and Lock Server setup

**Scope.** Lock Server accepts records only with an access token that has the `https://jans.io/oauth/lock/trace.write` scope; `log.write` is not enough. With TRACE enabled, Cedarling asks for this scope. When Cedarling registers through DCR (see [Lock Server integration](./cedarling-lock-server.md)), add the scope to the software roles of the SSA and to the `scope_list` of the interception script that adds the Cedarling scopes. A token set with `CEDARLING_LOCK_ACCESS_TOKEN_JWT` must already carry the scope.

**Producer key.** At startup Cedarling logs its producer id, `kid` and key thumbprint. Register the key in Lock Server under them, see [Register a key](../../janssen-server/lock/trace.md#register-a-key).

**Lock Server `traceConfiguration`** (see [Lock Server TRACE configuration](../../janssen-server/lock/trace.md#configuration)):

| Setting | Set it to | Without it |
|---|---|---|
| `enabled` | `true` | no `trace_bulk_endpoint` is published, so no records are sent |
| `chainRegistrationMode` | `FIRST_OBSERVED` | every record is refused with `chain_not_registered` |
| `defaultEvidenceDomainId` | a domain id, for example `default` | every request is refused with HTTP `403` `client_not_bound` |
| `defaultAllowedProducerIds` | a list that contains `CEDARLING_TRACE_PRODUCER_ID` | every record is refused with `producer_not_allowed` |

Cedarling starts a new producer chain at every start, and Lock Server registers it from its first record only in `FIRST_OBSERVED` mode. Cedarling also registers a new client through DCR at every start, so the default evidence domain and producer list are simpler than a per-client `clientDomainBindings` entry.

To see why Lock Server refused a record, set `errorReasonEnabled: true` in Lock Server and restart it. Cedarling then logs the `reason` with the error.

## How records are sent

- The authorization call puts the record in a queue and returns. The queue holds `CEDARLING_LOCK_LOG_CHANNEL_CAPACITY` records. When it is full, for example while Lock Server is down, the record is not emitted (`not_emitted_queue_full`).
- A background emitter sends what the queue holds as bulk requests, up to `CEDARLING_TRACE_BULK_MAX_RECORDS` records and `CEDARLING_TRACE_BULK_MAX_BYTES` bytes each, as soon as it can. `CEDARLING_LOCK_LOG_INTERVAL` does not apply.
- A request that may succeed later (no answer, HTTP `5xx`, `429`, `408`, or the matching gRPC status) is retried up to `CEDARLING_LOCK_LOG_MAX_RETRIES` times, waiting 10 seconds, doubling up to 5 minutes. Then its records are dropped.
- Any other refusal is final: the records are dropped and logged at `ERROR`. The message names the setting to change where there is one, see [Troubleshooting](#troubleshooting).
- While Lock Server keeps failing, the emitter waits between requests the same way, so Lock Server gets a request every few minutes rather than one per decision. Each wait is logged at `WARN`.
- On shut down, the emitter sends what it holds once, without retries. What Lock Server does not take is reported in one `WARN` and counted in `trace.dropped`.

## Troubleshooting

| Symptom | Cause | What to change |
|---|---|---|
| `WARN` "Lock Server does not publish trace_bulk_endpoint", status `not_emitted_no_endpoint` | TRACE is disabled in Lock Server | enable `traceConfiguration.enabled`, or set `CEDARLING_TRACE_ENDPOINT` |
| `WARN` that the endpoint "must use https", status `not_emitted_no_endpoint` | the endpoint is plain `http` to a host other than `localhost` | use `https` in `CEDARLING_TRACE_ENDPOINT` or in the Lock Server `baseEndpoint` |
| HTTP `403` `client_not_bound` | no evidence domain for the Cedarling client | `defaultEvidenceDomainId` |
| HTTP `403` "Invalid token or insufficient scopes" | the token has no `trace.write` scope | add the scope, see [Auth Server and Lock Server setup](#auth-server-and-lock-server-setup) |
| HTTP `401` after one renewal, or at once with `CEDARLING_LOCK_ACCESS_TOKEN_JWT` | the token is invalid or expired; a token set by hand cannot be renewed | check the token or use DCR |
| records refused with `chain_not_registered` | Lock Server does not register chains from their first record | `chainRegistrationMode: FIRST_OBSERVED` |
| records refused with `producer_not_allowed` | the producer id is not allowed | add `CEDARLING_TRACE_PRODUCER_ID` to `defaultAllowedProducerIds` |
| records refused with `unknown_producer_key` | the key is not registered in Lock Server | register the key Cedarling logged at startup |
| `invalid_request` `max_bulk_records_exceeded` or `bulk_body_too_large` | a request is larger than Lock Server accepts | lower the setting the `ERROR` names: `CEDARLING_TRACE_BULK_MAX_RECORDS` or `CEDARLING_TRACE_BULK_MAX_BYTES` |
| `invalid_request` without a reason | Lock Server hides the reason | set `errorReasonEnabled: true` in Lock Server and read the next `ERROR` |
| gRPC `UNIMPLEMENTED` | Lock Server or its proxy has no TRACE gRPC route | update Lock Server and its proxy routes (see [Lock Server gRPC transport](../../janssen-server/lock/trace.md#grpc-transport)), or set `CEDARLING_TRACE_TRANSPORT` to `rest` |
| gRPC `RESOURCE_EXHAUSTED` | a request over the gRPC message limit, or a server or proxy that slows Cedarling down | lower `CEDARLING_TRACE_BULK_MAX_BYTES` |
| `ERROR` that Lock Server's answer cannot be read | the answer is larger than `CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES` | raise `CEDARLING_HTTP_MAX_RESPONSE_SIZE_BYTES`. Lock Server has processed the records, so they are not sent again |
| many `not_emitted_queue_full` | Lock Server is down or slower than the decision rate | check Lock Server; raise `CEDARLING_LOCK_LOG_CHANNEL_CAPACITY` to ride out short outages |
| records refused with another error id | see the [Lock Server error catalog](../../janssen-server/lock/trace.md#error-catalog) | |

## Record status and metrics

With TRACE enabled, every decision log entry has a `trace_record_status` field:

| Status | Meaning |
|---|---|
| `emitted` | The record was queued for the emitter. Whether Lock Server accepted it shows in the emitter logs and the `trace.accepted` metric. |
| `not_emitted_missing_context` | The call had no TRACE context, or a required context field was empty. |
| `not_emitted_over_limit` | A value is longer, or a list larger, than Lock Server accepts. Cedarling does not truncate signed values. The first one is logged at `WARN` with the field. |
| `not_emitted_invalid_field` | A value Lock Server rejects, such as a JWT issuer that is not `https`, or a `.cjar` whose `metadata.json` has no `policy_store.id` or `policy_store.version`. The first one is logged at `WARN` with the field. |
| `not_emitted_queue_full` | The queue to the emitter is full. |
| `not_emitted_stopped` | The emitter has stopped: Cedarling was shut down, or the emitter failed, which is logged at `ERROR`. |
| `not_emitted_no_endpoint` | There is no TRACE endpoint to send to. |

When TRACE is disabled, the field is absent.

With metrics enabled, `operational_stats` counts each status as `trace.<status>` (for example `trace.emitted`) and adds:

- `trace.accepted`: records Lock Server accepted.
- `trace.rejected`: records Lock Server refused for good.
- `trace.dropped`: records given up: after their retries, on shut down, or with a producer chain Lock Server lost.
- `trace.sign_failed`: records that could not be signed.
- `trace.chains_opened`: producer chains started again because Lock Server lost or did not accept the previous one.

A counter appears only after it is first incremented.

## Tokens in a record

A record lists the tokens an `authorize_multi_issuer` decision used, by issuer, token type and `jti`. A token reported in [`dropped_tokens`](./cedarling-logs.md) is not listed. A decision that used more than 32 tokens gets no record (`not_emitted_over_limit`).

- A JWT is listed with its `iss`. Lock Server accepts only an `https` issuer, or `http` with host `localhost`, so a decision that uses a JWT with another issuer gets no record (`not_emitted_invalid_field`).
- A token from a [custom token processor](./cedarling-multi-issuer.md#custom-non-jwt-token-processing) is listed with the issuer `urn:jans:cedarling:custom-issuer:<issuer_id>`, where `<issuer_id>` is the lowercased [`issuer`](./cedarling-policy-store.md#trusted-issuersjson) of its token type in the policy store.
