# Implementing the Shibboleth IdP Trust Management REST APIs

> **Issue draft.** This is a brief for the API implementer, and the seed for the GitHub
> issue(s) that track the work. It restates the contracts in one place so the implementer does not
> have to reverse-engineer them — but it *will* drift. The **source of truth** is always the code:
> the wire contracts under `openapi/` and the Java public interfaces named below. Where this document
> and the code disagree, the code wins.

There are **three** APIs, and each is its own contract because each serves a different consumer, has a
different security posture, and has a different concurrency model:

| API | Base path | Consumer | Purpose |
|---|---|---|---|
| **Trust Config API** | `/v1/trust/config` | Humans (UI / CLI), user token | Declare *what* a trust relationship should be. |
| **Trust Activation API** | `/v1/trust/activation` | Worker processes, worker token | Reconcile that intent against the *actual* IdP, asynchronously. |
| **File Staging API** | `/v1/files` | Uploader + a store-backed claimer, OAuth2 scopes | Move file bytes out-of-band so they never ride the JSON contracts. |

Each has its own document in this folder:

- [`trust-config-api.md`](./trust-config-api.md)
- [`trust-activation-api.md`](./trust-activation-api.md)
- [`file-staging-api.md`](./file-staging-api.md)

This root explains **why** the split exists, the model that ties the three together, and the
conventions and wiring that all three share. Read it first; then read the per-API doc for whatever
you are building.

---

## 1. The problem, and why three APIs

An administrator needs to configure **trust relationships** for a Shibboleth IdP — SAML federations
and individual service providers: their metadata source, which SAML profiles are enabled and how,
and which attributes are released.

Turning a saved configuration into something the IdP actually honors is *not* itself configuration.
It means fetching and parsing metadata (which can be large or slow to reach), checking signatures and
trust, discovering entity IDs — work that is CPU- and memory-intensive and must run asynchronously,
possibly on several machines at once, without freezing the editing experience.

So the system separates three concerns that are tempting to fuse:

1. **Saying what you want** — the *desired state*. Fast, transactional, human-facing.
   → **Trust Config API**.
2. **Making it true** — reconciling desired state against the *actual* IdP. Slow, parallel,
   machine-facing.
   → **Trust Activation API**.
3. **Moving the raw bytes** of metadata/certificate files. A separate media and security concern
   (octet-stream transfer, size caps, content-type handling, scanning, quotas) that has no business
   on a JSON contract.
   → **File Staging API**.

Fusing them would drag worker/queue/lease vocabulary into the human-facing contract, and file-byte
concerns into both. Keeping them apart is the whole point.

---

## 2. Desired state vs. actual state — the worker model

The config API records **desired state**. When an admin finishes editing and activates a trust
relationship, it does **not** jump straight to live. It enters a transient `ACTIVATING` state, and the
config side raises an internal "activation requested" signal.

A coordination service (the **orchestrator**, exposed by the activation API) observes that demand and
turns it into a **work item** on a queue. **Workers** — ephemeral background processes — claim work
items, do the slow validation/metadata processing against the real IdP-facing world (the *actual
state*), and report the outcome. The orchestrator hands an authoritative report back to the trust
relationship, whose own state machine decides the result: success → `ACTIVE`, failure → back to
`READY`, no result yet → stays `ACTIVATING`.

The two sides live in **separate transactions, joined only by events** — so the model is *eventually
consistent*, and processing is *at-least-once* (a work item may be attempted more than once) with
*effectively-once finalization* (a given activation episode is finalized once). Workers must be
idempotent.

Plainly: **the config API records what the admin wants; a pool of workers reconciles that against
what the IdP actually has and reports back; the trust relationship's own state machine turns those
reports into its final state.**

---

## 3. The trust relationship lifecycle

Five states (`UPPER_SNAKE` on the wire). This machine is owned by the domain and is shared context
for both the config and activation docs.

```
                 set real source + a profile active
   ┌────────┐  ─────────────────────────────────►  ┌────────┐
   │ DRAFT  │                                       │ READY  │
   └────────┘  ◄─────────────────────────────────  └────────┘
        ▲        source→NONE or all profiles off        │  activate()
        │                                                ▼
        │                                          ┌────────────┐
        │       finalize: FAILED / cancelActivation│ ACTIVATING │◄─┐ re-activate
        │  ◄──────────────────────────────────────└────────────┘  │ (edit while ACTIVE)
        │       (source→NONE / all off)                 │ finalize: SUCCEEDED
        │                                                ▼          │
        │                                          ┌────────┐       │
        └───────── (source→NONE / all off) ────────│ ACTIVE │───────┘
                                                   └────────┘
                                        deactivate() │  ▲ activate() (re-evaluates readiness)
                                                      ▼  │
                                                  ┌──────────┐
                                                  │ INACTIVE │
                                                  └──────────┘
```

- **DRAFT** — new or incomplete: no real metadata source and/or no active profile.
- **READY** — has a real (non-`NONE`) metadata source **and** ≥1 active profile; eligible to activate.
- **ACTIVATING** — transient; an activation episode is in flight. This is the "locked for
  reconciliation" state.
- **ACTIVE** — validation succeeded; the relationship is live.
- **INACTIVE** — soft-disabled; deliberately inert until an explicit `activate()`.

Rules that matter to the implementer:

- Every path to live goes `READY → ACTIVATING → ACTIVE`. There is **no direct `READY → ACTIVE`**.
- **No structural edits while `ACTIVATING`.** Setting the metadata source, any profile configuration,
  or released attributes is rejected with `409` while activating — this keeps the activation target
  immutable within an episode, which is what makes a slow/stale worker report harmless. Only
  descriptive edits (display name / description) are allowed while activating, and they don't change
  state.
- **A `NO_DATA` finalize leaves the item `ACTIVATING`** — workers must explicitly report `SUCCEEDED`
  or `FAILED` to move it out.
- There is **no `ERROR` state**: a failed activation returns to `READY` carrying diagnostics.

---

## 4. Conventions shared by all three APIs

- **JSON is `snake_case`.** Enums are `UPPER_SNAKE_CASE` on the wire, matching the domain names.
  Durations are ISO-8601 (`PT5M`); timestamps are ISO-8601 date-time.
- **Contracts are OpenAPI 3.1**, under `openapi/`. They are the exact wire shape — every path, schema,
  and status code. Serve them as static resources for discovery.
- **Errors are RFC 7807 `application/problem+json`**, shape defined once in
  `openapi/components/common.yaml` (`Problem` / `Violation`). Each error carries a stable,
  machine-readable `code`; the `type` URI is `https://jans.io/shibboleth-idp/problems/{code}`. The
  `code` — not the HTTP status — is the precise discriminator.
- **Status mapping is uniform:** all client-input and domain-rule validation failures are **`400`**
  (deliberately *not* 422); **`409`** is reserved for state-transition and coordination/fence
  conflicts; **`404`** for missing resources; **`401`/`403`** for auth. Never leak internals in
  `detail`; unexpected failures are `500`.
- **Auth is bearer**, with **distinct audiences per API** (a user token for config, a worker token
  for activation; OAuth2 client-credentials scopes for file staging). Token issuance is out-of-band.
- **No file bytes on the config or activation contracts.** Files move through the File Staging API and
  are referenced by an opaque token/handle. See its doc.

### 4.1 The `Result` / `DomainError` contract (the single most important thing)

The domain and the mappers **never throw for domain outcomes** — they return
`io.jans.kernel.Result<T>` (`kernel/src/main/java/io/jans/kernel/Result.java`):

```java
Result<T> r = ...;
r.isSuccess() / r.isFailure()
r.getValue()   // T           — throws only if you call it on a failure (a programming bug)
r.getError()   // DomainError — throws only if you call it on a success
// factories: Result.success(v) / Result.failure(error)
```

There is **no `map`/`flatMap`, and no `java.util.Optional` anywhere in the domain** — absence is
modelled with null-object value types. Do not reintroduce `Optional` in domain-facing transport code.
Your controllers inspect the `Result`; they do not wrap domain calls in try/catch expecting
exceptions.

Every failure is a concrete `io.jans.kernel.DomainError` subclass. Build **one** central translator
that maps each subclass to `(HTTP status, code)` and emits `problem+json`. Keep the registry in one
place — all three APIs share the pattern. Wrapper errors that carry a cause
(`DomainObjectCreationFailed` / `DomainObjectUpdateFailed` / `DomainObjectConsistencyFailed`) should
surface the **cause's** `code`, not the wrapper's. Per-API error tables are in the per-API docs.

---

## 5. What exists, and what you are building

Already done and green — **reuse, do not reinvent**:

| Layer | Where | Status |
|---|---|---|
| Pure domain (aggregates, value objects, `Result`, errors, `WorkOrchestrator`, `FileStagingService`) | `trust-domain`, `file-staging-domain` | ✅ |
| Persistence adapters (jans-orm repository impls) | `trust-adapters/…/persistence/**`, `file-staging-adapters/…/persistence/**` | ✅ |
| Other adapters (document-store content store, time source, token generator, storage layout) | `file-staging-adapters/…/adapter/**` | ✅ |
| DTOs + DTO⇄domain mappers | `trust-adapters/…/dto/**`, `file-staging-adapters/…/dto/**` | ✅ |
| OpenAPI 3.1 contracts + shared components | `openapi/**` | ✅ |

**The gap to fill:** there is **no REST/transport layer yet** (no `@Path` classes exist in any
module). For each API you build the resource classes, wire them to the public interfaces named in the
per-API doc, translate `Result`/`DomainError` into HTTP + `problem+json`, and add the bearer-auth
filter and application bootstrap.

### 5.1 Modules & dependency direction

Active reactor modules today are the domains and their adapters. Dependency direction is inward and
must stay that way:

```
trust-api (new)  →  trust-adapters       →  trust-domain
file-api  (new)  →  file-staging-adapters →  file-staging-domain
                                          →  kernel  (io.jans.kernel: Result, DomainError, …)
```

Add the transport module(s) to the reactor. Keep transport out of the `*-adapters` modules so those
stay free of a servlet/JAX-RS dependency and remain unit-testable without a container. To match the
wider Janssen ecosystem the natural stack is **JAX-RS (RESTEasy) + CDI (Weld)** as a WAR, but the
framework is the implementer's choice provided the `Result`/`problem+json` contract in §4.1 holds.

### 5.2 Boot-time wiring you own

- **One `io.jans.orm.PersistenceEntryManager` per app**, built at startup and shared (it opens a
  connection pool and scans DB metadata — expensive). The repository impls take that manager plus
  their base DN(s); make base DNs, JDBC URL, credentials, and TTLs externally configurable. For the
  SQL backend `serverTimezone=UTC` is mandatory and timestamp columns must be a real timestamp type,
  never `varchar` — a wrong timezone silently corrupts round-tripped timestamps.
- **CDI producers / singletons** for the manager, the repositories, the `WorkOrchestrator`, and the
  `FinalizeActivationPort` bridge (see the activation doc).
- **Schedulers / server-driven signals** that are *not* endpoints: the orchestrator's
  `sweepExpiredLeases()` runs on a timer; config→activation demand
  (`onActivationRequested` / `onActivationCancelled`) is raised server-side when a trust relationship
  is activated/cancelled. Wire these internally.
- **Provisioning is not the app's job.** The jans setup/installer must create the storage branches and
  schema (object classes / tables) under `o=jans` before the app runs. The exact branch list is in
  the per-API docs; the canonical activation DDL reference is
  `docker/init-scripts/01-activation-init.sql`.

---

## 6. Definition of done (all three)

- [ ] Transport module(s) added to the reactor; dependency direction inward only.
- [ ] Every operation in all three `openapi/*.yaml` contracts is served, with request/response bodies
      matching the committed schemas.
- [ ] One central `Result`/`DomainError` → `problem+json` translator, with the per-API status/code
      tables implemented and exhaustively tested (one case per `DomainError` subclass).
- [ ] Bearer/OAuth2 auth enforced with the correct audience/scope per API.
- [ ] Singletons and DI wiring per §5.2; `FinalizeActivationPort` bridges activation → config;
      `sweepExpiredLeases` scheduled.
- [ ] Contract + translator + env-gated HTTP integration tests pass; the offline build stays green
      (`mvn -o`).
- [ ] Runbook note: the storage branches from the per-API docs must be provisioned before deployment.
