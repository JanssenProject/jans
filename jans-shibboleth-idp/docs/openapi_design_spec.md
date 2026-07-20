# OpenAPI Specification & DTO / Mapper Design Directives

> **Status:** design / pre-implementation. This document guides a **semi-automated, iterative,
> test-first** process that produces (1) OpenAPI specifications and (2) the DTOs and mappers that
> realise them, for the trust domain modelled in [`trust-domain/`](../trust-domain).
>
> **Companion test plan:** [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md) (created on first
> iteration) drives the TDD implementation of DTOs and mappers, mirroring the style of the existing
> [`*_tests.md`](./asynchronous_activation_tests.md) plans.

---

## 1. Purpose & Scope

Produce a versioned, executable API contract and its Java DTO/mapper layer for the trust domain —
**and nothing else**. The domain is the single source of truth; the API is a projection of it.

**In scope**

- OpenAPI 3.1 specification(s) covering the trust domain.
- Request/response DTOs and the mappers that translate them to/from domain types.
- A TDD test plan for the DTOs and mappers.

**Out of scope (this phase)**

- Persistence (see [`directory_structure_decisions.md`](./directory_structure_decisions.md) — `trust-persistence`).
- REST controller / framework wiring (the transport implementation).
- The out-of-band (OOB) file-upload mechanism (metadata files, certificates) — devised elsewhere.
- The out-of-band access-token issuance process (assumed; see §5.2).

---

## 2. Reference Documents (source of truth, in priority order)

| # | Document | Authoritative for |
|---|----------|-------------------|
| 1 | [`trust-domain/`](../trust-domain) source | Types, operations, value objects — **the** source of truth |
| 2 | [`tr_invariants_and_state_transition_rules.md`](./tr_invariants_and_state_transition_rules.md) | Allowed operations per state; invariants → validation & 409 semantics |
| 3 | [`asynchronous_activation.md`](./asynchronous_activation.md) | Activation coordination domain (drives the M2M API, §8) |
| 4 | [`directory_structure_decisions.md`](./directory_structure_decisions.md) | Module layout, artifact names, dependency direction |

> **Search boundary:** during this process, **no directory other than `trust-domain` is read for
> domain code.** DTOs/mappers/spec are written under `trust-dto` (§9).

---

## 3. Architectural Decisions (settled)

These are decided. Later sections elaborate; changing one requires updating this section first.

| # | Decision | Rationale |
|---|----------|-----------|
| D1 | **Two APIs, two specs.** A user-facing **config API** and a machine-to-machine **activation API**. | Different consumers, security posture, stability guarantees, and concurrency models. Mirrors the `config` vs `activation` bounded contexts and the existing `dto/config` vs `dto/activation` split. |
| D2 | **Config API first.** Fully specify + implement it, one endpoint at a time. The activation API is stubbed now (§8) and designed in a later phase. | Higher value, user-facing; the activation protocol is still pre-implementation. |
| D3 | **Command sub-resources**, not coarse PUT/PATCH-and-diff. Each domain operation maps to its own endpoint. | The domain is operation-based; each mutation has its own transition rules. 1:1 endpoints keep those rules explicit and request bodies small. |
| D4 | **Optimistic concurrency deferred.** `version` is exposed **read-only** in responses; not enforced at the API yet. | Keeps first cut simple. ETag/`If-Match` (§5.4) can be layered on later **without changing any request body schema**. |
| D5 | **RFC 7807 `application/problem+json`** for all errors, shared by both APIs. Each `DomainError` subclass maps to a stable machine-readable `code`. | Interoperable, tooling-friendly, and a natural target for the `Result`/`DomainError` model. |
| D6 | **`snake_case` (lower)** for every property name in requests and responses; this propagates to DTO field JSON bindings. | Consistency across the contract. |
| D7 | **Bearer authentication** with an access token obtained out-of-band. | Per original directive. |
| D8 | **No REST endpoint accepts file bytes.** Files (metadata, certificates) are referenced by an OOB-produced handle. | Per original directive; keeps the contract free of multipart concerns. |
| D9 | **OpenAPI 3.1** (JSON Schema alignment, better `null`/`oneOf` support for the polymorphic metadata sources). | — |
| D10 | **Assigned identity at the DTO boundary.** `id` is unassigned only *inside* the domain at `create()`; by the time a TR reaches the DTO layer it has been persisted and **has an id**. DTOs therefore treat `id` as **always present**. Adding an id does not change TR semantics. | The "unassigned until persistence" rule is a domain-internal detail; it does not cross the API boundary. |
| D11 | **Deletion is in scope, as a persistence-layer concern.** `DELETE /trust-relationships/{id}` exists; there is **no** domain delete operation. No domain change is required for a hard delete. | Removal is storage lifecycle, not aggregate behaviour. A *deletion guard* (if ever wanted) would be a read-only domain predicate, not a state transition (see §10). |
| D12 | **Error `type` namespace:** `https://jans.io/shibboleth-idp/problems/{code}`. | Under the agreed `https://jans.io/shibboleth-idp/` base; a `/problems/` segment leaves room for other URI-identified resources. |
| D13 | **Path scheme:** `/v1/trust/{context}/{resource}` — version is the **leading** segment, then bounded context (`config`, later `activation`), then a **plural** resource collection (`trust-relationships`). Creation is `POST` to the collection (no verb in path); only non-CRUD lifecycle operations use `/actions/{verb}` (D3). | Version-first prefix (per preference); the `trust/config` vs `trust/activation` context makes the two-API split visible in the URL; verbs in paths stay reserved for domain actions with no CRUD equivalent. |
| D14 | **List envelope & pagination.** Collections return `{ items: [...], page: { size, number, total_elements, total_pages, number_of_elements } }` — a `PagedModel`-style metadata block, snake_cased. Paging is **1-based** (`page`/`size` query params; `page.number` 1-based, first page = 1). Filtering and paging are **persistence concerns**; the DTO layer only shapes the envelope from an already-filtered, already-paged slice plus its metadata. | Familiar metadata shape; 1-based reads naturally; query logic stays out of the DTO/domain layers. |
| D16 | **Profile setters are `PATCH` + partial-override.** Each of the six profiles has its own endpoint (`PATCH .../profiles/{profile}`) with a profile-specific request whose fields are all optional; the mapper seeds the domain builder `from(the current profile config)` and overrides only the fields present. Omitted = unchanged. Wire types: enums as UPPER_SNAKE strings, `Duration` as ISO-8601 duration strings, flows/nameid-precedence as string arrays. | A profile can't be built without all fields, so building is always `from(existing)`; partial-override spares clients from resending ~20 fields and there is no "set to null" case (every field is a value/enum). `PATCH` (not `PUT`) because the semantics are partial. |
| D15 | **Descriptive updates merged.** `PUT /trust-relationships/{id}/basic-info` sets display name + description together, backed by a **new domain op** `TrustRelationship.updateBasicInfo(displayName, description)` (single atomic `build()` → version bumps at most once). `display_name` required (non-blank); `description` optional/nullable (absent/null → empty, per the domain). Full-block replace: omitting `description` clears it. | The two rule-free descriptive fields are naturally edited as a unit; a backing domain op keeps the endpoint 1:1 with a single operation (D3) and avoids the double version-bump of chaining two ops in the mapper. |

---

## 4. Ground Rules (process constraints)

- **TDD for DTOs and mappers.** Write the test-plan entry and the failing test *before* the mapper. The
  test plan lives in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md).
- **One endpoint at a time.** Pick the **first endpoint in §7 whose `Done` box is unchecked**, take it
  all the way through (spec → DTOs → mappers → passing tests), then tick it.
- **"Done" for an endpoint means:** its OpenAPI operation is written; request/response DTOs exist;
  mappers to/from domain types exist; and mapper tests pass. Only then is the box ticked.
- The OpenAPI YAML lives inside `trust-dto` (§9), not in `docs/`. `docs/` is human prose only.
- Every mapping must be justified by the domain source (§2 #1) — never invent fields the domain lacks.
- **The YAML must be legible for human review.** It is authored/generated to be read, not just parsed:
  - Logical ordering (`info` → `servers` → `security` → `tags` → `paths` → `components`), paths grouped
    by resource, operations in a consistent verb order.
  - Meaningful `operationId`s and `$ref` component names; **schemas factored into named `components`**
    and reused — no large inline schemas duplicated across operations.
  - A `summary`/`description` on every operation and a `description` on every non-obvious schema and
    field; representative `example`s where they aid understanding.
  - Consistent 2-space indentation, one concern per block, reasonable line length, no tool-generated
    noise (empty stanzas, machine hashes, redundant boilerplate).
  - A reviewer should be able to understand an endpoint's contract from the YAML alone, without running
    a generator or reading the DTO code.
- **Request and response schemas are modeled iteratively, and reviewed before mappers are written**
  (see the §6 *schema-review checkpoint*). The naive projection (map every domain field, both
  directions) is only the starting point. Expect, per endpoint, to:
  - **omit fields from the response** (internal, derived, or not meant to be exposed);
  - **omit fields from the request** (server-owned / read-only, e.g. `id`, `version`, `status`);
  - **constrain formats** (`format:`, `pattern`, enum, min/max length, required vs optional).

  A schema is not final until it passes that review; request and response shapes are considered
  **separately** (they are rarely mirror images).

---

## 5. Conventions

### 5.1 Naming

- Property names: `snake_case`, lowercase (e.g. `display_name`, `metadata_source`, `trust_relationship`).
- **Path scheme (D13):** `/v1/trust/{context}/{resource}` — leading `/v1`, then context
  (`config` now, `activation` later), then the resource. Config-API collection:
  `/v1/trust/config/trust-relationships`.
- Path segments: `kebab-case`, plural collections (`trust-relationships`); verbs only under
  `/actions/{verb}` for non-CRUD lifecycle operations (`.../actions/cancel-activation`).
- Enum values in the wire contract: `UPPER_SNAKE_CASE`, matching the domain enums verbatim
  (`DRAFT`, `READY`, `ACTIVATING`, `ACTIVE`, `INACTIVE`; `INDIVIDUAL`, `AGGREGATE`; `NONE`, `FILE`,
  `URI`, `UPSTREAM`, `MANUAL`, `MDQ`).

### 5.2 Authentication

- `bearerAuth` HTTP bearer security scheme, applied globally.
- Token is an access token from an OOB process (not specified here).
- The activation (M2M) API will likely use a **different audience/scope** (workers are not users) —
  recorded now, detailed when that spec is designed.

### 5.3 Errors — RFC 7807

- Media type `application/problem+json`. Base fields: `type` (URI), `title`, `status`, `detail`,
  `instance`.
- `type` URIs are anchored at **`https://jans.io/shibboleth-idp/problems/{code}`** (D12), where
  `{code}` is the machine-readable error code below.
- Extension members:
  - `code` — stable, machine-readable string derived from the `DomainError` subclass
    (e.g. `operation_forbidden_from_status`).
  - `violations` — optional array of `{ field, code, message }` for field-level validation failures.
- `DomainObjectCreationFailed` / `DomainObjectUpdateFailed` are **wrappers**; the response surfaces the
  wrapped **cause's** `code`, not the wrapper's.

**Domain-error → HTTP mapping (guidance; refine per endpoint):**

| Domain error (family) | HTTP | Notes |
|---|---|---|
| `RequiredValueMissing`, `InvalidUriSyntax`, malformed body | `400` | Syntactic / presence validation |
| `IncompatibleMetadataSourceForNature`, `OperationRestrictedToNature` | `400` | Domain-rule validation failure (we use `400`, not `422` — see note) |
| `InvalidStatusForOperation`, `OperationForbiddenFromStatus`, `TrustTransitionError` | `409` | Operation not allowed from current state |
| `InvalidVersion` | `409` | Concurrency / version rule |
| TR not found (read/mutate by id) | `404` | — |
| Auth missing/invalid | `401` / `403` | Transport concern |
| Unexpected | `500` | Never leak internals in `detail` |

> **Decision:** all client-input and domain-rule validation failures use **`400`** (we do not use
> `422`). `409` is reserved for state/transition conflicts and `404` for missing resources. The
> `problem+json` `code` remains the precise, machine-readable discriminator regardless of status.

### 5.4 Versioning & concurrency (deferred — D4)

- The domain `Version` (monotonic integer, bumped only when the aggregate is *effectively modified*)
  appears **read-only** as `version` in every TR response.
- Concurrency is **not enforced** yet. When it is, the intended design is HTTP `ETag` (derived from
  `version`) + `If-Match` on mutations, `412 Precondition Failed` on mismatch — additive, no body change.
- API/spec **versioning** is separate: `/v1` is the **leading** path segment (D13), ahead of the
  `trust/config` context.

### 5.5 Collections (list endpoints)

- **Paginated, 1-based.** Query params `page` (1-based, min 1, default 1) and `size` (min 1, max 100,
  default 20).
- **Response envelope** groups items and pagination metadata (all `snake_case`):

  ```json
  {
    "items": [ /* summary items */ ],
    "page": {
      "size": 20,
      "number": 1,
      "total_elements": 145,
      "total_pages": 8,
      "number_of_elements": 20
    }
  }
  ```

  `number` is the 1-based current page; `total_elements` is the count across all pages matching the
  filters; `total_pages = ceil(total_elements / size)` (0 when empty); `number_of_elements` is the
  count in the returned page.
- **Filters** for `GET /trust-relationships`, all optional and AND-combined: `nature` and `status`
  (enum-constrained, exact match); `display_name` and `description` (partial, case-insensitive match).
- **Items** are the summary representation (`TrustRelationshipSummary`), not the full detail.
- **Filtering and paging are persistence concerns** (D14) — the DTO layer only shapes the envelope.

### 5.6 Metadata sources & files (D8)

- `metadata_source` is polymorphic on `type` (`oneOf` + discriminator): `NONE`, `FILE`, `URI`,
  `UPSTREAM`, `MANUAL`, `MDQ`. Nature restricts which are valid (rules doc §1) — enforced by the domain.
- **`FILE`** carries an **OOB-produced upload token** (`token`), never file bytes or multipart. The file
  is uploaded out-of-band; the token is an opaque reference the server later resolves/verifies (that
  resolution is a separate phase). The domain treats it as an opaque file reference — "token" is a
  transport detail, deliberately kept out of the domain model.
- **`MANUAL`** needs no file: its signing certificate is an **inline base64 string**
  (`signing_certificate`, optional → no certificate), and `valid_until` is an ISO-8601 date-time.

### 5.7 HTTP status conventions

- `201 Created` + `Location` for create; body is the created TR (note: `id` is **unassigned** until
  persistence — see §7 create notes).
- `200 OK` for reads and successful mutations (returning the updated TR).
- `204 No Content` only where there is genuinely nothing to return.

---

## 6. The Iterative Workflow

Repeat until every box in §7 is ticked:

1. **Select** the first endpoint in §7 with an unchecked `Done` box.
2. **Draft** the OpenAPI operation (path, verb, params, request/response schemas, error responses)
   in the relevant YAML (§9), reusing shared components and honouring the legibility rules (§4).
3. **Schema-review checkpoint** — present the **request** and **response** schemas *separately* for
   review, and iterate: drop fields that should not appear, tighten formats/constraints, adjust
   required/optional. **Do not proceed to mappers until this passes.** (This is where "some fields
   should not make it into the response / some need a certain format" is resolved.)
4. **Plan tests** — add the endpoint's DTO/mapper cases to
   [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md).
5. **Red** — write the failing mapper/DTO tests.
6. **Green** — implement DTOs and mappers until tests pass; run the `trust-dto` module tests.
7. **Verify** the spec is valid and consistent with the domain and the rules doc.
8. **Tick** the `Done` box in §7 and note anything learned.

Work one endpoint per iteration; do not batch.

---

## 7. Config API — Endpoint Catalog

Effective base: **`/v1/trust/config`** (D13); collection: `/trust-relationships`. The paths below are
written **relative to that base** (i.e. full URL = `/v1/trust/config` + the path shown) — e.g. create is
`POST /v1/trust/config/trust-relationships`. In the YAML, `/v1/trust/config` is the `servers` base path
and `paths` entries start at `/trust-relationships`, keeping the file legible (§4). All operations
require `bearerAuth`. Tick `Done` only when spec + DTOs + mappers + passing tests are all complete (§4).

**Reads & collection**

- [x] **Create** — `POST /trust-relationships` — domain `TrustRelationship.create(displayName, description, nature)`. Body: `{ display_name, description, nature }`. `201` + `Location`; response is `TrustRelationshipSummary`. *The response carries an assigned `id`: persistence assigns it before the TR is mapped to a DTO (D10).* — spec `trust-config-api.yaml`; DTOs `CreateTrustRelationshipRequest`/`TrustRelationshipSummary`; mapper `TrustRelationshipMapper`; tests `Create` in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md) (15 passing).
- [x] **Get by id** — `GET /trust-relationships/{id}` — `TrustRelationshipDetail` (own fields + compact views of metadata source & profiles; full released attributes, activation diagnostics, discovered entity IDs). `200` / `404`. — mapper `TrustRelationshipMapper.toDetail`; tests `Get by id` in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). *Full metadata-source & per-profile detail deferred to their sub-resource endpoints. Domain change: added `EntityIds.getEntityIds()` read accessor.*
- [x] **List** — `GET /trust-relationships?nature=&status=&display_name=&description=&page=&size=` — `TrustRelationshipPage` (`{ items: [summary], page: {…} }`), 1-based; filters optional & AND-combined (§5.5). — mapper `TrustRelationshipMapper.toPage`; DTOs `TrustRelationshipPage`/`PageMetadata`; tests `List` in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). *Filtering/paging are persistence concerns; the DTO layer only shapes the envelope.*
- [ ] **Delete** — `DELETE /trust-relationships/{id}` — persistence-layer hard delete (D11); no domain operation. `204` / `404`. *No deletion guard for now (§10).*

**Descriptive updates** (allowed from all states)

- [x] **Update basic info** — `PUT /trust-relationships/{id}/basic-info` — domain `updateBasicInfo(displayName, description)` (single atomic update; version bumps at most once). Body `{ display_name (required, non-blank), description (optional; absent/null → empty) }`. Full-block replace (omitting `description` clears it). Allowed from all states. Response `TrustRelationshipSummary`. Errors `400`/`401`/`404`. — merges the former display-name & description updates (D15). Domain op `TrustRelationship.updateBasicInfo`; DTO `UpdateBasicInfoRequest`; mapper `TrustRelationshipMapper.updateBasicInfo`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md) + domain 2.3 cases.

**Structural updates** (forbidden from `ACTIVATING`)

- [x] **Set metadata source** — `PUT /trust-relationships/{id}/metadata-source` — `updateMetadataSource`. Polymorphic body (§5.6), `oneOf` + `type` discriminator, **all six types** (`NONE`, `FILE`, `URI`, `UPSTREAM`, `MDQ`, `MANUAL`). Nature restrictions apply (→ `400`); forbidden from `ACTIVATING` (→ `409`). Response `TrustRelationshipSummary`. DTOs `MetadataSourceRequest` (+ per-type subtypes) and `AssertionConsumerServiceRequest`; mapper `TrustRelationshipMapper.updateMetadataSource`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). Domain adds: `InvalidUuidSyntax`, `InvalidTimestampSyntax` (adapter parse errors, mirror `InvalidUriSyntax`); `ValidityPeriod.until(Instant)`+getter; `AssertionConsumerService` getters. `FILE` carries an OOB upload token (resolution deferred); `MANUAL` certificate is an inline base64 string.
- [x] **Set Shibboleth SSO profile** — `PATCH /trust-relationships/{id}/profiles/shibboleth-sso` — `updateShibbolethSsoProfileConfiguration`. Partial-override (D16); adds the Authentication fields (`post_authentication_flows`, `authentication_result_reuse_policy`, `max_authentication_age` duration) and `attribute_statement_policy`, `nameid_format_precedence` (string array). Two ISO-8601 durations (`max_authentication_age`, `assertion_lifetime`). Response `TrustRelationshipSummary`; errors `400`/`401`/`404`/`409`. DTO `ShibbolethSsoProfileConfigurationRequest`; mapper `TrustRelationshipMapper.updateShibbolethSsoProfileConfiguration`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md).
- [ ] **Set SAML2 SSO profile** — `PATCH /trust-relationships/{id}/profiles/saml2-sso` — `updateSaml2SsoProfileConfiguration`.
- [x] **Set SAML2 Attribute Query profile** — `PATCH /trust-relationships/{id}/profiles/saml2-attribute-query` — `updateSaml2AttributeQueryProfileConfiguration`. Partial-override (D16); adds the SamlAssertion fields (`assertion_time_condition`, `assertion_lifetime` as ISO-8601 duration, `assertion_signing_policy`) and `friendly_name_randomization_policy`/encryption policies. Response `TrustRelationshipSummary`; errors `400`/`401`/`404`/`409`. DTO `Saml2AttributeQueryProfileConfigurationRequest`; mapper `TrustRelationshipMapper.updateSaml2AttributeQueryProfileConfiguration`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). Domain add: `InvalidDurationSyntax` (adapter parse error).
- [x] **Set SAML2 Artifact Resolution profile** — `PATCH /trust-relationships/{id}/profiles/saml2-artifact-resolution` — `updateSaml2ArtifactResolutionProfileConfiguration`. Partial-override (D16); Logout's fields plus `assertion_signing_policy`, `assertion_encryption_policy`, `attribute_encryption_policy`. Response `TrustRelationshipSummary`; errors `400`/`401`/`404`/`409`. DTO `Saml2ArtifactResolutionProfileConfigurationRequest`; mapper `TrustRelationshipMapper.updateSaml2ArtifactResolutionProfileConfiguration`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md).
- [ ] **Set SAML2 ECP profile** — `PATCH /trust-relationships/{id}/profiles/saml2-ecp` — `updateSaml2EcpProfileConfiguration`.
- [x] **Set SAML2 Logout profile** — `PATCH /trust-relationships/{id}/profiles/saml2-logout` — `updateSaml2LogoutProfileConfiguration`. Partial-override (D16): all fields optional, only provided ones change. Fields: `status`, `inbound_flows`, `outbound_flows`, `message_signing_policy`, `request_signature_validation_policy`, `encryption_fallback_policy`, `nameid_encryption_policy`. Response `TrustRelationshipSummary`; errors `400`/`401`/`404`/`409`. DTO `Saml2LogoutProfileConfigurationRequest`; mapper `TrustRelationshipMapper.updateSaml2LogoutProfileConfiguration`; tests in [`trust_dto_mapper_tests.md`](./trust_dto_mapper_tests.md). *(First profile — leanest; the other five follow the same pattern, D16.)*
- [ ] **Set released attributes** — `PUT /trust-relationships/{id}/released-attributes` — `updateReleasedAttributes` (forbidden from `ACTIVATING`).

**Lifecycle actions** (non-idempotent → `POST`; enforce transition rules → `409`)

- [ ] **Activate** — `POST /trust-relationships/{id}/actions/activate` — `activate()`. Allowed from `READY`, `INACTIVE`.
- [ ] **Cancel activation** — `POST /trust-relationships/{id}/actions/cancel-activation` — `cancelActivation()`. Allowed from `ACTIVATING`.
- [ ] **Deactivate** — `POST /trust-relationships/{id}/actions/deactivate` — `deactivate()`. Allowed from `ACTIVE`.

**Sub-resource reads** (exposed — detailed in a later iteration)

Sub-resources are read individually, not only via the aggregate `GET` — profile configurations
especially. Precise shapes/paths are settled when we reach them; tracked here so they are not lost:

- [ ] **Read metadata source** — `GET /trust-relationships/{id}/metadata-source`.
- [ ] **Read a profile config** — `GET /trust-relationships/{id}/profiles/{profile}` (each of the six profiles).
- [ ] **Read released attributes** — `GET /trust-relationships/{id}/released-attributes`.

> **Deliberately excluded from the config API** (they are driven by the async validation process, not a
> user — they belong to the activation/M2M API, §8):
> `finalizeActivation(ActivationDiagnostics)` and `incorporateDiscoveredEntityIds(...)`.

---

## 8. Activation (M2M) API — deferred (D2)

Machine-to-machine protocol for the activation domain (`WorkItem`, `Worker`, `WorkOrchestrator`,
`Lease`) — claim, report, lease renewal, reclaim, and TR callbacks (`finalizeActivation`,
`incorporateDiscoveredEntityIds`). Designed in a later phase, from
[`asynchronous_activation.md`](./asynchronous_activation.md).

Effective base: **`/v1/trust/activation`** (D13), symmetric with the config API's `/v1/trust/config`.
For now, create `trust-activation-api.yaml` containing only the **shared foundation** (security scheme,
`problem+json` components, common params) and a placeholder note, so the two-API split is visible from
day one. **Its concurrency model is the domain's own fence tokens** (`WorkItemId`, `Lease`,
`StaleReport`/`NotLeaseHolder`), *not* HTTP ETag.

---

## 9. DTO & Mapper Conventions

Location (per [`directory_structure_decisions.md`](./directory_structure_decisions.md)):

```
trust-dto/
├── pom.xml                                  (artifact: jans-shibboleth-trust-dto; depends on trust-domain)
└── src/main/
    ├── java/io/jans/shibboleth/trust/dto/
    │   ├── config/        TrustRelationshipDto, ...ListItemDto, MetadataSourceDto (+ subtypes), *ProfileConfigurationDto, ReleasedAttributesDto
    │   ├── activation/    (later)
    │   ├── shared/        ProblemDto (RFC 7807), Violation
    │   └── mapper/
    │       ├── config/    TrustRelationshipMapper, ...
    │       └── activation/(later)
    └── resources/openapi/
        ├── trust-config-api.yaml            (built first)
        ├── trust-activation-api.yaml        (stub, §8)
        └── components/                      (shared: errors, security, common params)
```

Conventions:

- **DTOs are dumb data holders** — no domain logic. JSON property names are `snake_case` (D6).
- **Mappers own translation** both ways: `toDto(domain)` and `toDomain(dto) → Result<...>` (domain
  construction returns `Result`; mappers surface it, they don't throw for domain-rule failures).
- **Value objects flatten sensibly**: `Id`, `DisplayName`, `Description`, `Version` map to primitives;
  `MetadataSource`/profiles map to their polymorphic/structured DTOs.
- **Identity is always present** (D10): every TR mapped to a DTO carries an assigned `id`; the mapper
  does not accommodate an unassigned id past the domain boundary.
- **Read vs write asymmetry**: response DTOs expose read-only fields (`version`, `status`); `id` is
  present and read-only on responses; request DTOs omit anything the client must not set.
- **`activation_diagnostics` maps both ways (writable) for now**: the mapper reads and writes it
  (full round-trip). This may become read-only in the config contract once there is more context — it
  does not change the mapper's fidelity, only whether a request DTO accepts it.
- **Round-trip fidelity is a test invariant**: `toDomain(toDto(x))` reconstructs an equal aggregate
  (see the test plan) — modulo intentionally read-only/derived fields.

---

## 10. Open Questions (decide when first encountered)

- **Deletion guard:** deletion is in scope as a persistence hard delete (D11). Do we want a
  precondition — e.g. refuse to delete while `ACTIVATING` (in-flight async work) or `ACTIVE`? If so,
  model it as a **read-only domain predicate** (like `hasNoActiveProfileConfiguration()`), *not* a
  state transition. *Default: no guard.*

**Resolved** (moved into §3 / §5): two-API split (D1/D2), endpoint style (D3), concurrency deferred
(D4), RFC 7807 (D5), assigned identity at the DTO boundary (D10), deletion in scope (D11), error `type`
namespace (D12, `https://jans.io/shibboleth-idp/problems/{code}`), sub-resource reads exposed (§7),
`activation_diagnostics` writable for now (§9), `400` for all input/domain-rule validation (§5.3).
