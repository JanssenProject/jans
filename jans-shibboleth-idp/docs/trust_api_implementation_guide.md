# Implement the Trust REST API (transport layer)

> **For the API implementer.** This is a GitHub-issue-ready brief for building the REST transport that
> exposes the trust domain over HTTP. The domain (`trust-domain`), the persistence adapters, the DTOs, the
> mappers, and the two OpenAPI contracts (`trust-adapters`) **already exist and are green**. What does *not*
> exist yet is the piece both API design docs explicitly deferred: **the REST controllers and their
> framework wiring**. That is the whole of this task.
>
> Implementation language: **Java**. Build: `mvn` (offline-capable, `mvn -o`).

---

## 1. What this issue is (and is not)

**Build:** an HTTP application that realises the two OpenAPI specifications already committed under
`trust-adapters/src/main/resources/openapi/`, by wiring each operation to the domain through the existing
mappers, repositories, and the `WorkOrchestrator`, and by translating the domain's `Result` / `DomainError`
model into HTTP responses and RFC 7807 `problem+json` bodies.

**Do not build** (already done — reuse, do not reinvent):

| Layer | Where it lives | Status |
|---|---|---|
| Pure domain (aggregates, value objects, `Result`, errors, `WorkOrchestrator`) | `trust-domain` | ✅ done, green |
| Outbound persistence adapters (jans-orm repository impls) | `trust-adapters` `…/persistence/**` | ✅ done, green (SQL ITs env-gated) |
| DTOs (request/response) | `trust-adapters` `…/dto/**` | ✅ done |
| Mappers (DTO ⇄ domain) | `trust-adapters` `…/dto/mapper/**` | ✅ done |
| OpenAPI 3.1 contracts + shared components | `trust-adapters/src/main/resources/openapi/**` | ✅ done |

**The gap you are filling:** REST resource classes / controllers; dependency-injection wiring of mappers,
repositories and the orchestrator; the `Result`→HTTP and `DomainError`→`problem+json` glue; the bearer-auth
filter; application bootstrap; and the runtime configuration of the jans-orm `PersistenceEntryManager`.

---

## 2. Source-of-truth documents (read these first)

The design is already settled in prose. This guide points at those docs rather than restating their
decisions; where they disagree with the code, **the code wins**.

| Document | Authoritative for |
|---|---|
| [`openapi_design_spec.md`](./openapi_design_spec.md) | Config API: decisions D1–D16, conventions, full endpoint catalog, error→HTTP mapping |
| [`activation_api_design_spec.md`](./activation_api_design_spec.md) | Activation (M2M) API: decisions AA1–AA6, fencing model, endpoint catalog, error→HTTP mapping |
| [`activation_persistence_design.md`](./activation_persistence_design.md) | Storage shape; **§4.5 = provisioning (object classes, branches, columns) the installer must create** |
| [`trustrelationship_persistence_design.md`](./trustrelationship_persistence_design.md) | Config aggregate persistence |
| [`directory_structure_decisions.md`](./directory_structure_decisions.md) | Module layout, artifact names, dependency direction |
| [`asynchronous_activation.md`](./asynchronous_activation.md) | Ubiquitous language of the worker protocol, fence-token rationale |
| `trust-adapters/src/main/resources/openapi/*.yaml` | **The contract** — every path, schema, status code, and `problem+json` shape |

The two OpenAPI YAMLs are the exact wire contract. The endpoint catalogs in §6/§7 of the design docs are
checked-off (`[x]`) because the DTOs/mappers/spec for each already exist — the checkbox means "contract +
DTOs + mappers + tests done", **not** "endpoint served over HTTP". Serving them is this task.

---

## 3. Modules & coordinates

Reactor parent: `io.jans:jans-shibboleth-idp-parent:0.0.0-nightly` (packaging `pom`). Active modules:

- `io.jans:jans-shibboleth-trust-domain:0.0.0-nightly` — pure domain, framework-free.
- `io.jans:jans-shibboleth-trust-adapters:0.0.0-nightly` — depends on `trust-domain`; carries jackson,
  jans-orm (`jans-orm-annotation/-model/-core/-filter`, `jans-orm-sql` at test scope), the DTOs, mappers,
  and OpenAPI specs. Its own description already reads: *"Adapters for the Shibboleth IDP trust domain:
  DTOs, mappers, the OpenAPI specification (inbound) and jans-orm persistence (outbound)."*

**Add a new module for the transport** (suggested `trust-api` / `jans-shibboleth-trust-api`), depending on
`trust-adapters` (which transitively brings `trust-domain`). Register it in the reactor `<modules>`. Keep
the transport out of `trust-adapters` so the "adapter" module stays free of a servlet/JAX-RS container
dependency and can be unit-tested without one. Dependency direction stays inward:
`trust-api → trust-adapters → trust-domain`.

> `jans-orm-sql` is `test` scope in `trust-adapters`. The runtime app must depend on the concrete backend
> (`jans-orm-sql`, or another jans-orm backend) at **runtime/compile** scope so a real `PersistenceEntryManager`
> can be created at boot.

---

## 4. Architecture in one picture

```
HTTP request
   │  (JSON, snake_case)
   ▼
[REST resource / controller]         ← YOU BUILD THIS
   │  deserialize → request DTO
   ▼
[Mapper]  (exists)                    DTO → Result<domain>            e.g. TrustRelationshipMapper.toDomain(req)
   │
   ├─ config path ─────────────► [TrustRelationshipRepository] (exists) → jans-orm
   │                                   load → mutate via domain op → save
   │
   └─ activation path ────────► [WorkOrchestrator] (exists)
                                        └─► [WorkItem/Lease/Worker repos] (exist) → jans-orm
   │
   ▼
Result<domain>  ──► [Mapper] (exists) domain → response DTO   e.g. TrustRelationshipMapper.toSummary(tr)
   │
   ▼
[Result→HTTP translator]             ← YOU BUILD THIS
   success → 200/201/204 + DTO
   failure → status + problem+json (code from DomainError subclass)
```

Two distinct call shapes:

- **Config API** talks to a **repository + mappers**. A mutation is: `findById` → call the domain mutator
  (via the mapper's `updateX(existing, request)`) → `save` → map the saved aggregate to a response DTO.
- **Activation API** talks to the **`WorkOrchestrator` domain service** (which is itself repository-backed).
  Controllers call orchestrator methods; the orchestrator owns the lease/claim/fence logic.

---

## 5. The core contract: `Result` / `DomainError` → HTTP

This is the single most important thing to get right, and it is uniform across both APIs.

### 5.1 `Result<T>` — never throws for domain outcomes

```java
Result<T> r = ...;
r.isSuccess() / r.isFailure()
r.getValue()   // T — throws IllegalStateException ONLY if you call it on a failure (a programming bug)
r.getError()   // DomainError — throws IllegalStateException ONLY if called on a success
```

Factories are `Result.success(v)` / `Result.failure(error)`. There is **no** `map`/`flatMap`, and there is
**no `Optional` anywhere in the domain** (absence is modelled with null-object value types — do not
reintroduce `Optional` in the transport layer's domain-facing code). Mappers already return `Result` and
**never throw** for domain-rule failures; your controller must mirror that discipline — inspect the
`Result`, don't wrap domain calls in try/catch expecting exceptions.

### 5.2 `DomainError` → `problem+json`

`DomainError` is an abstract base (`getMessage()`); every failure is a concrete subclass. Map each subclass
to a stable machine-readable `code` and an HTTP status, and emit RFC 7807:

- Media type `application/problem+json`.
- `type` = `https://jans.io/shibboleth-idp/problems/{code}` (D12).
- Members: `type`, `title`, `status`, `detail`, `instance`, plus extension `code` and optional
  `violations: [{ field, code, message }]`.
- **Wrappers unwrap:** `DomainObjectCreationFailed` / `DomainObjectUpdateFailed` (and
  `DomainObjectConsistencyFailed`) carry a *cause*; surface the **cause's** `code`, not the wrapper's.

There is currently **no `ProblemDto` Java type** — only `PageMetadata` lives in `dto/shared`. The
`Problem`/`Violation` **schema** is defined in `openapi/components/common.yaml`. You must add the
`problem+json` Java representation (or serialize a map) and the central translator. Put the error-code
registry in one place (an enum or a `Map<Class<? extends DomainError>, (status, code)>`), because both APIs
share it.

### 5.3 Error → HTTP mapping (from the design docs — implement verbatim)

**Config API** (`openapi_design_spec.md` §5.3):

| Domain error (family) | HTTP |
|---|---|
| `RequiredValueMissing`, `InvalidUriSyntax`, `InvalidUuidSyntax`, `InvalidTimestampSyntax`, `InvalidDurationSyntax`, malformed body | `400` |
| `IncompatibleMetadataSourceForNature`, `OperationRestrictedToNature` | `400` (domain-rule validation — **not** 422) |
| `InvalidStatusForOperation`, `OperationForbiddenFromStatus`, `TrustTransitionError` | `409` |
| `InvalidVersion` | `409` |
| `TrustRelationshipNotFound`, `IdNotAssigned` (read/mutate by id) | `404` |
| auth missing/invalid | `401` / `403` |
| unexpected | `500` (never leak internals in `detail`) |

Rule of thumb: **all client-input and domain-rule validation = `400`; `409` only for state/transition
conflicts; `404` for missing resources.** The `problem+json` `code` is the precise discriminator regardless
of status.

**Activation API** (`activation_api_design_spec.md` §5):

| Domain error | HTTP | `code` |
|---|---|---|
| `WorkItemNotFound` | `404` | `work_item_not_found` |
| `WorkerNotFound` | `404` | `worker_not_found` |
| `WorkItemTransitionNotAllowed` | `409` | `work_item_transition_not_allowed` |
| `WorkerNotAlive` | `409` | `worker_not_alive` |
| `NotLeaseHolder` | `409` | `not_lease_holder` |
| `LeaseStillValid` / `LeaseNotPresent` / `LeaseAlreadyHeld` | `409` | `lease_still_valid` / `lease_not_present` / `lease_already_held` |
| `StaleReport` | `409` | `stale_report` |
| `RequiredValueMissing`, malformed body | `400` | `required_value_missing` |
| missing/invalid worker token | `401` | — |

All coordination conflicts are `409` (state/fencing, not auth). `claim-next` with nothing to claim is
**not** an error → `204` (see §7).

---

## 6. Config API — wiring recipe

Base path `/v1/trust/config`; collection `/trust-relationships` (D13). Full catalog: `openapi_design_spec.md`
§7. Reuse the existing `TrustRelationshipMapper` (all methods `public static`):

```
toDomain(CreateTrustRelationshipRequest)                        → Result<TrustRelationship>   // create
updateBasicInfo(TrustRelationship existing, UpdateBasicInfoRequest)                → Result<TrustRelationship>
updateMetadataSource(TrustRelationship existing, MetadataSourceRequest)            → Result<TrustRelationship>
updateShibbolethSsoProfileConfiguration(existing, …Request)                        → Result<TrustRelationship>
updateReleasedAttributes(existing, …Request)                                       → Result<TrustRelationship>
toSummary(TrustRelationship)          → TrustRelationshipSummary   // response for most mutations
toDetail(TrustRelationship)           → TrustRelationshipDetail    // GET by id
toPage(List<TrustRelationship>, …)    → TrustRelationshipPage      // list
toMetadataSourceView / toReleasedAttributesView / toProfilesView(tr, Set<ProfileType>)  // sub-resource reads
```

The repository port is `io.jans.shibboleth.trust.persistence.config.TrustRelationshipRepository` (note: it
lives in **trust-adapters**, not the domain):

```java
Result<TrustRelationship>            save(TrustRelationship);
Result<TrustRelationship>            findById(Id);
Result<TrustRelationshipSummaryPage> list(TrustRelationshipQuery);   // 1-based paging + name/description filters
Result<Void>                         delete(Id);
```

**Canonical mutation controller shape** (e.g. `PUT …/basic-info`):

```java
Result<TrustRelationship> loaded = repository.findById(Id.of(uuidFromPath));
if (loaded.isFailure()) return toHttp(loaded);                       // 404 TrustRelationshipNotFound
Result<TrustRelationship> updated = TrustRelationshipMapper.updateBasicInfo(loaded.getValue(), body);
if (updated.isFailure()) return toHttp(updated);                     // 400/409 per §5.3
Result<TrustRelationship> saved = repository.save(updated.getValue());
return toHttp(saved, tr -> TrustRelationshipMapper.toSummary(tr));   // 200 + summary
```

**Create** → `toDomain(req)` → `save` → `201 Created` + `Location` + `toSummary`. **Delete** →
`repository.delete(Id)` → `204` (persistence-layer hard delete, D11 — no domain op, no body). **Lifecycle
actions** (`activate`/`cancel-activation`/`deactivate`) take no body: load, call the domain transition,
save, return `toSummary`; `409` on a disallowed transition.

Gotchas:

- `Id.of(UUID)` accepts the path UUID; `Id.getValue()` returns `Result<UUID>` (fails `IdNotAssigned` when
  unassigned) — but by the DTO boundary an id is always assigned (D10).
- **Name collision:** `TrustRelationshipSummary` (Jackson DTO) is in `dto.config`; the persistence
  projection `TrustRelationshipSummaryPage` and `TrustRelationshipQuery` are in `persistence.config`. Import
  deliberately.
- List filters/paging are persistence concerns — build a `TrustRelationshipQuery(displayNameContains,
  descriptionContains, page /*1-based*/, size)` from query params; the DTO layer only shapes the envelope
  (`{ items, page:{ size, number, total_elements, total_pages, number_of_elements } }`, D14).

---

## 7. Activation (M2M) API — wiring recipe

Base path `/v1/trust/activation` (D13). Consumers are **worker processes**, worker-audience bearer token
(AA6). Full catalog: `activation_api_design_spec.md` §6. Controllers call the **`WorkOrchestrator`**:

```java
Result<WorkItemActivation> onActivationRequested(TrustRelationshipRef, …)  // driven by config context, NOT an endpoint (AA1)
Result<WorkItemActivation> find(WorkItemId)                                // GET /work-items/{id}
Result<Worker>             registerWorker(WorkerId)                        // POST /workers
Result<Worker>             heartbeatWorker(WorkerId)                       // POST /workers/{worker}/heartbeat
Result<Worker>             findWorker(WorkerId)                            // resolve presented origin → live worker
Result<WorkItemActivation> claim(WorkItemId, Worker)
Result<ClaimOutcome>       claimNext(WorkItemType, Worker)                 // POST /work-items/claim-next
Result<WorkItemActivation> heartbeat(WorkItemId, Worker)                   // POST /work-items/{id}/heartbeat
Result<WorkItemActivation> report(WorkItemId, ActivationDiagnostics)       // POST /work-items/{id}/report
void                       sweepExpiredLeases()                            // internal cron, NOT an endpoint (AA1)
Result<WorkItemActivation> onActivationCancelled(TrustRelationshipRef)     // config-driven, NOT an endpoint (AA1)
```

Mappers (all `public static`): `WorkerMapper.toWorkerId(String origin)` (blank → `RequiredValueMissing`),
`WorkerMapper.toView(Worker)`, `WorkItemMapper.toView(...)`, `ActivationDiagnosticsMapper.toDomain(request)`.

Endpoint notes:

- **`POST /workers`** — body `{ origin }` → `toWorkerId` → `registerWorker` → `200` `WorkerView`.
- **`POST /workers/{worker}/heartbeat`** — `{worker}` path = URL-encoded `Origin` (`instance@host`) →
  `heartbeatWorker` → `200`/`404 worker_not_found`.
- **`POST /work-items/claim-next`** — body `{ origin, type }`; resolve `origin` via `findWorker`
  (authoritative liveness), then `claimNext(type, worker)`. **`200` + `WorkItemView` when claimed; `204`
  when `ClaimOutcome` is empty (nothing claimable — NOT an error); `409 worker_not_alive`.**
- **`GET /work-items/{id}`** → `find` → `WorkItemView`.
- **`POST /work-items/{id}/heartbeat`** — body `{ origin }`; `findWorker` → `heartbeat(id, worker)`.
- **`POST /work-items/{id}/report`** — body `ActivationDiagnosticsRequest` →
  `ActivationDiagnosticsMapper.toDomain` → `report(id, diagnostics)`. `NO_DATA` is a no-op that leaves the
  item `ASSIGNED`; any other status completes it.

**Concurrency = the domain's own fences, never HTTP ETag (AA2).** The `{id}` path is the episode fence;
the lease-holder fence is the reporter's `WorkerId`. Map fence violations to `409` (`stale_report`,
`not_lease_holder`) per §5.3.

Accessor-naming difference to watch: **config value objects use `getX()`; activation model uses bare-noun
accessors** (`workItem.id()`, `lease.expiresAt()`, `worker.registeredAt()`). Don't assume JavaBean getters
on activation types.

---

## 8. Boot-time wiring (the plumbing you own)

### 8.1 One `PersistenceEntryManager` per app

Build a single `io.jans.orm.PersistenceEntryManager` at startup and share it (it opens a connection pool and
scans DB metadata — expensive; build once, like the SQL ITs do via `SqlEntryManagerExtension`). For the SQL
backend:

```java
Properties p = new Properties();
p.put("sql#connection.uri", <jdbc url>);
p.put("sql#db.schema.name", <schema, e.g. "public">);
p.put("sql#auth.userName", <user>);
p.put("sql#auth.userPassword", <password>);
p.put("sql#connection.driver-property.serverTimezone", "UTC");   // REQUIRED — see persistence doc §4.5
p.put("sql#connection.pool.max-total", "10");
p.put("sql#password.encryption.method", "SSHA-256");
PersistenceEntryManager em = new SqlEntryManagerFactory() {{ create(); }}.createEntryManager(p);
```

All four repository impls take that manager plus base DN(s):

```java
new TrustRelationshipRepositoryImpl(em, "ou=trustRelationships,o=jans");
new WorkItemRepositoryImpl(em, "ou=trustActivationWorkItems,o=jans",
                                "ou=trustActivationEpisodes,o=jans");   // NOTE: two base DNs
new LeaseRepositoryImpl (em, "ou=trustActivationLeases,o=jans");
new WorkerRepositoryImpl(em, "ou=trustActivationWorkers,o=jans");
```

`WorkItemRepositoryImpl` is the only one taking **two** base DNs (work items + the current-episode pointer
branch). Make base DNs, JDBC URL, credentials, and the lease/heartbeat TTLs **externally configurable** (do
not hard-code). `serverTimezone=UTC` is not optional — timestamps round-trip through jans-orm's native date
codec and a wrong timezone silently corrupts them.

### 8.2 Build the `WorkOrchestrator`

Its constructor is private; use the factory (returns a `Result`, so check it):

```java
Result<WorkOrchestrator> orch = WorkOrchestrator.create(
    timeSource,        // TimeSource      → Instant now();  production impl = system clock
    leaseTtl,          // Duration        (configurable)
    heartbeatTtl,      // Duration        (configurable)
    events,            // ActivationEventSink   → void emit(ActivationEvent)
    finalizePort,      // FinalizeActivationPort→ void finalizeActivation(TrustRelationshipRef, ActivationDiagnostics)
    workItemRepo, leaseRepo, workerRepo);
```

Three collaborators you must supply:

- **`TimeSource`** — return `Instant.now()` in production. (A fixed clock is what the tests use.)
- **`ActivationEventSink`** — best-effort, in-memory sink is accepted for now (AP6/§10.3 of the persistence
  doc: no durable outbox this phase). A no-op or log-and-drop impl is fine to start.
- **`FinalizeActivationPort`** — **the cross-context bridge, and the one non-trivial adapter you must write.**
  When a worker reports success, `report(...)` calls
  `finalizeActivation(TrustRelationshipRef, ActivationDiagnostics)`, which must apply the result to the
  **config** aggregate. Implement it as:

  ```java
  (ref, diagnostics) -> {
      Result<TrustRelationship> tr = trustRelationshipRepository.findById(Id.of(ref.value()));
      if (tr.isFailure()) return;                       // or log; port returns void
      Result<TrustRelationship> finalized = tr.getValue().finalizeActivation(diagnostics);
      if (finalized.isSuccess()) trustRelationshipRepository.save(finalized.getValue());
  }
  ```

  Note the type crossing: activation carries `TrustRelationshipRef` (a UUID wrapper); config keys on
  `Id.of(uuid)`. This port is where the activation and config contexts meet — keep it thin and one-directional.

### 8.3 Scheduling & auth

- **`sweepExpiredLeases()`** is an internal scheduled job, not an endpoint (AA1). Run it on a timer.
- Config→activation demand (`onActivationRequested` / `onActivationCancelled`) is driven by the **config**
  side on TR `activate()` / `cancelActivation()`, via internal integration/events — **not** worker endpoints
  (AA1). Wire it server-side.
- **Bearer auth** on every operation (D7). Config API = user-audience token; activation API =
  worker-audience token (AA6). Token issuance is out-of-band. Binding the worker token subject to the
  presented `Origin` is a noted hardening step, not blocking.

---

## 9. Provisioning dependency (must be in place before the app runs)

The app does **not** create its own storage branches or schema — the **jans setup/installer owns that**
(persistence doc §4.5 and §10.2). Before the API can serve traffic, the target database must contain, under
`o=jans`, exactly these object classes / branches (renaming any of them breaks `find`/`persist` at runtime,
not compile time):

| Aggregate | Object class (= SQL table / LDAP objectClass) | Branch |
|---|---|---|
| Trust relationship | `jansTrustRelationship` | `ou=trustRelationships,o=jans` |
| Work item | `jansTrustActivationWorkItem` | `ou=trustActivationWorkItems,o=jans` |
| Lease | `jansTrustActivationLease` | `ou=trustActivationLeases,o=jans` |
| Worker | `jansTrustActivationWorker` | `ou=trustActivationWorkers,o=jans` |
| Current-episode pointer | `jansTrustActivationEpisode` | `ou=trustActivationEpisodes,o=jans` |

Timestamp columns must be a real `timestamp`/generalized-time type, never `varchar` (persistence doc §4.5).
The **canonical reference DDL** for the activation tables is the test fixture
`trust-adapters/src/test/resources/init-scripts/01-activation-init.sql`; production provisioning must create
the equivalent structures and stay in sync with it. Coordinate the branch/schema additions with whoever owns
the jans setup.

---

## 10. Suggested stack (implementer's choice, but align with jans)

The framework is not mandated, provided the contract in §5 holds. To match the wider Janssen ecosystem, the
natural choice is **JAX-RS (RESTEasy) + CDI (Weld)**, packaged as a WAR on the IdP's servlet container:

- Two JAX-RS `@ApplicationPath` roots (or one, with the `/v1/trust/config` vs `/v1/trust/activation` split
  by resource-class `@Path`).
- CDI producers for the singleton `PersistenceEntryManager`, the four repositories, the `WorkOrchestrator`,
  and the `FinalizeActivationPort`.
- One `ExceptionMapper`/response-builder implementing the central `Result`/`DomainError` → `problem+json`
  translator (§5). Because mappers already return `Result` and don't throw, most of the translator is a
  plain success/failure switch, not exception handling.
- A JAX-RS `ContainerRequestFilter` for bearer auth (distinct audiences per API, AA6).
- Serve the two OpenAPI YAMLs as static resources for discovery.

Whatever the framework: DTOs are `snake_case` JSON (D6); enums are `UPPER_SNAKE_CASE` on the wire matching
the domain names; `Duration` is ISO-8601; timestamps are ISO-8601 date-time; no endpoint accepts file bytes
(D8 — files are OOB tokens).

---

## 11. Testing expectations

- **Contract tests:** validate served responses against the committed OpenAPI schemas; assert every
  documented status code and the `problem+json` shape (including wrapper-unwrapping of
  `DomainObjectCreationFailed`/`UpdateFailed`).
- **Translator unit tests:** one case per `DomainError` subclass → (status, `code`). This table is the
  contract; test it exhaustively.
- **Integration tests against a provisioned DB:** reuse `trust-adapters/docker-compose.yaml` (Postgres) and
  the env-gated pattern (`-Dtrust.it.sql.uri`); assume-skip when the URI is absent so the suite stays green
  offline. The activation ITs already prove the two-worker race resolves to one winner at the repository
  level — the API ITs should prove the same through HTTP (two concurrent `claim-next` → exactly one `200`,
  the other `204`).
- Keep the domain and adapter suites green: `mvn -o -pl trust-domain,trust-adapters test`.

---

## 12. Definition of done

- [ ] New `trust-api` module in the reactor, depending on `trust-adapters`; dependency direction inward only.
- [ ] Every operation in both OpenAPI specs is served, with request/response bodies matching the committed
      schemas.
- [ ] Central `Result`/`DomainError` → `problem+json` translator, with the §5.3 status/code tables
      implemented verbatim and covered by tests.
- [ ] Bearer auth enforced on both APIs (distinct audiences).
- [ ] Singleton `PersistenceEntryManager`; repositories and `WorkOrchestrator` wired via DI from external
      config; `FinalizeActivationPort` bridges activation→config; `sweepExpiredLeases` scheduled.
- [ ] Contract + translator + env-gated HTTP integration tests pass; the offline build stays green.
- [ ] Runbook note: the exact object classes/branches from §9 must be provisioned by the jans setup before
      deployment.

---

## 13. Pitfalls checklist (things that will bite)

- `Result.getValue()` throws if you call it on a failure — always branch on `isSuccess()` first.
- No `Optional` in domain-facing code; use the null-object value types the domain already returns.
- Config value objects use `getX()`; activation model uses bare-noun accessors (`id()`, `expiresAt()`).
- `TrustRelationshipRepository` and its query/summary/page types live in **trust-adapters**
  (`persistence.config`), not the domain. `TrustRelationshipSummary` (DTO) is a *different* type in
  `dto.config`.
- `WorkItemRepositoryImpl` takes **two** base DNs; the other repos take one.
- Repository impls take `io.jans.orm.PersistenceEntryManager` (the interface), not `SqlEntryManager`.
- `serverTimezone=UTC` is mandatory; timestamp columns must not be `varchar`.
- `claim-next` empty result is `204`, not an error.
- `problem+json` for wrapper errors surfaces the **cause's** `code`.
- All input/domain-rule validation is `400`; `409` is reserved for state/fencing conflicts.

---

*This guide is intentionally a pointer-and-glue document: the domain, adapters, DTOs, mappers, and OpenAPI
contracts are the source of truth and already exist. The work is the transport that binds HTTP to them under
the `Result`/`problem+json` contract above.*
