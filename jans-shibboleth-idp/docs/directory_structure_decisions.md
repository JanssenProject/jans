# Directory Structure Decisions

## Revision — 2026-07-20: two modules (pure domain + adapters)

> **This revision supersedes the three-module structure recorded below** (kept for history). The
> `trust-persistence` module and the "start simple: persistence → `trust-dto`" projection note are
> withdrawn.

### What changed and why

A module boundary does two separable jobs: it defines a **deployment/versioning unit**, and it enforces a
**compile-time dependency fence**. For this project the three modules are always built, versioned, and
shipped together — so as *deployment units* the split was ceremony. But one fence is genuinely
load-bearing: **`trust-domain` must not import any framework** (Jackson, jans-orm, jakarta). That
guarantee is worth a real module; the rest is not.

We also recognised that the query-side DTOs (the **view** DTOs) are **isomorphic with read models** — a
listing "read model" *is* `dto.config.TrustRelationshipSummary`. And the DTO/API layer and the persistence
layer are both **adapters** (inbound and outbound) around the same domain core, so keeping them in separate
modules bought nothing and forced an awkward `trust-persistence → trust-dto` dependency.

**Decision: two modules.**

1. **`trust-domain`** — the pure domain. No frameworks. Kept as its own module *specifically* so purity is
   enforced by the compiler, not by review.
2. **`trust-adapters`** — one module for everything else: the inbound adapter (DTOs, mappers, OpenAPI spec)
   and the outbound adapter (jans-orm persistence entities, payloads, repositories), plus the REST
   controllers when they arrive.

### Canonical structure

```
jans-shibboleth-idp/
├── docs/                         ← design docs, architecture, test plans (human prose only)
├── trust-domain/                 ← pure domain model (no framework/annotation deps) — the enforced fence
│   └── src/main/java/io/jans/shibboleth/trust/…
└── trust-adapters/               ← inbound (DTO/API) + outbound (persistence) adapters
    └── src/main/
        ├── java/io/jans/shibboleth/trust/
        │   ├── dto/              request/ + view/ DTOs (views double as read models), mapper/
        │   └── persistence/      @DataEntry entries, payload/, mapper/, repositories
        └── resources/openapi/    the executable API contract (lives with the adapters it drives)
```

### Maven artifact names

| Module | ArtifactId |
|---|---|
| `trust-domain` | `jans-shibboleth-trust-domain` |
| `trust-adapters` | `jans-shibboleth-trust-adapters` |

### Dependency direction

- `trust-adapters` → `trust-domain` (adapters map to/from domain types)
- `trust-adapters` → `jans-orm` (+ a backend impl at runtime) and Jackson — frameworks live here, never in the domain
- `trust-domain` → (nothing framework-related)

### Migration note

The existing `trust-dto` module is renamed to `trust-adapters` and gains the `persistence` packages. The
physical rename is **deferred until persistence build-out**, so it lands as one coherent change rather than
disturbing the working `trust-dto` module now.

### Domain purity is now the *only* module fence

Because there is a single adapters module, cross-adapter references (persistence populating a view DTO) are
ordinary in-module calls. The sole architectural invariant a boundary still guards is: **nothing under
`trust-domain` imports a framework.** (If we ever collapse to one module, that invariant would move to an
ArchUnit/import-ban test; with two modules the compiler enforces it for free.)

---

## Discussion date: 2026-07-18

> **Superseded by the 2026-07-20 revision above.** Retained for history.

### Context

Planning the directory structure for `jans-shibboleth-idp` to accommodate:
- An OpenAPI spec for the domain
- DTOs and mappers for the domain objects
- A future persistence module

### Final directory structure

```
jans-shibboleth-idp/
├── docs/                        ← design docs, architecture, test plans (human prose only)
├── trust-domain/                ← pure domain model (unchanged)
│
├── trust-dto/                   ← NEW: DTOs + mappers + OpenAPI spec
│   ├── pom.xml
│   └── src/main/
│       ├── java/io/jans/shibboleth/trust/dto/
│       │   ├── config/
│       │   │   ├── TrustRelationshipDto.java
│       │   │   ├── MetadataSourceDto.java
│       │   │   ├── ProfileConfigurationDto.java
│       │   │   └── ...
│       │   ├── activation/
│       │   │   ├── WorkItemDto.java
│       │   │   └── ...
│       │   ├── shared/
│       │   │   └── ApiErrorDto.java
│       │   └── mapper/
│       │       ├── config/
│       │       │   └── TrustRelationshipMapper.java
│       │       └── activation/
│       │           └── WorkItemMapper.java
│       └── resources/
│           └── openapi/
│               └── trust-dto.yaml
│
└── trust-persistence/           ← NEW: persistence adapters (eventual)
    ├── pom.xml
    └── src/main/java/io/jans/shibboleth/trust/persistence/...
```

### Maven artifact names

| Module | ArtifactId |
|---|---|
| `trust-domain` | `jans-shibboleth-trust-domain` |
| `trust-dto` | `jans-shibboleth-trust-dto` |
| `trust-persistence` | `jans-shibboleth-trust-persistence` |

### Dependency direction

- `trust-dto` → `trust-domain` (DTOs know about domain types for mapping)
- API layer → `trust-dto`
- `trust-persistence` → `trust-domain`

### Decisions made

1. **`trust-dto` over `trust-model`**: `trust-domain` is already the domain model. `trust-model` would create confusion.
2. **`trust-dto` over `trust-contracts`**: DTO has zero semantic overhead — everyone instantly knows what's in that directory.
3. **`trust-dto` over `trust-adapter`**: Too ambiguous; persistence would also be an adapter in hexagonal terms.
4. **`trust-dto` over `trust-dto-mappings`**: Too verbose; mappers are a subordinate concern that don't need equal billing.
5. **OpenAPI YAML lives inside `trust-dto`**: The spec is an executable contract that drives DTO generation and must be versioned alongside the DTOs. It does NOT live in `docs/` (which is for human-authored prose like this file).
6. **Sibling modules**: Separated from existing `trust-domain` to keep the domain module pure (zero framework/annotation dependencies).

### Persistence and DTO projections

> **Superseded by the 2026-07-20 revision.** With persistence and DTOs in one `trust-adapters` module the
> cross-module question is moot: whole-object reads map to the domain aggregate (rehydration), while the
> query path populates the view DTO directly in-module. The note below is retained only for history.

**Recommended approach (start simple):** `trust-persistence` depends on `trust-dto`. Repository methods project directly into the DTO contract shape. No extra translation layer.

```java
// In trust-persistence
List<TrustRelationshipListItemDto> findActiveByNature(TrustNature nature);
```

**Fallback (if needed later):** Introduce separate persistence projection classes and a mapper layer when:
- Projections carry internal fields the API shouldn't expose
- Multiple persistence backends produce divergent raw shapes
- Persistence-specific annotations would pollute DTOs

Starting with the simple approach is fine — the mapper boundary can be introduced later without breaking the API. Starting with it is premature.

### Naming convention alignment

The naming follows existing Jans project conventions observed in sibling projects:
- `jans-auth-server/model` → artifact `jans-auth-model`
- `jans-auth-server/persistence-model` → artifact `jans-auth-persistence-model`
- `jans-core/model` → artifact `jans-core-model`
