# TrustRelationship Asynchronous Activation — Design

> **Test plan:** see the companion [`asynchronous_activation_tests.md`](./asynchronous_activation_tests.md),
> which drives an incremental, test-first implementation of this design.

> **Status:** design / pre-implementation. This document models the *domain* of asynchronous
> activation in DDD terms (ubiquitous language, aggregates, invariants, lifecycles, events,
> concurrency). Infrastructure choices (persistence, transport, scheduling) are called out as
> **out of scope** but their domain-facing contracts are specified.

---

## 1. Background & Purpose

`TrustRelationship.activate()` does **not** move a TR straight to `ACTIVE`. On success it enters the
transient `ACTIVATING` state; it only reaches `ACTIVE` after out-of-process **validation / metadata
processing** completes and is reported back via `finalizeActivation(ActivationDiagnostics)`
(→ `ACTIVE` on success, → `READY` on failure, → stays `ACTIVATING` on no data).

That validation is **not configuration** — it is potentially memory/CPU-intensive and slow (large or
slow-to-reach metadata sources). It must therefore run **asynchronously and in parallel** with, and
decoupled from, TR configuration. This document defines the domain that coordinates that work.

---

## 2. Ubiquitous Language

Activation work comes in two kinds — `WorkItemType.PROCESS_AGGREGATE_METADATA` and
`PROCESS_INDIVIDUAL_METADATA`, selected from the TR's metadata source (an aggregate of entities vs. a single
entity's metadata) — handled by the existing `Worker` / `WorkItem` / `WorkOrchestrator` vocabulary and
reported through `Origin`.

| Term | Meaning | Code |
|------|---------|------|
| **TrustRelationship (TR)** | The aggregate being activated. Owns its own state machine (`activate`, `finalizeActivation`, `cancelActivation`). | `model/TrustRelationship` |
| **WorkItem** | The unit of activation work for exactly one TR — one per activation episode. The coordination aggregate whose lifecycle *is* the episode (spanning any claim/reclaim of Workers within it). | `activation/model/WorkItem` |
| **Worker** | A process that claims and executes `WorkItem`s. Ephemeral; has identity and liveness. | `activation/workers/Worker` |
| **WorkOrchestrator** | Domain/application service that turns activation demand into `WorkItem`s and routes results back to the TR. Owns the "queue". | `activation/coordination/WorkOrchestrator` |
| **WorkerId** | Stable identity of a `Worker` instance; wraps the shared `Origin` (`"instance@host"`). | `activation/workers/WorkerId` over `shared/Origin` |
| **WorkItemId** | Activation-owned identity of a `WorkItem`; also the episode fence token (§7b). | *new* (`activation.model`) |
| **Lease** | A time-bounded, exclusive claim by a `Worker` over a `WorkItem`. Renewed by heartbeats; expires if the Worker goes silent. | *new* |
| **ActivationDiagnostics** | The result value object a `Worker` reports (status, origin, log entries, timings). | `model/core/diagnostics/ActivationDiagnostics` |

---

## 3. Bounded Contexts & Boundaries

Two **bounded contexts**, each in its own package tree:

1. **Trust Configuration** (`io.jans.shibboleth.model`) — the `TrustRelationship` aggregate. It knows
   *nothing* about Workers, queues, or leases. Its only activation surface is the methods `activate()`,
   `finalizeActivation(...)`, `cancelActivation()`.
2. **Activation Coordination** (`io.jans.shibboleth.activation`) — `WorkOrchestrator`, `WorkItem`, `Worker`,
   leases, in three packages layered to stay acyclic (`workers ← model ← coordination`): `activation.workers`
   for the `Worker` and its `WorkerId` (leaf); `activation.model` for the `WorkItem` aggregate and its value
   objects (references `WorkerId`); `activation.coordination` for the `WorkOrchestrator`, `TimeSource`, and the
   supporting coordination services. It observes that a TR *wants* activation, drives Workers to satisfy it,
   then feeds the result back through the TR's existing method.

> **DDD rule:** the TR aggregate must **not** reference the coordination context, and the coordination
> context never holds the TR aggregate — it addresses the TR by an **opaque identity value** and interacts
> only through the TR's published contract. The two are updated in **separate transactions** and reconciled
> by **domain events** (§6) — *eventual consistency*, not a shared transaction.

> **Shared contract, not shared model.** `ActivationDiagnostics` (carrying `ActivationStatus`, log entries,
> and a shared `Origin`) is the payload of `TrustRelationship.finalizeActivation(...)`, so it belongs to
> **Trust Configuration** (`io.jans.shibboleth.model.core.diagnostics`) as that method's published contract. Activation Coordination
> *produces* it and hands it back; it is deliberately **not** moved into `io.jans.shibboleth.activation`,
> since that would force the TR aggregate to depend on the coordination context and break the boundary above.

> **Identity is context-owned.** Activation Coordination defines its **own** identity types (`WorkItemId`,
> `WorkerId`) and does **not** reuse Trust Configuration's `Id`. A `WorkItem`'s link to its TR is held as an
> **opaque value** (a raw `UUID` or a thin `TrustRelationshipRef`), so the activation *domain* imports nothing
> from the trust context; the `WorkOrchestrator` — an application-layer component aware of both contexts —
> resolves that value to the TR's `Id` only at the boundary, when it invokes `finalizeActivation`. (Reusing
> `model.core.Id` is rejected precisely because it is entangled with `TrustResult` / `TrustError`, which would
> drag trust-context infrastructure into the coordination domain.)

> **Shared kernel.** `Origin` — the neutral `"instance@host"` identity — is used by *both* contexts
> (`ActivationDiagnostics` carries one; `WorkerId` is built on one), so it lives in a shared kernel,
> `io.jans.shibboleth.shared`, that both may depend on without either depending on the other. It qualifies
> because it is fully self-contained (JDK-only, no trust infrastructure). `Id` does **not** qualify — it is
> entangled with `TrustResult` / `TrustError` — and therefore stays trust-local. The rule is the same in both
> cases: **share only clean, neutral types.**

---

## 4. Domain Model

### 4.1 `WorkItem` — coordination aggregate (one per activation episode)

Fields: `id` (`WorkItemId` — **also the fence token**, §7b), `type` (`PROCESS_AGGREGATE_METADATA` or
`PROCESS_INDIVIDUAL_METADATA`), `trustRelationshipId` (an **opaque TR reference** — raw `UUID` / thin `TrustRelationshipRef`, **not** the trust `Id`; §3), `state`, `lease` (`Lease`; the sentinel `Lease.NONE` when unassigned — see below),
`createdAt`, `lastTransitionAt`. One `WorkItem` per episode makes the `id` itself the fence (§7b) — there is
no separate fencing counter — and there is no successor link: authority is the orchestrator's "current
`WorkItem` for this TR" pointer, and episode history is recoverable from `trustRelationshipId` + `createdAt`.

> **Lease presence — null-object modelling (Java 11).** The lease is the only conditional piece of
> `WorkItem` state: absent in `PENDING`, present in `ASSIGNED`. It is modelled with the **null-object**
> pattern — a sentinel `Lease.NONE` instance stands for "no lease" — so `WorkItem.lease` is always a total,
> non-null `Lease` (`Lease.NONE` while `PENDING`, a real lease while `ASSIGNED`). A `WorkItem` toggles
> `Lease.NONE → <lease> → Lease.NONE` as it is claimed and reclaimed (§5.1); no field is ever `null` and no
> caller unwraps an `Optional`. `Lease` exposes `isNone()` / `isPresent()` so call-sites read intent rather
> than compare against the sentinel. (Sealed types and records are unavailable on the Java 11 target, so the
> null object is the idiomatic choice.)

The `WorkItem` is the **consistency boundary** for "who owns this activation and is that claim current."
All ownership/lease transitions happen atomically on this aggregate.

### 4.2 `Worker` — entity (ephemeral)

Fields: `id` (`WorkerId`), `registeredAt`, `lastHeartbeatAt`. Liveness is derived: a Worker is **alive**
iff `now - lastHeartbeatAt ≤ heartbeatTtl`. Workers "pop in and out of existence," so they are tracked
transiently, keyed by `WorkerId`.

### 4.3 Value objects

- **`WorkItemId`** — activation-owned identity of a `WorkItem`; also the episode fence token (§7b). The
  context defines its own identity type rather than reusing the trust `Id` (§3).
- **TR reference** — a `WorkItem`'s link to its TR is an **opaque value** (raw `UUID` / thin
  `TrustRelationshipRef`), resolved to the trust `Id` only at the orchestrator boundary (§3).
- **`WorkerId`** — unify with `Origin`. The `ActivationDiagnostics.origin` a Worker already reports *is*
  its identity; don't introduce a second identity concept.
- **`Lease`** — `{ workerId, grantedAt, expiresAt }`. Immutable; renewed by producing a new `Lease`.
- **Fence token** — the `WorkItemId` distinguishes activation episodes: one `WorkItem` per episode (§7b).
- **`ActivationDiagnostics`** — reused unchanged as the report payload.

### 4.4 Invariants

- A `WorkItem` references exactly one `TrustRelationship` and has at most **one** active `Lease`
  (→ "a TR is assigned to at most one Worker" — enforced structurally, not by convention).
- A `WorkItem` in `ASSIGNED` carries a real, non-expired `Lease`; in `PENDING` its `lease` is `Lease.NONE`.
  Neither state uses `null` or `Optional`.
- A Worker may hold **many** `WorkItem`s concurrently (one-Worker-to-many-WorkItems), but each `WorkItem`
  names a single `workerId`.
- A report is applied only if its `WorkItem` is the TR's **current** one (identity fence, §7b); reports
  naming a cancelled or completed `WorkItem` — or a prior episode's `WorkItem` — are discarded.
- A `WorkItem` reaches a terminal state (`COMPLETED`/`CANCELLED`) at most once.

---

## 5. Lifecycles

### 5.1 `WorkItem` state machine

```
               claim(worker)                report(diagnostics)
   PENDING ────────────────────► ASSIGNED ──────────────────────► COMPLETED   (terminal)
   │ ▲                           │    │    self-loop on ASSIGNED: heartbeat renews the lease
   │ └─ lease expired / reclaim ─┘    │
   │                                  │
   │ cancelActivation()               │ cancelActivation()
   ▼                                  │
   CANCELLED ◄────────────────────────┘   (terminal)
```

> A single `WorkItem` lives for the whole activation episode. Lease expiry is **not** terminal: the item is
> reclaimed — lease cleared to `Lease.NONE`, `ASSIGNED → PENDING` — and offered for re-claim (the diagram's
> "lease expired / reclaim" arrow). The item becomes terminal only via **`COMPLETED`** (report applied — the
> `ActivationDiagnostics` is carried through to `finalizeActivation`) or **`CANCELLED`** (TR
> `cancelActivation()`). The state set is **`PENDING / ASSIGNED / COMPLETED / CANCELLED`**, extending the
> existing `WorkItemState` with `CANCELLED`.

### 5.2 `Worker` liveness

`REGISTERED → (heartbeat)* → EXPIRED`. An expired Worker's held `WorkItem`s are reclaimed (their leases
expire), returning them to `PENDING` for reassignment. No explicit "deregister" is required for
correctness — silence + lease TTL is sufficient — though a graceful `release()` is a nice-to-have.

### 5.3 How coordination drives the TR

`ACTIVATING` (TR) ── Worker reports ──► `WorkOrchestrator` calls `finalizeActivation(diagnostics)` ──►
`ACTIVE` (success) / `READY` (failure) / stays `ACTIVATING` (no data). The TR's existing
finalize-outcome rules are the contract; coordination never invents new TR states.

---

## 6. Domain Events

Coordination and configuration communicate through events, not direct calls:

| Event (raised by) | Reaction |
|-------------------|----------|
| **`ActivationRequested`** — TR entered `ACTIVATING` (raised by the Trust Configuration side on a successful `activate()`) | `WorkOrchestrator` creates a `WorkItem(PENDING, type, trustRelationshipId)`, where `type` is `PROCESS_AGGREGATE_METADATA` or `PROCESS_INDIVIDUAL_METADATA` per the TR's metadata source. This *is* the "queue". |
| **`WorkItemAssigned`** — a Worker claimed a `WorkItem` (lease granted) | Worker begins processing. |
| **`WorkItemLeaseExpired`** — heartbeat TTL exceeded | `WorkItem` reclaimed: lease cleared to `Lease.NONE`, `ASSIGNED → PENDING`, available for re-claim. Same id (same episode). |
| **`ActivationReported`** — Worker reported `ActivationDiagnostics` for the TR's current `WorkItem` | `WorkOrchestrator` invokes `TR.finalizeActivation(diagnostics)`, then marks the `WorkItem` `COMPLETED`. |
| **`ActivationCancelled`** — TR left `ACTIVATING` via `cancelActivation()` | `WorkOrchestrator` marks the `WorkItem` `CANCELLED`; any late report is ignored. |

> The event boundary is what lets the TR aggregate and the coordination aggregate live in separate
> transactions while staying consistent *eventually*.

---

## 7. Concurrency & Correctness

Two composable primitives cover the concurrency risks:

**(a) Lease-based ownership (handles crashes / silence).** A Worker must hold a non-expired `Lease` to
work a `WorkItem`. Heartbeats renew it. If a Worker crashes or is partitioned, its lease expires and the
`WorkItem` returns to `PENDING` for reassignment. This gives **at-least-once** processing.

**(b) Identity fencing (handles stale reports across two dimensions).** Because processing is
at-least-once, a report can arrive *stale* in two distinct ways:

- **Across activation episodes.** A TR can cycle `ACTIVATING → … → ACTIVATING` — e.g. an `ACTIVE` TR whose
  metadata is changed re-enters `ACTIVATING` against **different** metadata. A slow Worker from the
  *first* episode must not have its report applied to the *second*. The TR's status guard **cannot** catch
  this, because both episodes are `ACTIVATING`. **Fence: give each activation episode its own `WorkItem`
  identity (id / sequence);** a report names the `WorkItem` it was assigned under, and the orchestrator
  applies it only if that `WorkItem` is still the TR's *current* one.
- **Within one episode, across reassignment.** A Worker whose lease expired mid-episode may finish and
  report after the `WorkItem` was re-leased to another Worker. **Fence: lease ownership** — accept a report
  only from the Worker currently holding the lease.

> **Model — per-episode.** One `WorkItem` id per activation *episode* fences the load-bearing
> **cross-episode** dimension — the orchestrator tracks the **"current `WorkItem` for this TR"** and applies a
> report only if its `WorkItem` is that current one. The **within-episode reassignment** dimension is covered
> by **lease ownership** (accept only from the current lease holder) as a cheap early-drop — and is anyway
> *harmless* even if it slips through, because the target is immutable within an episode and the aggregate's
> `ACTIVATING`-only guard blocks a double-finalize (see the note below). Leases (§7a) detect a dead Worker; on
> expiry the item is **reclaimed to `PENDING`** (same id, same episode) — reused in place, not replaced.

> **Where the fence lives — the `WorkOrchestrator`, not the aggregate.** The identity/lease fence is a
> *coordination* concern (it speaks of Workers, leases, WorkItems); enforcing it inside
> `finalizeActivation` would leak coordination vocabulary into the Trust Configuration aggregate and break
> the §3 boundary. So the orchestrator decides whether a report is authoritative and simply **does not
> call** `finalizeActivation` for a stale one. The aggregate stays clean **and is still safe on its own
> two generic counts:**
>
> 1. **Its state machine (already implemented):** `finalizeActivation` is valid only from `ACTIVATING`.
>    The first authoritative report moves the TR out of `ACTIVATING`, so any later finalize is rejected by
>    the existing `OperationForbiddenFromStatus` rule — no coordination awareness required.
> 2. **Optimistic concurrency on `Version` (at the repository):** a stale save fails the version check and
>    the orchestrator re-evaluates (dropping the now-stale report). This is how *any* aggregate command is
>    persisted safely — not something injected for activation.
>
> **Net: no activation-specific change to the TR aggregate.** Coordination staleness → orchestrator;
> concurrent-write safety → generic optimistic lock; illegal timing → the aggregate's own state machine.
> (Note: *within* an episode the target is immutable — metadata/profile/attribute edits are forbidden from
> `ACTIVATING` — so an intra-episode late report is even semantically harmless. It is the **cross-episode**
> case in (b) that makes the identity fence genuinely load-bearing, since a later episode can validate
> *different* metadata.)

**Single vs multiple Workers.** These are not two designs — the lease + fencing model is one design,
parameterized by pool size:

- *Single Worker* = exclusive registration = a distributed lock. Simple, but a stuck/slow activation
  blocks all others, and a crashed holder needs the same lease-expiry machinery anyway.
- *Multiple Workers* = the general case; long-running activations no longer head-of-line-block others.

The design is the multiple-Worker + lease + fencing model; a single Worker is simply a pool of size 1.
There is no separate exclusive-registration mechanism.

---

## 8. Operation Flow

1. **(Out of scope, context)** A client calls `TR.activate()`; the TR transitions `READY|INACTIVE → ACTIVATING` and the Trust Configuration side raises **`ActivationRequested`**.
2. `WorkOrchestrator` handles `ActivationRequested` → creates `WorkItem(PENDING)` for `trustRelationshipId`. *(This is the "queue".)*
3. An available, alive `Worker` **claims** a `PENDING` `WorkItem`: lease granted (`lease` set to the granted `Lease`), state → `ASSIGNED`, event `WorkItemAssigned`. Claiming is atomic on the `WorkItem` (compare-and-set on state) so two Workers can't claim the same item.
4. The Worker performs the (out-of-scope) validation, **heartbeating** to renew its lease. If it goes silent, the lease expires and the item is reclaimed to `PENDING` (`lease` → `Lease.NONE`); step 3 repeats — same `WorkItemId`, same episode.
5. The Worker **reports** `ActivationDiagnostics` tagged with its `WorkerId`(=`Origin`) and the `WorkItemId` it was assigned under.
6. `WorkOrchestrator` checks the report's `WorkItem` is the TR's current one (and, as an early-drop, that the reporter still holds the lease); if so it resolves the opaque `trustRelationshipId` to the TR's `Id` and calls `TR.finalizeActivation(diagnostics)`, then marks the `WorkItem` `COMPLETED`. If not current, the report is dropped.
7. Terminal cleanup: on `COMPLETED` or `CANCELLED` the `WorkItem` is removed from the active set. On `cancelActivation()` the item is `CANCELLED` and later reports are ignored by fence.

---

## 9. Failure Modes & Handling

| Failure | Handling |
|---------|----------|
| Worker crashes mid-activation | Lease expires → `WorkItem` → `PENDING` → reassigned (§7a). |
| Slow Worker reports after reassignment | Dropped by the lease-ownership early-drop (§7b); even if applied it is harmless — same immutable target, and the aggregate's `ACTIVATING`-only guard blocks a double-finalize. |
| Duplicate / late report | Dropped by the orchestrator's identity fence; if one ever slips through, the aggregate's `ACTIVATING`-only guard rejects it (the TR already left `ACTIVATING`), and optimistic locking rejects a stale save. No-op either way. |
| TR `cancelActivation()` while a Worker is busy | **Not a special case.** The `WorkItem` stops being the TR's *current* one (mark it terminal / `CANCELLED`), so the Worker's eventual report is discarded by the identity fence (§7b) exactly like any non-current report. The TR sits in `READY`; a later `activate()` starts a fresh episode with a new `WorkItem`. |
| TR concurrently edited during `ACTIVATING` | Already constrained by the aggregate: `updateMetadataSource`/`updateXXXProfileConfiguration`/`updateReleasedAttributes` are forbidden from `ACTIVATING`; only descriptive updates and `incorporateDiscoveredEntityIds` (AGGREGATE) are allowed and do not change status — so the activation target is stable. |
| No Worker ever picks up the item | Stays `PENDING`; surfaced by monitoring. Optionally a max-age → dead-letter. |
| Worker reports `NO_DATA` | Coherent with the aggregate: `finalizeActivation(NO_DATA)` leaves the TR `ACTIVATING`; treat as "not done" — keep/reset the lease rather than completing. |

---

## 10. Design Decisions

- **Two bounded contexts.** Trust Configuration (`io.jans.shibboleth.model`) and Activation Coordination
  (`io.jans.shibboleth.activation`), integrated by domain events. `ActivationDiagnostics` stays with Trust
  Configuration as the `finalizeActivation` contract, not the coordination context (§3).
- **Context-owned identity.** The activation context defines its own `WorkItemId` / `WorkerId` and holds the
  TR reference as an opaque value, resolved to the trust `Id` only at the orchestrator boundary; it does not
  reuse `model.core.Id` (entangled with `TrustResult` / `TrustError`) (§3).
- **Shared kernel.** `Origin` — the one neutral, self-contained value object used by both contexts — is
  extracted to `io.jans.shibboleth.shared`; both depend on it, neither on the other. `Id` stays trust-local
  under the same "share only clean types" rule (§3).
- **Ubiquitous language.** `Worker` / `WorkItem` / `WorkOrchestrator` name the coordination context (§2).
- **Fence lives in the orchestrator.** The TR aggregate gains no fencing code; it relies on its
  `ACTIVATING`-only status guard plus generic optimistic concurrency on `Version` at the repository (§3, §7).
- **Per-episode `WorkItem` granularity.** One `WorkItem` per activation episode; lease expiry reclaims it to
  `PENDING` (same id) rather than minting a replacement — no lease history. The `WorkItem` id fences the
  cross-episode dimension; lease ownership covers within-episode reassignment (§7b). The `WorkItem` id is
  distinct from the TR `Version` (generic optimistic lock).
- **Lease TTL / heartbeat interval.** Externally configured (not hard-coded); consumed by the
  `WorkOrchestrator` / `Worker` liveness logic.
- **Delivery semantics.** At-least-once processing with effectively-once finalization (non-current /
  duplicate reports discarded by the identity fence) ⇒ Workers and the activation work must be safe to run
  more than once (idempotent side effects).

---

## 11. Out of Scope

Persistence and transport of `WorkItem` / `Worker` / heartbeats are out of scope here. Any implementation
must provide: an **atomic claim** (compare-and-set on `WorkItem` state), **lease expiry** against a time
source, and the **"current `WorkItem` for this TR"** pointer.
