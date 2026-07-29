# Activation Persistence — Design Directives (planning)

> **Status:** **delivered** — the 8-step plan (§9) is implemented and green (`trust-domain` + `trust-adapters`);
> the SQL integration tests are env-gated. Remaining work is the **follow-ups** in §10. This document scopes
> the **persistence** of the `activation`
> bounded context (`WorkItem`, `Worker`, and the coordination state in `WorkOrchestrator`) using
> **`jans-orm`** as the storage backend. It is the activation counterpart of
> [`trustrelationship_persistence_design.md`](./trustrelationship_persistence_design.md), which explicitly
> deferred this context ("separate doc, separate aggregate"). Same iterative, test-first workflow.
>
> **Module:** per [`directory_structure_decisions.md`](./directory_structure_decisions.md) (two-module
> structure), persistence lives in **`trust-adapters`**; the **pure `trust-domain`** stays framework-free.
> Unlike the TrustRelationship design, this one **does** require `trust-domain` changes (see §3) — that is
> the headline finding of this planning pass.

---

## 0. What jans-orm is, and why it shapes everything below

Despite the name, `jans-orm` is **not** a relational ORM. It is an abstraction layer that presents any of its
backends (LDAP, SQL, Spanner) as a **document store**. The single guarantee it exposes uniformly across every
backend is that **an entry's identity — its `inum` / DN — is unique**. There is *no* portable secondary
`UNIQUE` constraint, and *no* portable conditional update: a write compiles to `UPDATE … WHERE <primary key>`
only (verified in the SQL layer — the update WHERE clause is keyed on `doc_id` alone). A create compiles to an
`INSERT` that raises `DuplicateEntryException` on a primary-key collision.

That is the whole reason multi-node locking was hard, and it is also the key to the solution: **the only lock
primitive we have is "atomic create of a uniquely-identified entry."** So instead of fighting the store with
compare-and-set, we **encode the mutual-exclusion invariant into the entry's identity** and let inum
uniqueness enforce it (§6). Everything downstream follows from this one fact.

---

## 1. Why this is not "just another adapter"

`TrustRelationship` was already a self-contained aggregate: it carried its own id, version, and full state,
and had a `builder()` rehydration path. Persisting it was pure adapter work in `trust-adapters`; the domain
did not move.

The activation context is the opposite. **All of its live state is held in `WorkOrchestrator`'s three
in-memory maps:**

```java
private final Map<WorkItemId, WorkItem> items;              // the work items
private final Map<TrustRelationshipRef, WorkItemId> currentByTr;  // "current" pointer per TR
private final Map<WorkerId, Worker> workers;                // the registered workers
```

Every operation — `onActivationRequested`, `claim`, `claimNext`, `heartbeat`, `sweepExpiredLeases`,
`report`, `onActivationCancelled`, worker register/heartbeat — reads and mutates those maps directly. There
is no store behind them; a process restart loses everything, and a second node shares nothing.

Persisting activation therefore means **turning the orchestrator's maps into repository-backed ports** and
adding the domain machinery that durable storage needs but the in-memory version never did. That work is in
`trust-domain` and must land before (or with) the adapter.

---

## 2. What needs to be persisted (and what does not)

| State | Persist? | Rationale |
|---|---|---|
| **`WorkItem`** (the `items` map) | **Yes — durable aggregate** | The unit of activation work; must survive restart and be visible across nodes. Its `PENDING`/`ASSIGNED` state is **derived** from lease existence (§6); only the terminal flag is stored. |
| **`Lease`** (today a field on `WorkItem`) | **Yes — as a separate, create-only satellite aggregate** | The lease's *existence* is the assignment, and its **identity is the lock** (§6). Promoting it out of `WorkItem` is what turns "claim" into an atomic `persist()` instead of a compare-and-set. |
| **`Worker`** (the `workers` map) | **Yes — durable, short-lived** | Liveness (`isAlive`/`lastHeartbeatAt`) gates claiming; a restarted node must still see live workers. Candidate for a store-level TTL (AP4). |
| **`currentByTr`** ("current work item per TR") | **Yes — as a derivation or a flag, not a third table** (AP3) | Only used by `isCurrent` / `report` / `onActivationCancelled`. Options in §4.4. |
| Config: `leaseTtl`, `heartbeatTtl` | **No** | Deployment configuration, injected into the orchestrator. |
| `TimeSource` | **No** | A port; production impl is the system clock. |
| `ActivationEventSink` events (`WorkItemAssigned`, `WorkItemLeaseExpired`) | **No, this phase** (AP6) | Transient notifications today. If reliable delivery is later required, add a transactional **outbox** table — a follow-up, not built now. |
| `ActivationDiagnostics` from `report(...)` | **No — flows to the *config* aggregate** | `report` calls `finalizePort.finalizeActivation(...)`, which writes diagnostics onto the `TrustRelationship` (already persisted there). The work item stores no diagnostics. |

So: **three durable aggregates (`WorkItem`, `Lease`, `Worker`)** plus a decision about the current-per-TR pointer.

---

## 3. Required `trust-domain` changes (the prerequisite work)

These are not adapter concerns — they are gaps in the domain that in-memory maps masked.

- **AP1 — Rehydration paths.** `WorkItem.create(...)` always generates a fresh id and starts `PENDING`;
  `Worker.register(...)` always stamps "now". Neither can reconstruct a stored instance. Add
  `builder`-style **rehydrate** paths (mirroring `TrustRelationship.builder()` vs `create()`) to `WorkItem`,
  `Worker`, and the new `Lease` aggregate — a create path (new) and a from-store path (verbatim). The
  entry↔domain mapper needs the from-store path.

- **AP2 — `Lease` becomes a create-only satellite aggregate; state derives from it.** Today `Lease` is a
  value held inside `WorkItem` and mutated via transitions. Promote it to its own aggregate that is **created,
  never claimed-in-place**, carrying `workItemId`, `generation`, `holder`, `grantedAt`, `expiresAt`.
  `WorkItem` then stops storing the lease; its `PENDING`/`ASSIGNED` state is **derived** — `ASSIGNED` iff a
  live (max-generation, unexpired) lease exists, `PENDING` otherwise — while `COMPLETED`/`CANCELLED` remain
  terminal flags on `WorkItem`. This is the domain change that makes §6 possible. **No `version`/optimistic
  field on `WorkItem`** — the earlier optimistic-CAS idea is dropped.

- **AP5 — Orchestrator becomes repository-backed.** Replace the three maps with repository ports
  (`WorkItemRepository`, `LeaseRepository`, `WorkerRepository`) plus current-per-TR resolution (AP3). The
  orchestrator stays a domain **service** expressing transitions; it loads/saves instead of `map.get/put`.
  Query-shaped operations change:
  - `claim` / `claimNext` → acquire a lease by **atomic create** (§6), not a status mutation.
  - `claimNext` → select oldest non-terminal candidate(s) of a `type`, attempt the lease acquire, and on a
    lost race move to the next candidate — not a full `values().stream()`.
  - `sweepExpiredLeases` → expiry is implicit (a lease with `expiresAt < now` is dead); the sweep becomes
    lazy, race-safe **GC** of superseded/expired lease rows, not an in-place reclaim.
  - `isCurrent` → resolved via AP3.

---

## 4. Storage shape (jans-orm)

Convention follows the TrustRelationship adapter: **DN is the primary key** (`@DN dn`, inherited from
`BaseEntry`); `inum` is the stable id attribute (`ignoreDuringUpdate`). Enums stored by `.name()`. `Instant`
stored as jans-orm date/generalized-time. Flat columns for anything filtered/sorted/compared.

### 4.1 `WorkItem` → `@DataEntry @ObjectClass("jansTrustActivationWorkItem")`

| Attribute | Source | Notes |
|---|---|---|
| `inum` (`@DN` PK) | `WorkItemId` — **random UUIDv4** | Single creator (the orchestrator), no claim-race → random id is correct (AP9). DN = `inum=<uuidv4>,ou=trustActivationWorkItems,o=jans` |
| `jansWorkItemType` | `WorkItemType.name()` | **indexed** — `claimNext` filter |
| `jansTrId` | `TrustRelationshipRef` UUID | **indexed** — current-per-TR + per-TR history |
| `jansWorkItemStatus` | terminal flag: `COMPLETED` \| `CANCELLED` \| *null* | *null* ⇒ `PENDING`/`ASSIGNED`, **derived** from live-lease existence (§6). Not the full enum. |
| `jansCreatedAt` | `createdAt` | **sortable** — `claimNext` picks oldest |
| `jansLastTransitionAt` | `lastTransitionAt` | audit/ordering |
| `jansCurrent` (bool) | AP3 option (b) | only if we store the flag rather than derive |

No lease columns and no version column live on `WorkItem` — both moved out under AP2.

### 4.2 `Lease` → `@DataEntry @ObjectClass("jansTrustActivationLease")` — the lock

| Attribute | Source | Notes |
|---|---|---|
| `inum` (`@DN` PK) | **deterministic name-based UUID** of `(workItemId, generation)` (AP9) | The lock. Two workers computing the same `(item, gen+1)` derive the **same inum** → PK collision → one wins. DN = `inum=<name-uuid>,ou=trustActivationLeases,o=jans` |
| `jansWorkItemRef` | `workItemId` UUID | **indexed** — "find leases for this work item" (max-generation lookup) |
| `jansLeaseGen` | `generation` (int, monotonic) | the **fencing token**; current holder = max generation |
| `jansLeaseWorker` | `holder` (`Origin` string) | who holds it |
| `jansLeaseGrantedAt` | `grantedAt` | |
| `jansLeaseExpiresAt` | `expiresAt` | expiry is implicit; no in-place reclaim |

`Lease` rows are **append-mostly**: created on acquire, `expiresAt` updated only by the holder on heartbeat,
deleted only as lazy GC of superseded generations.

### 4.3 `Worker` → `@DataEntry @ObjectClass("jansTrustActivationWorker")`

| Attribute | Source | Notes |
|---|---|---|
| `inum` (`@DN` PK) | **deterministic name-based UUID** of the worker's `Origin` | Uniformly shaped id like every other entry; derived from the origin so a direct DN lookup by worker id needs no secondary search. DN = `inum=<name-uuid>,ou=trustActivationWorkers,o=jans` (AP7 — origin no longer sits in the DN). |
| `jansWorkerOrigin` | `WorkerId` = `Origin.value` (a **string**) | the raw origin; what a read rebuilds the worker id from |
| `jansRegisteredAt` | `registeredAt` | |
| `jansLastHeartbeatAt` | `lastHeartbeatAt` | liveness basis; `isAlive` is computed from it (AP4) |

### 4.4 The "current work item per TR" (AP3 — needs a decision)

`currentByTr` is set on `onActivationRequested` and cleared on `onActivationCancelled`; `isCurrent` and
`report` consult it. It is **not** simply "newest by `createdAt`": after cancelling the newest item, the map
has *no* current for that TR, whereas "newest" would still point at the cancelled row. Options:

- **(a) Derive** `current` = newest **non-cancelled** item for the TR (query by `jansTrId`, order by
  `jansCreatedAt` desc, skip cancelled). No extra column; one query. Slight semantic drift to verify against
  the current tests.
- **(b) Store a `jansCurrent` boolean** on `WorkItem`, set on request, cleared on cancel. Faithful to the map;
  costs a column and a "clear previous current" write on each new request.
- **(c) A tiny pointer entry** mapping TR→WorkItemId. Most faithful, most moving parts.

**Recommendation:** (a) if the behaviour survives the existing `WorkOrchestrator` tests; otherwise (b).

---

### 4.5 Provisioning — schema and branches must track these exact names

> **For whoever owns provisioning (the jans setup / installer, not this application code).** The object-class
> names, attribute names, and `ou=` branch names below are **storage-visible identifiers**, not internal code
> details. jans-orm maps an entry's `@ObjectClass` straight onto a **SQL table name** (and onto an LDAP
> `objectClass`), and each `@AttributeName` onto a **column** (LDAP attribute). If provisioning creates
> anything under a different name, `find`/`persist` fail at runtime — a table/attribute-not-found, not a
> compile error. These names were **renamed** (see AP7 and §9): any environment provisioned against the older
> `jansWorkItem` / `jansWorkItemLease` / `jansActivationWorker` / `jansCurrentEpisode` names, or the older
> `ou=workItems` / `ou=workItemLeases` / `ou=activationWorkers` / `ou=currentEpisodes` branches, must be
> migrated to the names here.

**The canonical reference DDL** lives at
`trust-adapters/src/test/resources/init-scripts/01-activation-init.sql`. That file is a **test fixture** (it
provisions the Postgres the env-gated ITs run against); production provisioning must create the equivalent
structures but is owned by the installer. Keep the two in sync when either changes.

**Four object classes / four branches.** Every entry is `inum=<id>,<branch>`; the DN is the primary key, and
on SQL jans-orm derives `doc_id` (the PK column) from the first RDN value — i.e. the `inum`. For leases and
workers the `inum` is a **deterministic name-based UUID** (§6, AP7), so PK uniqueness *is* the mutual-exclusion
lock — no secondary `UNIQUE` constraint is needed or portable.

| Aggregate | Object class (= SQL table, = LDAP objectClass) | Branch |
|---|---|---|
| `WorkItem` | `jansTrustActivationWorkItem` | `ou=trustActivationWorkItems,o=jans` |
| `Lease` | `jansTrustActivationLease` | `ou=trustActivationLeases,o=jans` |
| `Worker` | `jansTrustActivationWorker` | `ou=trustActivationWorkers,o=jans` |
| current-episode pointer | `jansTrustActivationEpisode` | `ou=trustActivationEpisodes,o=jans` |

**Common columns on every table** (the jans-orm base entry, mirroring `jansTrustRelationship`): `doc_id`
(varchar PK — the `inum`), `objectClass` (varchar), `dn` (varchar), `inum` (varchar).

**Per-object-class attributes** (name — SQL type — notes):

- `jansTrustActivationWorkItem`
  - `inum` — varchar(64) — random UUIDv4 work-item id (single creator, no race → random is correct).
  - `jansWorkItemType` — varchar(64) — indexed (with status) for the `claimNext` candidate filter.
  - `jansTrId` — varchar(64) — the trust-relationship id.
  - `jansWorkItemStatus` — varchar(64), **nullable** — stores only the terminal flag (`COMPLETED`/`CANCELLED`);
    **null means non-terminal** and `PENDING`/`ASSIGNED` is derived from live-lease existence (§6, AP2).
  - `jansCreatedAt` — **timestamp** — FIFO ordering of candidates.
  - `jansLastTransitionAt` — **timestamp**.
  - Index: `(jansWorkItemType, jansWorkItemStatus)`.
- `jansTrustActivationLease`
  - `inum` — varchar(64) — **deterministic** name-based UUID of `(workItemId, generation)`. The PK collision on
    this value is the lock; provisioning must **not** add any other uniqueness that would change that.
  - `jansWorkItemRef` — varchar(64) — the work-item id this lease is for; indexed (max-generation lookup).
  - `jansLeaseGen` — int4 — monotonic fencing token.
  - `jansLeaseWorker` — varchar(128) — holder origin.
  - `jansLeaseGrantedAt` — **timestamp**.
  - `jansLeaseExpiresAt` — **timestamp**.
  - Index: `(jansWorkItemRef)`.
- `jansTrustActivationWorker`
  - `inum` — varchar(64) — **deterministic** name-based UUID of the origin (AP7); the origin itself is *not* in
    the DN.
  - `jansWorkerOrigin` — varchar(128) — the raw origin string; a read rebuilds the `WorkerId` from this.
  - `jansRegisteredAt` — **timestamp**.
  - `jansLastHeartbeatAt` — **timestamp**.
- `jansTrustActivationEpisode` (one pointer per trust relationship)
  - `inum` — varchar(64) — the trust-relationship id (this is the key: at most one pointer per TR).
  - `jansWorkItemRef` — varchar(64) — the current work item for that TR.

**Timestamps must be a real date/time type, never `varchar`.** On SQL these six columns are `timestamp`
(generalized-time on LDAP). jans-orm owns the date codec: the entry fields are `java.util.Date` and jans-orm
auto-detects date-shaped values, so a plain `varchar` column silently reformats the value on read (drops the
`Z`, shifts to local time) and breaks `Instant` round-tripping. The connection is configured with
`serverTimezone=UTC` so the native `timestamp` binding round-trips losslessly.

**`doc_id` sizing.** varchar(64) is sufficient everywhere: every `inum` is now either a UUIDv4 (36 chars) or a
UUID string — the worker id is no longer the free-form origin, so no oversized-DN concern remains.

---

## 5. Repositories, read models, listing

- `WorkItemRepository`: `save`, `findById`, `delete`, plus the candidate finder `claimNext` needs
  (oldest non-terminal of a `WorkItemType`). Returns rehydrated domain `WorkItem`s.
- `LeaseRepository`: `create` (the atomic acquire — surfaces "lost the race" from `DuplicateEntryException`),
  `findByWorkItem` (for the max-generation lookup), `renew` (holder-only `expiresAt` update), `delete` (GC).
- `WorkerRepository`: `save`, `findById`, `delete` (or rely on AP4 TTL for eviction).
- **Read model / list:** **out of scope for now** — persistence exists purely to back the orchestrator, not
  to serve an activation list API (§8 Q3). If such an API later appears, add a reduced summary entry + query
  projection exactly as `TrustRelationshipSummaries` does (TP10/TP11 pattern) so listing never rehydrates the
  full aggregate.

---

## 6. Concurrency model — mutual exclusion via identity (settled)

The requirement is genuine **multi-node atomic claim**: exactly one worker wins a claimable item, and a dead
holder's item can be taken over. Given §0 (the only portable primitive is atomic create of a
uniquely-identified entry), we **make the identity the lock**. This is a Lamport-style monotonic lock /
ZooKeeper-sequential-znode pattern — a recognized lock-free technique, not a workaround.

**Lease identity = a deterministic name-based UUID of `(workItemId, generation)`.** Because it is
*deterministic*, two workers racing for the same generation compute the **same** inum and collide on the
primary key; because generations are *monotonic*, they double as a fencing token. Everything reduces to four
primitives every backend supports — **create / read / update-your-own / delete-old** — with **zero conditional
writes**:

- **Claim / take over.** Read the lease rows for the item (`findByWorkItem`), find the max generation `N`. If
  none, or `N` is expired, `create(Lease{ inum = nameUuid(workItemId, N+1), generation = N+1, … })`. Two
  workers both attempt `N+1` → **inum uniqueness lets exactly one win**; the loser catches
  `DuplicateEntryException`, re-reads, sees a fresh `N+1`, and yields. You can never leapfrog a live lease —
  you only ever attempt `max+1` of what you actually observed.
- **`claimNext`.** Select oldest non-terminal candidate(s) of the type; attempt the claim above; on a lost
  race, move to the next candidate. Lease existence — not a `WorkItem` status column — is authoritative.
- **Heartbeat.** The holder extends `expiresAt` on **its own** generation's lease row (an inum only it
  holds). **Uncontended by construction** → a plain update, no CAS.
- **Reclaim / sweep.** No conditional delete: expiry is implicit (`expiresAt < now` ⇒ dead), and takeover is
  just creating `N+1`. Superseded rows are **lazily GC'd** by deleting generations strictly below the current
  holder's — race-safe, because the max is never deleted while live.
- **Fencing.** The generation is the fencing token. When the holder finalizes (`report` →
  `finalizeActivation`, which writes to the `TrustRelationship`), it carries its generation; a resurrected
  zombie at generation `N` is rejected because a higher generation exists (AP10). `WorkItem.state` stays
  derived; terminal transitions (`complete`/`cancel`) set the `WorkItem` terminal flag and delete the lease(s).

**Why this is not the earlier hack.** The mutual exclusion is a *property of the identity scheme*, enforced by
the same mechanism that keeps any two entries from sharing a DN — the one guarantee jans-orm gives us on every
backend. The atomic step is `persist()`; there is no reaching under the ORM and no backend-specific SQL. The
superseded conditional-`UPDATE`/optimistic-version approaches from earlier drafts are **dropped**.

**Residual caveat (inherent to distributed locks):** the finalize path must honour fencing (AP10). The
verification checkpoint before we build: confirm a duplicate-inum `persist()` surfaces as a catchable
`DuplicateEntryException` we can read as "lost the race" (the SQL `addEntry` is an `INSERT` declared to throw
it — expected yes, to be confirmed against each target backend).

---

## 7. Decisions (settled unless marked "to confirm")

| # | Decision |
|---|---|
| **AP1** | Add from-store **rehydration** paths to `WorkItem`, `Worker`, and the new `Lease` aggregate. |
| **AP2** | Promote `Lease` to a **create-only satellite aggregate**; `WorkItem.state` (`PENDING`/`ASSIGNED`) is **derived** from live-lease existence, with **only** `COMPLETED`/`CANCELLED` stored as terminal flags (confirmed §8 Q1). No optimistic `version` on `WorkItem`. |
| **AP3** | Represent current-per-TR by a **persisted per-TR pointer entry** (4.4 option c): a `ou=trustActivationEpisodes` (`jansTrustActivationEpisode`) entry keyed by trust-relationship id → current work-item id. Chosen over derivation (option a) because a fixed clock / same-instant creation makes "newest by `createdAt`" ambiguous; the pointer reproduces the in-memory map exactly. Delivered as follow-up §10.1. |
| **AP4** | **Rely on `isAlive()` for now** (computed from `lastHeartbeatAt + heartbeatTtl`); **no** store-level `@Expiration` TTL this phase (confirmed §8 Q2). TTL-based auto-eviction is a future option. |
| **AP5** | Refactor `WorkOrchestrator` to be **repository-backed** (`WorkItem`/`Lease`/`Worker` repos); claim = lease acquire, sweep = lazy GC. |
| **AP6** | Do **not** persist activation events this phase; an **outbox** is a future option. |
| **AP7** | `Worker` storage id (`inum`) is a **deterministic name-based UUID of the origin**, not the raw origin — so the caller-supplied string never sits in the DN (no DN-escaping concern) while a direct DN lookup by worker id still resolves without a secondary search. The raw origin is stored in `jansWorkerOrigin` for rehydration. |
| **AP8** | Concurrency model = **leaderless, multi-node atomic claim** via deterministic-inum lease creation (§6); mutual exclusion is enforced by inum uniqueness — the one portable jans-orm guarantee. |
| **AP9** | **`WorkItem` inum = random UUIDv4** (single creator, no race); **`Lease` inum = deterministic name-based UUID** of `(workItemId, generation)` (contended slot). The determinism *is* the lock — a random lease inum would silently break mutual exclusion, so the derivation must be documented at the call site. Candidate impl: `UUID.nameUUIDFromBytes(("lease|"+workItemId+"|"+generation).getBytes(UTF_8))` (JDK v3, dependency-free; v5/SHA-256 if stronger hashing is wanted). |
| **AP10** | **Fencing on finalize:** `report`/`finalizeActivation` carries the holder's generation; a holder whose generation is no longer the max is rejected, so a zombie cannot finalize work it has lost. |

---

## 8. Open questions — resolved

All settled; recorded here for provenance.

1. **Concurrency model** → §6/AP8: leaderless multi-node atomic claim via deterministic-inum leases.
2. **`WorkItem` terminal-state modelling** → **store only the terminal flag**; `PENDING`/`ASSIGNED` derived
   from lease existence (AP2).
3. **Worker lifecycle** → **rely on `isAlive()` for now**; no `@Expiration` TTL (AP4).
4. **Activation API surface** → **none for now**; persistence exists purely to back the orchestrator, so no
   read-model/list projection this phase (§5).
5. **Ordering** → **domain-first, as incrementally as possible** (see §9): each step is independently
   buildable and testable, and the domain refactor lands and stays green before any adapter code.

---

## 9. Implementation order — domain-first, incremental — ✅ delivered

All eight steps are implemented, test-first, one commit each, tree green throughout. Domain landed and stayed
green (steps 1–5) before any adapter code (steps 6–8). Summary of what shipped:
`Lease`/`LeaseGeneration` satellite aggregate; derived `WorkItem.state`; rehydration paths; repository ports
+ collision-modelling fakes; a repository-backed, **leaderless multi-node** `WorkOrchestrator` returning
`WorkItemActivation`; `jansTrustActivationWorkItem`/`jansTrustActivationLease`/`jansTrustActivationWorker`
entries + mappers with deterministic lease and worker inums; the three `PersistenceEntryManager`-backed
repository impls; and env-gated SQL
integration tests + starter DDL proving the two-worker race resolves to one winner.

**Domain (`trust-domain`, no store):**

1. **`Lease` as a create-only satellite aggregate** — new `Lease` with `workItemId`, `generation`, `holder`,
   `grantedAt`, `expiresAt`; `create`/rehydrate paths; `isExpired`, `isHeldBy`, generation accessor. Unit
   tests only. (`WorkItem` not touched yet.)
2. **Derived `WorkItem.state`** — `WorkItem` stops holding the lease; state becomes `PENDING`/`ASSIGNED`
   derived from a supplied "live lease?" plus stored terminal flag (`COMPLETED`/`CANCELLED`). Adjust
   `WorkItem` transitions/tests accordingly (AP2).
3. **Rehydration paths (AP1)** — from-store builders on `WorkItem` and `Worker` (Lease got its own in step 1).
4. **Repository ports** — `WorkItemRepository`, `LeaseRepository` (create surfaces "lost the race"),
   `WorkerRepository`; plus in-memory **fakes** whose `create` models inum-collision so lease semantics are
   exercised without a DB.
5. **Repository-backed `WorkOrchestrator` (AP5)** — swap the three maps for the ports; claim = lease acquire,
   `claimNext` = candidate loop, sweep = lazy GC. Every existing orchestrator test stays green against the fakes.

**Adapter (`trust-adapters`):**

6. **Entries + mappers** — `jansTrustActivationWorkItem` / `jansTrustActivationLease` /
   `jansTrustActivationWorker` `@DataEntry`s and entry↔domain mappers, including the deterministic lease- and
   worker-inum derivations (AP9). TDD like `TrustRelationshipEntryMapper`.
7. **Repository impls** over `PersistenceEntryManager` — claim (`create` → catch `DuplicateEntryException`),
   `findByWorkItem`, holder-only renew, GC; mocked entry-manager tests.
8. **Env-gated SQL integration tests + starter DDL** (reusing the Postgres `docker-compose.yaml` already in
   `trust-adapters`) that actually exercise a two-worker race for the same generation.

---

## 10. Follow-ups (post-delivery)

Handled in this order:

1. **Persist the current-episode pointer** (the AP3 carry-over). `WorkOrchestrator` still holds `currentByTr`
   in memory, so the current-episode-per-TR pointer does not survive restart and is not shared across nodes —
   the one piece of activation state still not durable. Persisted as a per-TR pointer entry
   (`ou=trustActivationEpisodes,o=jans`, keyed by the trust-relationship id → current work-item id), which reproduces
   the in-memory map exactly (upsert on request, clear on cancel) without depending on `createdAt` ordering.
   This makes the orchestrator fully stateless.
2. **Branch bootstrap** — ~~ensure the `ou=…` branches exist via `SimpleBranch` at first-run~~ **not needed
   in application code:** the jans setup already provisions these branches (`trustActivationWorkItems`,
   `trustActivationLeases`, `trustActivationWorkers`, `trustActivationEpisodes`) alongside the schema. Creating them here would duplicate a concern
   the installer owns. Skipped by decision.
3. **Outbox (AP6)** — only if reliable activation-event delivery is later required; otherwise events stay
   transient (best-effort, in-memory sink — accepted for now). Lowest priority; not started unless asked.

   *Can the outbox pattern be done on jans-orm alone?* The **textbook outbox — no**: it requires committing
   the state change and the event row in one transaction, and jans-orm exposes only single-entry autocommit
   `persist`/`merge`/`remove` (no cross-entry transaction — its LDAP lowest-common-denominator has none). Any
   two-write scheme leaves a crash window (state-then-event loses the event; event-then-state yields a phantom).

   Its **guarantee — yes**, via the same move as the lease lock: fold the event into the one durable primitive
   the store gives us, i.e. **treat the state entries as the event log and tail them**. Each event is already a
   projection of a committed entry (`WorkItemAssigned` *is* "a lease row exists"), so there is only one write —
   the state write. A relay polls `findEntries(<orderKey> > watermark, ordered)`, publishes, then advances a
   per-consumer watermark it owns; crash-after-publish re-publishes → **at-least-once**, entirely within jans-orm.

   Three catches: (a) needs a monotonic queryable order — a timestamp column, with the same same-instant tie
   caveat that drove AP3 (no sequences in jans-orm); (b) **removal events can't be tailed** — `WorkItemLeaseExpired`
   removes the lease row, so it would need **tombstones** (soft-delete, reclaimed marker) instead of our lazy
   hard-GC; (c) consumers must be idempotent. True exactly-once co-commit would require dropping below jans-orm
   to a native SQL transaction (sacrificing backend portability) — the same trade-off as the conditional-UPDATE
   claim we rejected. Log-tailing-from-durable-state is the ceiling on the document-store abstraction, and it is
   the shape CDC / "poll the outbox" designs reduce to anyway.

Not a code follow-up: running the env-gated SQL ITs against a real provisioned database.
```