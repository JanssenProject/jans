# Trust Config API — Implementation Guide

> **Issue draft.** Restates the contract for the implementer; it will drift. Source of truth:
> `openapi/trust-config/trust-config-api.yaml` and the Java public interfaces named below.
> Read [`README.md`](./README.md) first for the shared model, lifecycle, and `Result`/`problem+json`
> conventions.

## What this API is

The human-facing, **desired-state** API. Base path `/v1/trust/config`, user-audience bearer token. It
is the single writable projection of the `TrustRelationship` aggregate: create a relationship, set its
metadata source, configure its SAML profiles, choose released attributes, and drive its lifecycle
(activate / cancel / deactivate). It knows nothing about workers, queues, or leases — activation is
requested here and reconciled elsewhere (see [`trust-activation-api.md`](./trust-activation-api.md)).

Endpoints are **task-based, not generic CRUD**: each operation has its own small body and its own
transition rules.

## Endpoint catalog

| Method | Path | Operation |
|---|---|---|
| `GET` | `/trust-relationships` | List (paginated, filterable). |
| `POST` | `/trust-relationships` | Create (→ `DRAFT`). |
| `GET` | `/trust-relationships/{id}` | Full detail. |
| `PUT` | `/trust-relationships/{id}` | Update basic info (display name + description). |
| `DELETE` | `/trust-relationships/{id}` | Delete. |
| `GET`/`PUT` | `/trust-relationships/{id}/metadata-source` | Read / set the metadata source. |
| `GET` | `/trust-relationships/{id}/profiles` | Read profile configs (optionally a subset). |
| `PATCH` | `/trust-relationships/{id}/profiles/{profile}` | Partially update one profile. |
| `GET`/`PUT` | `/trust-relationships/{id}/released-attributes` | Read / replace released attributes. |
| `POST` | `/trust-relationships/{id}/actions/activate` | Start activation. |
| `POST` | `/trust-relationships/{id}/actions/cancel-activation` | Abort a pending activation. |
| `POST` | `/trust-relationships/{id}/actions/deactivate` | Deactivate an active relationship. |

The six `{profile}` values are: `shibboleth-sso`, `saml2-sso`, `saml2-logout`, `saml2-ecp`,
`saml2-attribute-query`, `saml2-artifact-resolution`. Lifecycle actions take **no body**.

## Public interfaces you call

Unlike activation, **there is no application-service class** for config — you drive the aggregate
directly through a repository and the static mappers.

- **Repository** (an interface implemented in `trust-adapters`, wire the impl at boot):
  `io.jans.shibboleth.trust.persistence.config.TrustRelationshipRepository`
  ```java
  Result<TrustRelationship>            save(TrustRelationship tr);
  Result<TrustRelationship>            findById(Id id);           // failure: TrustRelationshipNotFound → 404
  Result<TrustRelationshipSummaryPage> list(TrustRelationshipQuery query);  // 1-based paging + filters
  Result<Void>                         delete(Id id);
  ```
  `TrustRelationshipQuery` and `TrustRelationshipSummaryPage` live beside it in `persistence.config`
  (they are *persistence* projections — not the JSON DTOs).

- **Mappers** — `io.jans.shibboleth.trust.dto.mapper.config.TrustRelationshipMapper` (all methods
  `public static`, all DTO→domain ones return `Result`):
  ```java
  toDomain(CreateTrustRelationshipRequest)                          → Result<TrustRelationship>
  updateBasicInfo(TrustRelationship, UpdateBasicInfoRequest)        → Result<TrustRelationship>
  updateMetadataSource(TrustRelationship, MetadataSourceRequest)    → Result<TrustRelationship>
  update<Profile>ProfileConfiguration(TrustRelationship, …Request)  → Result<TrustRelationship>  // one per profile
  updateReleasedAttributes(TrustRelationship, …Request)             → Result<TrustRelationship>
  toSummary(TrustRelationship)   → TrustRelationshipSummary   // response for most mutations
  toDetail(TrustRelationship)    → TrustRelationshipDetail    // GET by id
  toPage(TrustRelationshipSummaryPage) → TrustRelationshipPage
  toMetadataSourceView / toProfilesView(tr, Set<ProfileType>) / toReleasedAttributesView(tr)
  ```
  The JSON DTOs live in `io.jans.shibboleth.trust.dto.config`. **Watch the name collision:** the DTO
  `TrustRelationshipSummary` (in `dto.config`) is a *different* type from the persistence
  `TrustRelationshipSummaryPage` / `TrustRelationshipQuery` (in `persistence.config`).

- **Aggregate** — `io.jans.shibboleth.trust.config.TrustRelationship`. You rarely call it directly (the
  mappers do), but the lifecycle actions map to its methods: `activate()`, `cancelActivation()`,
  `deactivate()` — each returns `Result<TrustRelationship>`. `Id.of(UUID)` builds the key from a path
  UUID.

## The canonical mutation shape

Every mutation except create/delete follows: **load → map-update → save → map-to-DTO**.

```java
// PUT /trust-relationships/{id}   (basic info)
Result<TrustRelationship> loaded = repository.findById(Id.of(idFromPath));
if (loaded.isFailure()) return toHttp(loaded);                         // 404
Result<TrustRelationship> updated =
        TrustRelationshipMapper.updateBasicInfo(loaded.getValue(), body);
if (updated.isFailure()) return toHttp(updated);                       // 400 / 409
Result<TrustRelationship> saved = repository.save(updated.getValue());
return toHttp(saved, TrustRelationshipMapper::toSummary);             // 200 + summary
```

- **Create** → `toDomain(req)` → `save` → `201` + `Location: /v1/trust/config/trust-relationships/{id}`
  + `toSummary`.
- **Delete** → `repository.delete(Id.of(id))` → `204` (hard delete, no domain op, no body).
- **Lifecycle actions** → load → call the aggregate transition (`activate()` etc.) → save →
  `toSummary`; `409` on a disallowed transition.
- **List** → build a `TrustRelationshipQuery` from the query params (`nature`, `status`,
  `display_name`, `description` partial/case-insensitive; `page` 1-based; `size`) → `repository.list`
  → `toPage`.

## Metadata source — one polymorphic endpoint

`PUT …/metadata-source` takes a single body discriminated by `type`. Six types; **which are valid
depends on the relationship's nature**, enforced by the domain (an incompatible combination →
`400`):

| `type` | INDIVIDUAL | AGGREGATE | Body (besides `type`) |
|---|:---:|:---:|---|
| `NONE` | ✓ | ✓ | — (clears the source) |
| `FILE` | ✓ | ✓ | `token` (from File Staging — see below) |
| `URI` | ✓ | ✓ | `uri` |
| `UPSTREAM` | ✓ | — | `parent_id`, `entity_id` |
| `MANUAL` | ✓ | — | `entity_id`, `valid_until`, `assertion_consumer_service`, optional `signing_certificate` (base64) |
| `MDQ` | — | ✓ | `base_url` |

**`FILE` never carries bytes.** The `token` is produced out-of-band by the File Staging API. When you
process a `FILE` write you **claim** that token: call the staging service with the token plus the
destination path in the shared document store; staging moves the file there and returns a durable
handle, which is persisted in place of the token. A missing/expired token fails the write
synchronously (`400`/`409`). Only cheap structural intake happens here — semantic metadata validation
is the worker's job. See [`file-staging-api.md`](./file-staging-api.md).

Reads (`GET …/metadata-source`, and the `metadata_source` block in detail) show the resolved source;
`FILE` shows the stored `file_path`, never a token.

## Profiles & released attributes

- **`PATCH …/profiles/{profile}`** is partial-override: only fields present in the body change; omitted
  fields keep their current values. Each profile has a different field set (see the schemas); toggling
  a profile on/off is the `status` field (`ACTIVE`/`INACTIVE`).
- **`GET …/profiles?profiles=SAML2_SSO,SAML2_LOGOUT`** returns the requested configs keyed by profile
  (all six when the filter is omitted) — one call for a configuration screen.
- **`PUT …/released-attributes`** is a **full replacement** of the attribute set; an empty array clears
  them.
- All of these are structural edits → **rejected with `409` while `ACTIVATING`**.

## Error → HTTP mapping

| Domain error (family) | HTTP |
|---|---|
| `RequiredValueMissing`, `InvalidUriSyntax`, `InvalidUuidSyntax`, `InvalidTimestampSyntax`, `InvalidDurationSyntax`, malformed body | `400` |
| `IncompatibleMetadataSourceForNature`, `OperationRestrictedToNature` (domain-rule validation) | `400` |
| `InvalidStatusForOperation`, `OperationForbiddenFromStatus`, `TrustTransitionError`, `InvalidVersion` | `409` |
| `TrustRelationshipNotFound`, `IdNotAssigned` | `404` |
| auth missing/invalid | `401` / `403` |
| unexpected | `500` |

Rule of thumb: **all input and domain-rule validation → `400`; `409` only for state/transition
conflicts; `404` for missing.** The `problem+json` `code` is the precise discriminator regardless of
status. `DomainObjectCreationFailed`/`UpdateFailed` unwrap to their cause's `code`.

## Worked example: stand up a URI-based individual SP and activate it

```http
POST /v1/trust/config/trust-relationships
Content-Type: application/json

{ "display_name": "University Portal SP", "description": "Student portal", "nature": "INDIVIDUAL" }
```
```http
201 Created
Location: /v1/trust/config/trust-relationships/7f3a9c2e-4b1d-4c8a-9e2f-1a2b3c4d5e6f

{ "id": "7f3a…5e6f", "display_name": "University Portal SP", "description": "Student portal",
  "nature": "INDIVIDUAL", "status": "DRAFT", "version": 1 }
```
```http
PUT /v1/trust/config/trust-relationships/7f3a…5e6f/metadata-source
{ "type": "URI", "uri": "https://sp.example.org/metadata.xml" }
```
```http
PATCH /v1/trust/config/trust-relationships/7f3a…5e6f/profiles/saml2-sso
{ "status": "ACTIVE", "assertion_lifetime": "PT5M" }
```
At this point the relationship has a real source **and** an active profile, so the domain has moved it
to `READY`. Now activate:
```http
POST /v1/trust/config/trust-relationships/7f3a…5e6f/actions/activate
```
```http
200 OK
{ "id": "7f3a…5e6f", …, "status": "ACTIVATING", "version": 4 }
```
It stays `ACTIVATING` until a worker reports a result via the activation API, which flips it to
`ACTIVE` (or back to `READY` on failure).

## Provisioning

The installer must create, under `o=jans`:

| Aggregate | Object class / table | Branch |
|---|---|---|
| Trust relationship | `jansTrustRelationship` | `ou=trustRelationships,o=jans` |
