# Trust Activation API — Implementation Guide

> **Issue draft.** Restates the contract for the implementer; it will drift. Source of truth:
> `openapi/trust-activation/trust-activation-api.yaml` and the Java public interfaces named below.
> Read [`README.md`](./README.md) first for the shared model, lifecycle, and `Result`/`problem+json`
> conventions.

## What this API is

The **machine-to-machine** side. Base path `/v1/trust/activation`, **worker-audience** bearer token
(distinct from the config user token). Its consumers are **worker processes** that reconcile a trust
relationship's desired state against the actual IdP. Through it a worker announces liveness, claims a
unit of work, renews its claim, and reports the outcome.

Every operation is a thin shell over one application service:
`io.jans.shibboleth.trust.activation.coordination.WorkOrchestrator`. The orchestrator owns the work
queue, the leases, and the fences. **Coordination conflicts are resolved by the domain's own fences,
never by HTTP preconditions/ETags** — they surface as `409`.

## The coordination model, plainly

Two primitives give correctness with a pool of any size (single-worker is just a pool of one):

- **Leases** handle crashes and silence. A worker must hold a live, time-bounded lease to work an
  item; heartbeats renew it. If a worker goes silent, its lease expires and the item is reclaimed
  (`ASSIGNED → PENDING`) for another worker. This is what makes processing *at-least-once*.
- **Identity fences** handle stale reports. Each activation episode gets its own **work item**; the
  orchestrator tracks which work item is *current* for a trust relationship and accepts a report only
  from the current item **and** the current lease holder. This stops a slow worker from an old episode
  from finalizing a newer one against different metadata.

Vocabulary: a **work item** is one unit of activation work per episode
(`PENDING → ASSIGNED → COMPLETED`, plus `CANCELLED`; lease expiry reclaims `ASSIGNED → PENDING`). A
**worker** is an ephemeral process whose liveness is derived from its last heartbeat, identified by an
`origin` string `"instance@host"`.

## Endpoint catalog

| Method | Path | Operation |
|---|---|---|
| `POST` | `/workers` | Register / re-announce a worker (idempotent). |
| `POST` | `/workers/{worker}/heartbeat` | Renew a worker's liveness (`{worker}` = URL-encoded origin). |
| `POST` | `/work-items/claim-next` | Atomically claim the oldest available item of a type. |
| `GET` | `/work-items/{id}` | Read a work item. |
| `POST` | `/work-items/{id}/heartbeat` | Renew the lease on an assigned item. |
| `POST` | `/work-items/{id}/report` | Report an activation outcome. |

## Public interfaces you call

**`WorkOrchestrator`** — build it once at boot via its factory (returns a `Result`, so check it):

```java
Result<WorkOrchestrator> orch = WorkOrchestrator.create(
    timeSource,     // TimeSource            → Instant now()  (system clock in production)
    leaseTtl,       // Duration              (configurable)
    heartbeatTtl,   // Duration              (configurable)
    events,         // ActivationEventSink   → void emit(ActivationEvent)   (best-effort; no-op/log ok for now)
    finalizePort,   // FinalizeActivationPort→ the cross-context bridge (below)
    workItemRepo, leaseRepo, workerRepo);   // the three repository ports, wired from jans-orm impls
```

Endpoint → orchestrator method:

| Endpoint | Orchestrator call |
|---|---|
| `POST /workers` | `registerWorker(WorkerId)` → `200` `WorkerView` |
| `POST /workers/{worker}/heartbeat` | `heartbeatWorker(WorkerId)` → `200` / `404 worker_not_found` |
| `POST /work-items/claim-next` | resolve origin via `findWorker(WorkerId)`, then `claimNext(WorkItemType, Worker)` |
| `GET /work-items/{id}` | `find(WorkItemId)` → `WorkItemView` |
| `POST /work-items/{id}/heartbeat` | `findWorker` → `heartbeat(WorkItemId, Worker)` |
| `POST /work-items/{id}/report` | `report(WorkItemId, ActivationDiagnostics)` |

**Mappers** (`io.jans.shibboleth.trust.dto.mapper.activation`, all `public static`):
`WorkerMapper.toWorkerId(String origin)` (blank → `RequiredValueMissing`), `WorkerMapper.toView(Worker)`,
`WorkItemMapper.toView(WorkItemActivation)`, `ActivationDiagnosticsMapper.toDomain(ActivationDiagnosticsRequest)`.

**Not endpoints — server-driven, wire internally:** `onActivationRequested(...)` and
`onActivationCancelled(...)` are raised by the *config* side when a relationship is activated/cancelled;
`sweepExpiredLeases()` runs on a timer. Do not expose these over HTTP.

### The cross-context bridge: `FinalizeActivationPort`

This is the one non-trivial adapter you must write. When a worker reports success, the orchestrator
calls `finalizeActivation(TrustRelationshipRef, ActivationDiagnostics)`, which must apply the result to
the **config** aggregate:

```java
(ref, diagnostics) -> {
    Result<TrustRelationship> tr = trustRelationshipRepository.findById(Id.of(ref.value()));
    if (tr.isFailure()) return;                                   // port returns void; log if you like
    Result<TrustRelationship> done = tr.getValue().finalizeActivation(diagnostics);
    if (done.isSuccess()) trustRelationshipRepository.save(done.getValue());
};
```

Keep it thin and one-directional: activation carries a `TrustRelationshipRef` (a UUID wrapper); config
keys on `Id.of(uuid)`. This is the only place the two contexts meet.

## Endpoint notes & gotchas

- **`claim-next` with nothing to claim is `204`, not an error.** `claimNext` returns a `ClaimOutcome`;
  when it's empty, respond `204`. When claimed, `200` + `WorkItemView`. A non-live worker → `409
  worker_not_alive`.
- **`report` with `status = NO_DATA` is a no-op** that leaves the item `ASSIGNED`; any other status
  (`SUCCEEDED`/`FAILED`) completes it. The request body carries the worker's `origin` — the server
  fences it against the current work item and lease holder.
- **Fences are the concurrency control.** The `{id}` in the path is the episode fence; the reporting
  worker's `origin` is the lease-holder fence. Map violations to `409` (`stale_report`,
  `not_lease_holder`).
- **Accessor style differs from config.** Activation model types use bare-noun accessors
  (`workItem.id()`, `lease.expiresAt()`, `worker.registeredAt()`), not JavaBean `getX()`.

## Error → HTTP mapping

| Domain error | HTTP | `code` |
|---|---|---|
| `WorkItemNotFound` | `404` | `work_item_not_found` |
| `WorkerNotFound` | `404` | `worker_not_found` |
| `WorkItemTransitionNotAllowed` | `409` | `work_item_transition_not_allowed` |
| `WorkerNotAlive` | `409` | `worker_not_alive` |
| `NotLeaseHolder` | `409` | `not_lease_holder` |
| `LeaseAlreadyHeld` / `LeaseStillValid` / `LeaseNotPresent` | `409` | `lease_already_held` / `lease_still_valid` / `lease_not_present` |
| `StaleReport` | `409` | `stale_report` |
| `RequiredValueMissing`, malformed body | `400` | `required_value_missing` |
| missing/invalid worker token | `401` | — |

All coordination conflicts are `409` (state/fencing, not auth).

## Worked example: a worker processes one episode

```http
POST /v1/trust/activation/workers
{ "origin": "worker-1@host" }
```
```http
POST /v1/trust/activation/work-items/claim-next
{ "origin": "worker-1@host", "type": "PROCESS_INDIVIDUAL_METADATA" }
```
```http
200 OK
{ "id": "9c1e…", "type": "PROCESS_INDIVIDUAL_METADATA",
  "trust_relationship_ref": "7f3a…5e6f", "state": "ASSIGNED",
  "lease_expires_at": "2026-08-12T12:05:00Z" }
```
The worker does the slow metadata work, heartbeating to keep the lease while it runs:
```http
POST /v1/trust/activation/work-items/9c1e…/heartbeat
{ "origin": "worker-1@host" }
```
Then reports the outcome — which finalizes the episode and, via `FinalizeActivationPort`, flips the
config relationship to `ACTIVE`:
```http
POST /v1/trust/activation/work-items/9c1e…/report
{ "origin": "worker-1@host", "status": "SUCCEEDED",
  "started_at": "2026-08-12T12:00:05Z", "completed_at": "2026-08-12T12:01:30Z",
  "log_entries": [ { "timestamp": "2026-08-12T12:01:29Z", "level": "INFO", "message": "3 endpoints validated" } ] }
```
A second worker that raced `claim-next` for the same item gets `204` (nothing left to claim); a late
report from a superseded episode gets `409 stale_report`.

## Provisioning

The installer must create, under `o=jans` (the `WorkItem` repository takes **two** base DNs — the
items branch *and* the current-episode pointer branch):

| Aggregate | Object class / table | Branch |
|---|---|---|
| Work item | `jansTrustActivationWorkItem` | `ou=trustActivationWorkItems,o=jans` |
| Current-episode pointer | `jansTrustActivationEpisode` | `ou=trustActivationEpisodes,o=jans` |
| Lease | `jansTrustActivationLease` | `ou=trustActivationLeases,o=jans` |
| Worker | `jansTrustActivationWorker` | `ou=trustActivationWorkers,o=jans` |

Canonical reference DDL: `docker/init-scripts/01-activation-init.sql`.
