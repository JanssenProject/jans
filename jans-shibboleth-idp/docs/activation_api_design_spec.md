# Activation API — Design Directives (machine-to-machine)

> **Status:** design / pre-implementation. This is the activation-context counterpart of
> [`openapi_design_spec.md`](./openapi_design_spec.md) (the config API). It governs the **machine-to-machine
> (worker) API** for the `activation` bounded context. The domain protocol it exposes is defined by
> [`asynchronous_activation.md`](./asynchronous_activation.md) and the code under
> `trust-domain/.../activation/`.

---

## 1. Purpose & scope

Expose the **worker protocol** of the `WorkOrchestrator` as a REST API: workers announce liveness, claim
activation work, renew their lease, and report results. The orchestrator is the server (it owns the
queue and the authoritative "current work item per trust relationship" pointer).

**Out of scope** (per the domain doc §3/§7/§11 and by decision):
- **Scheduling** — `sweepExpiredLeases()` is an internal scheduled job, **not** an endpoint.
- **Config → activation integration** — `onActivationRequested`/`onActivationCancelled` are driven by the
  *config* context (on TR `activate()`/`cancelActivation()`), not by workers. They are handled by domain
  events / internal integration, **not** this API (AA1).
- **Persistence & transport** of work items, workers and heartbeats (infra).
- The TR aggregate itself — addressed only by the opaque `TrustRelationshipRef`; finalization crosses
  back to the config context via `FinalizeActivationPort` (server-side, not over this API).

---

## 2. Reference documents (source of truth)

| # | Document | Authoritative for |
|---|----------|-------------------|
| 1 | `trust-domain/.../activation/` source | The operation surface (`WorkOrchestrator`), model, fences, errors |
| 2 | [`asynchronous_activation.md`](./asynchronous_activation.md) | Ubiquitous language, actors, lifecycle, fence-token rationale |
| 3 | [`openapi_design_spec.md`](./openapi_design_spec.md) | Shared conventions (below) |

---

## 3. Relationship to the config API

**Shared, inherited from the config spec:** `snake_case` bodies (D6); RFC 7807 `problem+json` errors (D5)
with `code` under `https://jans.io/shibboleth-idp/problems/{code}` (D12); OpenAPI 3.1 (D9); the
`/v1/trust/{context}/…` path scheme (D13) — here the context is **`activation`**; bearer auth (D7).

**Deliberately different for this API:**
- **Consumers are worker processes**, not humans/UI.
- **Auth audience/scope is worker-specific** (AA6) — a worker token, distinct from the config user token.
- **Concurrency is the domain's own fences, never HTTP ETag** (AA2) — see §5.

---

## 4. Decisions (settled)

| # | Decision | Rationale |
|---|----------|-----------|
| AA1 | **Worker-facing scope only.** Endpoints cover the worker protocol (register, heartbeat, claim-next, get, renew, report). `sweepExpiredLeases` stays an internal cron; config→activation demand/cancel is out-of-API (events/internal). | The doc marks demand-raising and scheduling out of scope; their callers aren't workers. |
| AA2 | **Concurrency via domain fences, not ETag.** The `WorkItemId` in the path is the episode fence token; the lease-holder fence is the reporter's `WorkerId` carried in `ActivationDiagnostics.origin`. Coordination conflicts → `409`. | The domain already fences (cross-episode identity + within-episode lease ownership); HTTP optimistic-lock would be redundant and weaker. |
| AA3 | **Explicit worker resource for liveness.** `POST /workers` (register) + `POST /workers/{worker}/heartbeat`, backed by `Worker.register`/`heartbeat`/`isAlive`. Worker identity is its `Origin` (`instance@host`). | `claim` requires the worker be `isAlive`; a distributed server must track worker liveness, which the in-memory orchestrator leaves to transport. |
| AA4 | **Atomic `claim-next`.** `POST /work-items/claim-next` (body: worker + `WorkItemType`) finds and claims one `PENDING` item, returning it or empty. | `claim` targets a specific id and there is no "list PENDING" query; a single atomic op avoids thundering-herd races and matches the CAS-on-state claim. |
| AA5 | **Shared OpenAPI schemas** in `openapi/components/common.yaml` — the `problem+json` `Problem` and `Violation` schemas — referenced by both specs via cross-file `$ref`. The generic error **responses** (`BadRequest`/`Unauthorized`/`NotFound`/`Conflict`) and the `bearerAuth` **securityScheme** stay **local** in each spec: responses are tiny, per-spec-worded wrappers that just `$ref` the shared `Problem` schema (keeping operations referencing *local* responses, which is more legible); and a `security:` requirement can only reference a scheme in the same document (OpenAPI constraint). | One source of truth for the error *shape*, without forcing cross-file `$ref` on every operation or fighting the security-scheme locality rule. |
| AA6 | **Worker-audience bearer token.** Same `bearerAuth` HTTP scheme, but the token identifies a worker (distinct audience/scope from the config user token). Binding the token subject to the presented `Origin` is a hardening step (noted, not blocking). | Workers are machines, not users. |

---

## 5. Concurrency & fencing (the heart of this API)

No `If-Match`/ETag. Correctness comes from the domain's two fences, surfaced as follows:

- **Episode fence (cross-episode, load-bearing):** the `{id}` path segment is the `WorkItemId`. A `report`
  is applied only if that id is the trust relationship's *current* work item; otherwise the domain returns
  `StaleReport` → **`409`**. This blocks a slow worker from a previous `ACTIVATING` episode finalizing a
  later one.
- **Lease-ownership fence (within-episode):** `report`/`heartbeat` succeed only for the current lease
  holder. The holder is the `WorkerId` derived from `ActivationDiagnostics.origin` (report) or the
  authenticated worker (heartbeat). A non-holder → `NotLeaseHolder` → **`409`**.
- **Lease lifecycle:** `claim` grants a lease (`now → now + leaseTtl`) and moves `PENDING → ASSIGNED`;
  `heartbeat` renews it; silence lets it expire and an internal sweep reclaims `ASSIGNED → PENDING`
  (same id, same episode) for re-offer. Delivery is **at-least-once** processing, **effectively-once**
  finalization.
- **`NO_DATA` report** is a no-op: finalization is invoked but the item stays `ASSIGNED` (mirrors the TR's
  "no data → stays ACTIVATING" rule). Any non-`NO_DATA` report completes the item (`COMPLETED`, terminal).

### Error → HTTP mapping

| Domain error | HTTP | `code` |
|---|---|---|
| `WorkItemNotFound` | `404` | `work_item_not_found` |
| `WorkerNotFound` | `404` | `worker_not_found` |
| `WorkItemTransitionNotAllowed` | `409` | `work_item_transition_not_allowed` |
| `WorkerNotAlive` | `409` | `worker_not_alive` |
| `NotLeaseHolder` | `409` | `not_lease_holder` |
| `LeaseStillValid` / `LeaseNotPresent` | `409` | `lease_still_valid` / `lease_not_present` |
| `StaleReport` | `409` | `stale_report` |
| `RequiredValueMissing`, malformed body | `400` | `required_value_missing` |
| missing/invalid worker token | `401` | — |

All coordination conflicts are `409` (they are state/fencing conflicts, not auth failures). `claim-next`
returning "nothing to claim" is **not** an error — see the catalog.

---

## 6. Endpoint catalog

Effective base: **`/v1/trust/activation`** (D13). All operations require the worker `bearerAuth` (AA6).
Tick `Done` when spec + DTOs + mappers + passing tests are complete (same bar as the config spec §4).

**Workers**

- [x] **Register worker** — `POST /workers` (`registerActivationWorker`) — `WorkOrchestrator.registerWorker`. Body `{ origin }` (required, non-blank). Idempotent announce/renew. `200` → `WorkerView` (`{ origin, registered_at, last_heartbeat_at }`); `400`/`401`. DTOs `RegisterWorkerRequest`/`WorkerView`; mapper `WorkerMapper.toWorkerId`/`toView`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md).
- [x] **Worker heartbeat** — `POST /workers/{worker}/heartbeat` (`heartbeatActivationWorker`) — `WorkOrchestrator.heartbeatWorker`. `worker` path segment = the worker's `Origin` ("instance@host", URL-encoded). Keeps the worker `isAlive` (independent of any lease). `200` → `WorkerView`; `400`/`401`/`404` (`worker_not_found`). Mapper `WorkerMapper.toWorkerId(String)` (blank → `RequiredValueMissing`); response via `WorkerMapper.toView`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). *No domain addition needed (P2).*

**Work items**

- [x] **Claim next** — `POST /work-items/claim-next` (`claimNextActivationWorkItem`) — atomic find-and-claim of one `PENDING` item (AA4) via `WorkOrchestrator.claimNext` (P1). Body `{ origin, type }` (`WorkItemType`). The endpoint resolves the presented `origin` to a registered worker via `findWorker` (authoritative liveness). `200` → `WorkItemView`; **`204`** (empty `ClaimOutcome`) when nothing is claimable; `400`; `401`; `404` `worker_not_found` (unknown worker); `409` `worker_not_alive`. DTO `ClaimNextRequest`; no dedicated mapper (reuses `WorkerMapper.toWorkerId` + `WorkItemMapper.toView`); tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md).
- [x] **Get work item** — `GET /work-items/{id}` (`getActivationWorkItem`) — `WorkOrchestrator.find`. `200` → `WorkItemView`; `401`/`404`. DTO `WorkItemView`; mapper `WorkItemMapper.toView`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). *No domain addition needed.*
- [x] **Renew lease** — `POST /work-items/{id}/heartbeat` (`renewActivationLease`) — `WorkOrchestrator.heartbeat`. Body `{ origin }`. The endpoint resolves `origin` to a registered worker via `findWorker`, then renews the lease on the work item. `200` → `WorkItemView`; `400`; `401`; `404` (`work_item_not_found` / `worker_not_found`); `409` (`work_item_transition_not_allowed` / `not_lease_holder`). DTO `RenewLeaseRequest`; no dedicated mapper (reuses `WorkerMapper.toWorkerId` + `WorkItemMapper.toView`); tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). *No domain addition needed (P2 + existing `heartbeat`).*
- [x] **Report result** — `POST /work-items/{id}/report` (`reportActivationResult`) — `WorkOrchestrator.report`. Body `ActivationDiagnosticsRequest` (`origin` = reporting `WorkerId`, `status`, timestamps, log entries). `200` → `WorkItemView` (`COMPLETED`, or unchanged `ASSIGNED` for `NO_DATA`); `400`/`401`/`404`/`409` (`stale_report` / `not_lease_holder`). DTOs `ActivationDiagnosticsRequest`/`ActivationLogEntryRequest`; mapper `ActivationDiagnosticsMapper.toDomain` (builds domain diagnostics; response via `WorkItemMapper.toView`); tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). *No domain addition needed.*

### DTO sketches (finalized at each endpoint's schema-review)

- **`WorkItemView`** — `{ id, type, trust_relationship_ref, state, lease_expires_at (date-time, nullable) }`. Read-only; no gaps (all accessors exist).
- **`WorkerView`** — `{ origin, registered_at, last_heartbeat_at }`.
- **`ActivationDiagnosticsRequest`** (report body) — `{ origin, status (NO_DATA|SUCCEEDED|FAILED), started_at, completed_at, log_entries: [{ timestamp, level, message }] }`. `origin` is the fence identity. (The config API already has the read-side `ActivationDiagnosticsDto`; this is the write-side counterpart — separate, per read/write separation.)

---

## 7. Prerequisites — small domain additions (design-first, per confirmed decisions 2 & 3)

The in-memory `WorkOrchestrator` doesn't yet expose two things a distributed worker API needs. These are
domain additions to design **before/with** the endpoints that depend on them:

- **P1 — "claim next PENDING of type" (for AA4 `claim-next`).** ✅ **Done.** `WorkOrchestrator.claimNext(WorkItemType, Worker)`
  → `Result<ClaimOutcome>` atomically selects the **oldest** `PENDING` item of the type (FIFO by
  `createdAt`, no starvation) and claims it via the existing `claim` (same assign + `WorkItemAssigned`
  event). `ClaimOutcome` is a null-object value (like `Lease.NONE`): `isClaimed()`/`isEmpty()`/`workItem()`.
  An empty outcome means nothing is claimable (**not** a failure — it maps to `204`); `WorkerNotAlive`
  → `409`; missing arg → `RequiredValueMissing` → `400`. `Optional` is deliberately avoided in the domain.
  12 domain tests added (8 `claimNext` + 4 `ClaimOutcome`).
- **P2 — worker registry / liveness (for AA3).** ✅ **Done.** `WorkOrchestrator` now owns a
  `Map<WorkerId, Worker>` with `registerWorker(WorkerId)`, `heartbeatWorker(WorkerId)` and
  `findWorker(WorkerId)` (all `Result<Worker>`), plus a new `WorkerNotFound` error (→ `404`). `claim`
  and `heartbeat` are unchanged — the API resolves a presented `WorkerId` via `findWorker` (authoritative
  liveness) and passes the registered `Worker` in. 6 domain tests added.
- **P3 — (only if needed)** a read query for a worker's currently-held items, should a "what am I working
  on" endpoint be wanted later. Not in the initial catalog.

These are analogous to the small, TDD'd domain additions we made for the config API (`EntityIds.getEntityIds`,
`ValidityPeriod.until`, the adapter parse errors) — added with tests, in the domain module.

---

## 8. Module layout

Same `trust-dto` module. The activation DTOs/mappers live under the `activation` sub-package that
`directory_structure_decisions.md` already reserved:

```
trust-dto/src/main/
├── java/io/jans/shibboleth/trust/dto/
│   ├── activation/         WorkItemView, WorkerView, ActivationDiagnosticsRequest, …
│   └── mapper/activation/  WorkItemMapper, …
└── resources/openapi/
    ├── trust-config-api.yaml       (config; migrated to reference components/common.yaml)
    ├── trust-activation-api.yaml    (this API)
    └── components/common.yaml       (shared Problem/Violation + error responses)
```

Workflow is identical to the config spec (§6 there): one endpoint at a time, schema-review checkpoint,
TDD for DTOs/mappers, tick the box.
