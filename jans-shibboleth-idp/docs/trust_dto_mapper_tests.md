# Trust DTO & Mapper — Test Plan

> Companion to [`openapi_design_spec.md`](./openapi_design_spec.md). A human-facing overview of the
> coverage realised by the tests in the `trust-dto` module — the tests themselves remain the source
> of truth. One section per endpoint, added as each endpoint is taken through the workflow.

Conventions:
- **Mapper-in** = `toDomain(requestDto)` → `Result<domain>` (surfaces the domain `Result`; never throws
  on a domain-rule failure).
- **Mapper-out** = `toDto(domain)` → DTO (throws only on a programming-error/contract violation, e.g.
  an aggregate reaching the mapper without a persisted id).
- **JSON** = wire-contract binding: `snake_case` keys, enum values verbatim.

---

## Create — `POST /v1/trust/config/trust-relationships`

Domain: `TrustRelationship.create(DisplayName, Description, TrustNature)` → `DRAFT`, `version = 1`,
all other fields defaulted. DTOs: `CreateTrustRelationshipRequest` (request),
`TrustRelationshipSummary` (response). Mapper: `TrustRelationshipMapper`.

### Mapper-in — `toDomain(CreateTrustRelationshipRequest)`

| Given | Then |
|-------|------|
| `display_name="University Portal SP"`, `description="…"`, `nature=INDIVIDUAL` | success; TR is `DRAFT`, `version=1`, nature `INDIVIDUAL`, display name & description set, metadata source `NONE`, id unassigned |
| same but `nature=AGGREGATE` | success; nature `AGGREGATE` |
| `display_name=null` | failure; error is `RequiredValueMissing` (not thrown) |
| `display_name="   "` (whitespace only) | failure; domain trims → blank → `RequiredValueMissing` |
| `display_name="  Portal SP  "` | success; domain trims to `"Portal SP"` |
| `description=null` | success; description normalises to `""` |
| `description="  hi  "` | success; description trims to `"hi"` |
| `nature=null` | failure; creation fails on the nature presence invariant (not thrown) |

### Mapper-out — `toSummary(TrustRelationship)`

| Given | Then |
|-------|------|
| `DRAFT` TR with an assigned id | summary carries that UUID, display name, description, `nature`, `status=DRAFT`, `version=1` |
| TR with an **unassigned** id | throws `IllegalStateException` |
| TR whose `version` was bumped (e.g. to 2) | `summary.version == 2` |

### JSON — wire contract

| Given | Then |
|-------|------|
| serialise `TrustRelationshipSummary` | keys are exactly `id, display_name, description, nature, status, version`; `nature`/`status` are UPPER_SNAKE enum strings; `id` is the UUID string |
| deserialise a `snake_case` create body | fields populate; `nature` binds to the enum |
| deserialise a create body with an **unknown** field | rejected (unknown properties are not silently ignored) |

### Integration — create then summarise

| Given | Then |
|-------|------|
| `toDomain(valid request)` → assign id → `toSummary(...)` | display name, description and nature round-trip unchanged; `status=DRAFT`, `version=1` |

---

## Get by id — `GET /v1/trust/config/trust-relationships/{id}`

Response DTO: `TrustRelationshipDetail` (own fields + compact views of structured parts + full released
attributes, activation diagnostics and discovered entity IDs). Mapper: `TrustRelationshipMapper.toDetail`.

### Mapper-out — `toDetail(TrustRelationship)`

| Given | Then |
|-------|------|
| a default `DRAFT` with an assigned id | core fields set; `metadata_source.type=NONE`; six profile summaries, all `INACTIVE`, one per profile kind; released attributes & discovered entity IDs empty; `activation_diagnostics` is `NO_DATA`, empty origin, epoch timestamps, no log entries |
| a TR with discovered entity IDs | `discovered_entity_ids` lists the entity-ID URIs |
| a TR with released attributes | `released_attributes` lists each `{id, display_name}` |
| a TR whose SAML2 SSO profile is `ACTIVE` | the SAML2 SSO profile summary reports `ACTIVE` |
| a TR with populated activation diagnostics (`SUCCEEDED`, origin, timestamps, one log entry) | status/origin mapped; `started_at`/`completed_at` and the log entry's timestamp are ISO-8601 strings; level & message mapped |
| a TR with an **unassigned** id | throws `IllegalStateException` |

### JSON — wire contract

| Given | Then |
|-------|------|
| serialise `TrustRelationshipDetail` | top-level keys are `snake_case`; nested `metadata_source`, `profiles[]`, `released_attributes[]`, `activation_diagnostics` (incl. `started_at`, `completed_at`, `log_entries[]`) and `discovered_entity_ids[]` all use `snake_case`; enums verbatim; timestamps are ISO-8601 strings (no Java-time Jackson module required) |

---

## List — `GET /v1/trust/config/trust-relationships`

Response DTO: `TrustRelationshipPage` (`{ items: [TrustRelationshipSummary], page: PageMetadata }`).
Mapper: `TrustRelationshipMapper.toPage(items, number, size, totalElements)`. Filtering and paging are
persistence concerns and are **not** exercised here — the mapper only shapes the envelope.

### Mapper — `toPage(...)`

| Given | Then |
|-------|------|
| an empty slice, `number=1`, `size=20`, `total_elements=0` | `items` empty; page `size=20`, `number=1`, `total_elements=0`, `total_pages=0`, `number_of_elements=0` |
| two assigned-id items, `number=1`, `size=20`, `total_elements=145` | `items` holds the two summaries in order; page `number=1`, `size=20`, `total_elements=145`, `total_pages=8`, `number_of_elements=2` |
| various `(total_elements, size)` | `total_pages = ceil(total/size)`: (40,20)=2, (41,20)=3, (20,20)=1, (1,20)=1, (0,20)=0 |
| a slice containing an item with an **unassigned** id | throws `IllegalStateException` (from `toSummary`) |

### JSON — wire contract

| Given | Then |
|-------|------|
| serialise `TrustRelationshipPage` | top-level keys are exactly `items`, `page`; the `page` object keys are `size`, `number`, `total_elements`, `total_pages`, `number_of_elements`; each item is a `snake_case` summary |

---

## Update basic info — `PUT /v1/trust/config/trust-relationships/{id}/basic-info`

Request DTO: `UpdateBasicInfoRequest` (`display_name` required, `description` optional). Mapper:
`TrustRelationshipMapper.updateBasicInfo(existing, request)` → `Result<TrustRelationship>` (applies the
domain's `updateBasicInfo`, which bumps the version at most once). Response: `TrustRelationshipSummary`.

### Mapper — `updateBasicInfo(existing, request)`

| Given | Then |
|-------|------|
| a valid request (new display name + description) on an existing TR | success; both fields updated; version bumped exactly once (`existing.version + 1`) |
| a request with a null description | success; description normalised to `""` |
| a request with a blank display name | failure; error is `RequiredValueMissing` (not thrown) |
| a request with a null display name | failure; error is `RequiredValueMissing` |
| a request with the same display name and description | success; result equals the original; version unchanged |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise a `snake_case` body | `display_name`/`description` populate |
| deserialise a body omitting `description` | `display_name` populates, `description` is null (the mapper later normalises it to empty) |
| deserialise a body with an **unknown** field | rejected |

---

## Set metadata source — `PUT /v1/trust/config/trust-relationships/{id}/metadata-source`

Request DTO: `MetadataSourceRequest` — polymorphic on `type` (`NONE`, `FILE`, `URI`, `UPSTREAM`, `MDQ`,
`MANUAL`). `FILE` carries an OOB upload `token` (opaque reference; resolution deferred); `MANUAL` has an
inline base64 `signing_certificate` (optional) and an ISO-8601 `valid_until`. Mapper:
`TrustRelationshipMapper.updateMetadataSource(existing, request)` → `Result<TrustRelationship>` (builds
the domain source, then delegates; nature and state restrictions are enforced by the domain). Response:
`TrustRelationshipSummary`.

### Mapper — `updateMetadataSource(existing, request)`

| Given | Then |
|-------|------|
| `NONE` on any TR | success; metadata source type is `NONE` |
| `FILE` with a token | success; source is `FILE`, the token stored as the file reference |
| `FILE` with a missing token | failure (`RequiredValueMissing`) |
| `URI` with a valid URL | success; source is `URI` with that URL |
| `UPSTREAM` on an INDIVIDUAL TR | success; source is `UPSTREAM` with the parent id and entity id |
| `MDQ` on an AGGREGATE TR | success; source is `MDQ` with that base URL |
| `MANUAL` on an INDIVIDUAL TR (full) | success; entity id, `valid_until` instant, ACS fields and base64 certificate all mapped |
| `MANUAL` with no `signing_certificate` | success; certificate is absent (`NoCertificateInfo`) |
| `MANUAL` with ACS `index`/`is_default` omitted | success; index defaults to 1, is_default to true |
| `MDQ` on an INDIVIDUAL TR | failure (`DomainObjectUpdateFailed` — nature restriction) |
| `UPSTREAM` on an AGGREGATE TR | failure (`DomainObjectUpdateFailed` — nature restriction) |
| `MANUAL` on an AGGREGATE TR | failure (`DomainObjectUpdateFailed` — nature restriction) |
| `URI` with a malformed URL | failure (`InvalidUriSyntax`) |
| `URI` with a missing URL | failure (`RequiredValueMissing`) |
| `UPSTREAM` with a malformed `parent_id` | failure (`InvalidUuidSyntax`) |
| `MANUAL` with a malformed `valid_until` | failure (`InvalidTimestampSyntax`) |
| `MANUAL` with a missing `entity_id` | failure (`RequiredValueMissing`) |

### JSON — wire contract (polymorphic)

| Given | Then |
|-------|------|
| deserialise `{type: NONE\|FILE\|URI\|UPSTREAM\|MDQ\|MANUAL, …}` | binds to the matching subtype with its fields |
| deserialise an unknown `type` | rejected (`InvalidTypeIdException`) |
| deserialise a body with an **unknown** field | rejected |

### Domain value objects (supporting)

| Given | Then |
|-------|------|
| `ValidityPeriod.until(instant)` | success; `getValidUntil()` returns that instant; null → `RequiredValueMissing` |
| `AssertionConsumerService.of(...)` | getters expose location/binding/index/is_default; defaults 1/true via the two-arg overload |
