# TrustRelationship Persistence — Design Directives

> **Status:** design / pre-implementation. This document governs the **persistence adapter** for the
> `config` bounded context's aggregate root, `TrustRelationship`, using **`jans-orm`** as the storage
> backend. It is the persistence counterpart of [`openapi_design_spec.md`](./openapi_design_spec.md)
> (the DTO/API layer) and follows the same iterative, test-first workflow.
>
> **Module:** per the revised [`directory_structure_decisions.md`](./directory_structure_decisions.md)
> (two-module structure, 2026-07-20), the persistence code lives in the **`trust-adapters`** module —
> the single module that holds the inbound (DTO/API) and outbound (persistence) adapters — alongside a
> **pure `trust-domain`**. Where this doc says "the persistence package," it means the persistence
> packages within `trust-adapters`.

---

## 1. Purpose & scope

Persist and retrieve the `TrustRelationship` aggregate durably, so orchestration/config survives process
restarts and can run on more than one node. The domain remains the single source of truth; storage is a
projection of it, reconstructed back into a valid aggregate on read.

**In scope**
- A `jans-orm` entry (`@DataEntry`) for `TrustRelationship` and the JSON payload POJOs for its nested state.
- An entry ⇄ **domain** mapper, including **rehydration** (rebuilding a valid aggregate from stored fields).
- A `TrustRelationshipRepository` with CRUD + the **filtered, paged, sorted list** the config API already exposes (D14).
- A **read/query projection** so listing returns the view summary (`dto.config.TrustRelationshipSummary`) directly, never materializing the full aggregate (TP10).
- Id-assignment-on-persist (D10) and hard delete (D11).

**Out of scope (this phase)**
- The activation context (`WorkItem`/`Worker`) — separate doc, separate aggregate.
- REST controller / CDI wiring beyond what the repository needs.
- Optimistic-concurrency enforcement (D4; see §9 — and jans-orm offers no CAS to build it on anyway).
- Schema/DDL provisioning and the `ou=…` branch bootstrap (an ops concern; noted in §8).

---

## 2. Reference documents (source of truth)

| # | Document / source | Authoritative for |
|---|---|---|
| 1 | [`trust-domain/`](../trust-domain) source | The aggregate, its value objects, factories, and invariants — **the** source of truth |
| 2 | [`openapi_design_spec.md`](./openapi_design_spec.md) | Shared conventions (D4, D10, D11, D14) and the wire shapes the list projection feeds |
| 3 | [`directory_structure_decisions.md`](./directory_structure_decisions.md) | Module name, artifact id, dependency direction |
| 4 | `jans-orm/` (annotation, core, model, filter, sql) | Persistence annotations, `PersistenceEntryManager`, `Filter`, paged search |

---

## 3. Decisions (settled)

Prefix **TP** ("trust persistence"). Changing one requires updating this section first.

| # | Decision | Rationale |
|---|---|---|
| TP1 | **Whole-object persistence maps to the domain aggregate; the query projection is the view DTO.** The `save`/`findById` path reconstructs a validated `TrustRelationship` via rehydration (never a DTO). The list/query path returns the **view summary DTO** directly (`dto.config.TrustRelationshipSummary`, which *is* the read model — TP10). Both the persistence code and the DTOs live in the same **`trust-adapters`** module (two-module structure), so there is no cross-module hop. | The domain is the source of truth and the only place invariants are enforced, so the write model must load through it (loading validates stored data). A read projection is shaped for its view and needs no invariants, so returning the view DTO directly is correct CQRS — and, within one adapters module, carries no dependency-direction cost. |
| TP2 | **Queryable fields are real columns; the rest is JSON.** The fields the config API filters/sorts on — `id`, `displayName`, `description`, `nature`, `status`, `version`, and `entityIds` — are first-class `@AttributeName` attributes. Everything else (metadata source, the six profile configs, released attributes, activation diagnostics) is stored via `@JsonObject`. | `findPagedEntries` + `Filter` can only filter/sort on real attributes. The rich sub-structure is never queried by field, so JSON keeps the schema small and evolvable (the `CustomScript`/`Scope` pattern in jans-orm). |
| TP3 | **Dedicated persistence payload POJOs for the JSON blobs**, structured to the same wire conventions as the DTOs (enums as `UPPER_SNAKE` names, `Duration` as ISO-8601 strings, flows as string lists, per D16) but owned by the `trust-adapters` persistence package. | Keeps the JSON stable and independent of the API contract (an API shape change is not a storage migration), while making the entry⇄domain mapping mechanical. Decided in §7 (dedicated payloads, not the request/view DTOs). |
| TP4 | **Domain enums are stored as plain strings and converted in the mapper.** The entry/payload POJOs do **not** make domain enums implement `jans-orm`'s `AttributeEnum`. | The domain must stay framework-free. A String column + `.name()`/`valueOf(...)` in the mapper keeps annotations out of `trust-domain`. |
| TP5 | **Id is assigned on first persist (D10).** `create()` yields `Id.unassigned()`; `save()` of an unassigned aggregate generates the UUID, forms the DN, persists, and returns the aggregate **rehydrated with its assigned id** (version unchanged). | Matches D10 ("unassigned until persistence"). Immutability means `save` returns a new instance rather than mutating. |
| TP6 | **Hard delete via the entry manager (D11).** `delete(id)` removes the entry; there is no domain delete op. | D11 — removal is storage lifecycle, not aggregate behaviour. |
| TP7 | **Optimistic concurrency stays deferred (D4), and is not faked.** `version` is persisted and returned, but writes are last-write-wins for now. jans-orm exposes **no** `LockModeType`/CAS/conditional-update, so there is nothing to enforce it with at the store. | D4 already defers it; §9 records the future options honestly rather than pretending the store supports a guard it doesn't. |
| TP8 | **The repository is the adapters module's own interface (not a domain port) — for now.** It returns `Result<TrustRelationship>` for the write model and the view summary for queries. | The `config` context has no domain service that needs a repository port (unlike the activation orchestrator). If one emerges, promote the interface into the domain then. |
| TP9 | **Backend is `jans-orm` (any supported backend).** No dependence on SQL-only features. | User decision; and TP7 means we need no SQL-only locking primitive. Substring/paged/sorted search is supported across backends via `Filter` + `findPagedEntries`. |
| TP10 | **Read model = the view DTO (CQRS-lite).** Listing — and any query that doesn't need the whole aggregate — returns the existing view summary `dto.config.TrustRelationshipSummary` (`{ id, display_name, description, nature, status, version }`), populated directly from a **reduced-attribute projection entry** (`TrustRelationshipSummaryEntry`) mapped to the same `jansTrustRelationship` object class but declaring only the summary columns. No `@JsonObject` blob is fetched or deserialized, no profiles / metadata / attributes are rebuilt, and no aggregate invariants run. Whole-object ops (`save`/`findById`) still go through the full aggregate. There is **no** domain read-model type. | Materializing the entire aggregate per row to show six fields is the waste to avoid; jans-orm's column-restricted `findPagedEntries` reads only what the list shows. The view DTO already exists as a dumb data holder and *is* the read model; with persistence and DTOs in one module (`trust-adapters`), the projection maps straight to it. (An earlier draft placed a `TrustRelationshipSummary` read model in `trust-domain`; that conflated a query artifact with the domain model and is dropped.) |
| TP11 | **Command mappers vs query projections are distinct pipelines.** *(a)* **Command mappers** (in `dto/mapper/`) translate **domain ↔ DTO**: request → domain operation, and a domain object → its response DTO (`toSummary(TrustRelationship)`, `toDetail`, …). *(b)* **Query projections** produce a read model **from the store, bypassing the domain**: `TrustRelationshipSummaryEntry` → view summary. These live on the **query/persistence side** (a repository projection), **never in `dto/mapper/`**. *(c)* Envelope assembly (`toPage`: items + total → `PageMetadata`) is **packaging**, not translation, and needs no domain object. The aggregate ⇄ entry mapper (`persistence/mapper/`) is a third, persistence-internal mapping — also not a domain↔DTO mapper. | The read side bypasses the domain **by design** (CQRS). Filing a store→DTO projection under `dto/mapper/` conflates it with genuine domain translations and is the source of the "this mapper isn't mapping a domain object" unease. Keeping `dto/mapper/` strictly domain↔DTO preserves the intuition that a mapper translates a domain object; the projection is honestly a query concern. |

---

## 4. Storage shape — the `@DataEntry`

- **Object class:** `jansTrustRelationship` (convention `jans` + PascalCase noun).
- **Tree:** `ou=trust-relationships,o=jans`; per-entry DN `inum=<uuid>,ou=trust-relationships,o=jans`.
- **Primary key:** the DN (`@DN dn`, inherited from `BaseEntry`) — the jans convention. `inum` is the stable id attribute (part of the DN, `ignoreDuringUpdate`).
- **Base class:** extend `io.jans.orm.model.base.BaseEntry` (gives the `@DN` `dn` field).

`TrustRelationshipEntry` (in the `trust-adapters` persistence package) — attribute map:

| Domain field | Java type on entry | `@AttributeName` | Storage | Notes |
|---|---|---|---|---|
| `id` (`Id`→UUID) | `String` | `inum` (`ignoreDuringUpdate`) + `@DN` | in DN (primary key) | authoritative id; UUID string; DN = `inum=<uuid>,…` |
| `displayName` | `String` | `displayName` | column | list **filter** (substring) + **sort** |
| `description` | `String` | `description` | column | list **filter** (substring); may be `""` |
| `nature` (`TrustNature`) | `String` | `jansTrustNature` | column | `INDIVIDUAL`/`AGGREGATE` (TP4) |
| `status` (`TrustStatus`) | `String` | `jansTrustStatus` | column | `DRAFT`…`INACTIVE` (TP4) |
| `version` (`Version`→int) | `Integer` | `jansTrustVer` | column | read-only to API (D4) |
| `discoveredEntityIds` | `List<String>` | `jansEntityId` | multi-valued attr | URI strings; queryable later ("who references X") |
| `metadataSource` | `MetadataSourcePayload` | `jansMetadataSrc` | `@JsonObject` | polymorphic, §6 |
| 6 profile configs | `ProfilesPayload` | `jansProfiles` | `@JsonObject` | all six in one payload, §7 |
| `releasedAttributes` | `ReleasedAttributesPayload` | `jansReleasedAttr` | `@JsonObject` | §7 |
| `activationDiagnostics` | `ActivationDiagnosticsPayload` | `jansActivationDiag` | `@JsonObject` | §7 |

`@DataEntry(sortBy = "displayName", sortByName = "displayName")` sets the default list ordering.

The queryable columns are the **sole** storage for those six/seven fields (they are *not* duplicated inside
the JSON payloads), so there is no risk of divergence; rehydration combines columns + payloads.

---

## 5. Rehydration (entry → domain)

The mapper rebuilds a **valid** aggregate using the domain's own factories (the loader must satisfy the
same invariants the data satisfied when written). Reconstruct via `TrustRelationship.builder()` — the
**create-new path** (`original == null`), which skips transition-status recomputation and the version bump
but still runs `TrustInvariants.enforce` + `TrustOperationRestrictions.enforce` (operationType `NONE`).

Field-by-field factory calls (from the domain inventory):
- Scalars → VOs: `Id.of(uuid)`, `DisplayName.of(str)`, `Description.of(str)`, `TrustNature.valueOf`, `TrustStatus.valueOf`, `Version.of(int)`.
- `discoveredEntityIds`: `EntityIds.builder().addAll(map each String → EntityId.of(URI)).build()`.
- `metadataSource`: switch on payload `type` (§6).
- profiles: rebuild each via its `Builder` seeded from `SamlProfileConfigurationDefaults.<profile>()`, overriding every stored field through the Support VOs (`CommonConfigurationSupport`, `AuthenticationConfigurationSupport`, …).
- `releasedAttributes`: `ReleasedAttributes.builder().addAll(each → ReleasedAttribute.of(Id.of(uuid), displayName)).build()`.
- `activationDiagnostics`: `ActivationDiagnostics.of(status, Origin.of(str), logEntries, startedAt, completedAt)`; entries via `ActivationLogEntry.of(...)`, or `ActivationDiagnostics.none()` when absent.
- assemble: `TrustRelationship.builder().withId(...).withDisplayName(...)….build()`.

Every factory returns `Result<T>`; any failure on load is **data corruption** and surfaces as
`DomainObjectConsistencyFailed` (config error family) — the repository returns `Result.failure(...)`, it
does not throw.

Writing (domain → entry) is the inverse and total (a valid aggregate always maps): read accessors already
expose everything (`getId`, `getDisplayName`, …, `getMetadataSource().getType()`, the profile Support
getters). No new domain read surface is required.

**Two small domain additions** this needs (both in the spirit of prior TDD'd additions like
`ValidityPeriod.until` and the adapter parse errors):
1. Confirm/ensure a rehydration path that does not bump `version` (the `builder()` create-new path already provides this — verify with a round-trip test rather than adding API).
2. `DomainObjectConsistencyFailed` mapping for load-time invariant failures (the error type already exists in `config/error/`).

Neither requires annotations or a store dependency in the domain.

---

## 6. Polymorphic metadata source (JSON)

`MetadataSourcePayload` carries a `type` discriminator (`MetadataSourceType` name) plus the union of
variant fields; the mapper switches on `type`:

| `type` | payload fields | domain factory |
|---|---|---|
| `NONE` | — | `NoMetadataSource.getInstance()` |
| `FILE` | `filePath` | `FileMetadataSource.of(filePath)` |
| `URI` | `uri` | `UriMetadataSource.of(URI)` |
| `MDQ` | `baseUrl` | `MdqMetadataSource.of(URI)` |
| `UPSTREAM` | `parentId`, `entityId` | `UpstreamMetadataSource.of(Id.of(parentId), EntityId.of(URI))` |
| `MANUAL` | `entityId`, `validUntil` (ISO-8601), `acs{location,binding,index,isDefault}`, `signingCert{type,data}` | `ManualMetadataSource.builder()…build()`; `ValidityPeriod.until(Instant)`; `AssertionConsumerService.of(...)`; `SamlBinding` by name; cert = `NoCertificateInfo` or `SamlX509CertificateInfo.fromBase64CertificateData(data)` |

This mirrors the `oneOf`+discriminator shape the DTO layer already uses for metadata sources, so the
persistence payload and the API view stay structurally familiar (but independent — TP1/TP3).

`CertificateInfo` is a nested polymorphism inside `MANUAL`: a `signingCert.type` of `NONE`/`X509` selects
`NoCertificateInfo` vs `SamlX509CertificateInfo`.

---

## 7. Profiles / released-attributes / diagnostics payloads

`ProfilesPayload` holds six sub-objects (one per profile). Each sub-object mirrors that profile's Support
VOs, with **enums as `UPPER_SNAKE` strings, `Duration` as ISO-8601 strings, flows/name-ids as string
lists** — identical to the D16 wire conventions. The mapper rebuilds each profile through its `Builder`
seeded from `SamlProfileConfigurationDefaults`, so any field absent from an older stored blob falls back to
the documented default (forward-compatible reads).

`ReleasedAttributesPayload` = list of `{ id (uuid), displayName }`. `ActivationDiagnosticsPayload` =
`{ status, origin, startedAt, completedAt, logEntries: [{ timestamp, level, message }] }`.

**Decided: dedicated payloads (TP3).** The blob payloads are their own POJOs in the `trust-adapters`
persistence package, distinct from the `dto` request/view types even though the two are structurally
similar. This matters *even within one module*: the stored-blob format must not be the API wire shape, or
an API change silently becomes a storage migration. The cost is some deliberate duplication that the
mechanical, well-tested mapper absorbs. (Note this is the opposite call from the read side, TP10, where the
view DTO *is* the read model — a read projection is meant to track its view; a stored write-blob is not.)

---

## 8. Repository API, read models, listing, id lifecycle, deletion

`TrustRelationshipRepository` (interface in the `trust-adapters` persistence package):

```java
Result<TrustRelationship> save(TrustRelationship tr);        // insert (assign id) or update
Result<TrustRelationship> findById(Id id);                   // full aggregate, by DN; not-found → config error
Result<TrustRelationshipSummaryPage> list(TrustRelationshipQuery query);   // view summaries (TP10)
Result<Void> delete(Id id);                                  // hard delete (D11)
```

`TrustRelationshipSummaryPage` is a lightweight holder: `List<dto.config.TrustRelationshipSummary>` items
plus the total count; the existing `TrustRelationshipPageMapper`/`PageMetadata` build the D14 envelope
(all in-module).

### Two read paths (TP10)

- **Whole-object read (`findById`)** rehydrates the full aggregate (§5): all columns + every `@JsonObject`
  blob → a validated `TrustRelationship`. Used by GET-detail and by every mutation (load → transition → save).
- **Query read (`list`)** never touches the aggregate. A **query projection** on the repository/query side
  queries a **reduced-attribute projection entry**, `TrustRelationshipSummaryEntry`
  — `@DataEntry @ObjectClass("jansTrustRelationship")` declaring **only** `inum`, `displayName`,
  `description`, `jansTrustNature`, `jansTrustStatus`, `jansTrustVer` — and builds the view summary
  `dto.config.TrustRelationshipSummary` directly. No blob is fetched or deserialized; no invariants run; **no
  domain object and no `dto/mapper/` translation are involved** (TP11). `toPage` then *envelopes* the
  already-projected summaries + total into the D14 page — packaging, not translation.

The existing `dto.config.TrustRelationshipSummary` (a dumb data holder) *is* the read model, reached by two
distinct routes for two purposes: the **create** path translates the just-built aggregate via the command
mapper `TrustRelationshipMapper.toSummary(TrustRelationship)` (a genuine domain→DTO translation); the
**list** path projects it from the store (the query projection above). Same output shape, two pipelines —
kept as one class while identical; split only if they diverge (TP11).

- **Save / id assignment (TP5):** if `tr.getId().isNotAssigned()`, generate a UUID, build the DN, `persist`;
  else `merge`. Either way, reload/rehydrate and return the aggregate carrying its assigned id.
- **Listing (D14) → column-restricted `findPagedEntries`:** the query object carries `displayName`/`description`
  substring filters + 1-based `page`/`size`. Build a `Filter` with
  `Filter.createSubstringFilter("displayName", null, new String[]{ term }, null)` (and the same for
  `description`), combined with `createANDFilter`/`createORFilter`; pass the summary attribute names as
  `ldapReturnAttributes`, `sortBy = "displayName"`, `SortOrder.ASCENDING`, `start = (page-1)*size`,
  `count = size`. `PagedResult.getTotalEntriesCount()` → `total_elements`; `total_pages = ceil(total/size)`.
  The DTO layer then shapes the D14 envelope from this already-paged slice — paging/filtering stays a
  persistence concern, exactly as D14 states.
- **Delete (TP6):** `entryManager.remove(dn, TrustRelationshipEntry.class)` (or `remove(entry)`).
- **Branch bootstrap:** `ou=trust-relationships,o=jans` must exist (via `SimpleBranch`), created at
  install/first-run — an ops/wiring concern, noted not built here.

---

## 9. Concurrency, durability, clustering

- **Durability:** persisting the aggregate is the whole point — a restart reloads state; no in-memory map to lose.
- **Concurrency (TP7):** two nodes updating the same TR is currently **last-write-wins**. `version` is stored
  and bumped by the domain on modify, but jans-orm has no conditional update to reject a stale write. Future
  options, when D4 is lifted: (a) app-level read-check-write of `jansTrustVer` — a race window remains, so
  weak; (b) a DB-level guard outside jans-orm (SQL `UPDATE … WHERE jansTrustVer = ?`), which sacrifices
  backend portability; (c) an `ETag`/`If-Match` API layer (D4/§5.4 of the API spec) translated to (a)/(b).
  This doc does not choose; it records that the store gives us nothing for free.

---

## 10. Module layout, dependencies, testing

Two modules (per the revised `directory_structure_decisions.md`): a **pure `trust-domain`** and a single
**`trust-adapters`** holding the inbound (DTO/API) and outbound (persistence) adapters. This design touches
only `trust-adapters` — `trust-domain` is unchanged.

```
trust-domain/  (pure; artifact jans-shibboleth-trust-domain) — UNCHANGED by this design
  └── io/jans/shibboleth/trust/…       aggregate, value objects, invariants, transitions (no frameworks)

trust-adapters/  (artifact jans-shibboleth-trust-adapters — merged DTO + persistence [+ future api])
  └── io/jans/shibboleth/trust/
      ├── dto/
      │   ├── request/ + view/ DTOs   (view summaries double as read models)
      │   ├── mapper/                 domain ↔ DTO translations ONLY — command pipeline (TP11):
      │   │                           requests → domain ops; domain objects → response DTOs
      │   └── resources/openapi/
      └── persistence/
          ├── TrustRelationshipEntry.java          (@DataEntry, columns + @JsonObject fields)
          ├── TrustRelationshipSummaryEntry.java   (@DataEntry, same object class, summary columns only — TP10)
          ├── payload/  MetadataSourcePayload, ProfilesPayload, ReleasedAttributesPayload, ActivationDiagnosticsPayload
          ├── mapper/   TrustRelationshipEntryMapper  (aggregate ⇄ entry, rehydration — a persistence-internal
          │                                            mapping, NOT a domain↔DTO mapper)
          ├── TrustRelationshipRepository + impl    (wraps PersistenceEntryManager; the query projection
          │                                          TrustRelationshipSummaryEntry → view summary lives here — TP11)
          └── TrustRelationshipQuery / TrustRelationshipSummaryPage
```

> The current `trust-dto` module becomes `trust-adapters` (rename + add the persistence packages). That
> physical rename is deferred until persistence build-out, so it lands as one coherent change.

- **Dependencies:** `trust-adapters → trust-domain` + `jans-orm` (`annotation`, `core`, `model`, `filter`;
  a backend impl such as `sql` at runtime) + Jackson (already used by the DTOs). `trust-domain` depends on
  nothing framework-related — it is the **one boundary kept as a separate module** to guarantee domain
  purity at compile time (adapters purity is not a goal; adapters legitimately use frameworks).
- **Testing:**
  - *Mapper round-trip (fast, no DB):* domain → entry → domain equals the original, for a matrix of aggregates
    (every metadata-source variant, every profile customized, released attributes, diagnostics present/absent,
    unassigned vs assigned id). This is the bulk of the value and needs no backend.
  - *Query projection (fast, no DB):* `TrustRelationshipSummaryEntry` → `dto.config.TrustRelationshipSummary`
    builds the six fields — a query-side projection, no domain object, no `dto/mapper/` involvement (TP11).
  - *Repository integration:* against jans-orm's SQL backend (H2/MySQL/Postgres test harness) — save/find/list
    (filter+page+sort)/delete, plus a check that `list` returns summaries **without** materializing the blobs
    (e.g. a row whose stored blob is intentionally malformed still lists fine). Kept separate from the fast suite.

---

## 11. Risks & follow-ups

- **Two domain `equals`/`hashCode` bugs — ✅ fixed.** `ShibbolethSsoProfileConfiguration.hashCode()` hashed
  `samlAssertionConfigurationSupport` twice and omitted `samlConfigurationSupport`; and
  `Saml2ArtifactResolutionProfileConfiguration.equals()` compared `samlConfigurationSupport` to itself
  instead of to the other instance. Both let a change confined to `samlConfigurationSupport` escape the
  aggregate's value-equality check (and thus the "modified → bump version" decision). Fixed in
  `trust-domain` with regression tests (`ProfileConfigurationEqualityTests`, 4 tests).
- **`Description` may be `""`** (never null); the substring filter and rehydration must treat empty as a
  present-but-empty value, not absent.
- **Stored-blob forward compatibility:** seeding profile rebuilds from `SamlProfileConfigurationDefaults`
  means a new profile field added later reads as its default from old rows — good, but note it so a future
  field addition is a conscious "defaults on read" choice.
```
