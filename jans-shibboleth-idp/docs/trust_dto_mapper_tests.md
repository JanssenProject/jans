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

---

## Update SAML2 Logout profile — `PATCH /v1/trust/config/trust-relationships/{id}/profiles/saml2-logout`

Request DTO: `Saml2LogoutProfileConfigurationRequest` — all fields optional (`status`, `inbound_flows`,
`outbound_flows`, `message_signing_policy`, `request_signature_validation_policy`,
`encryption_fallback_policy`, `nameid_encryption_policy`). Mapper:
`TrustRelationshipMapper.updateSaml2LogoutProfileConfiguration(existing, request)` — seeds the builder
`from` the current profile and overrides only present fields, then delegates. Response:
`TrustRelationshipSummary`. (First profile; the other five follow the same partial-override pattern.)

### Mapper — `updateSaml2LogoutProfileConfiguration(existing, request)`

| Given | Then |
|-------|------|
| only `status` provided | success; profile status changes, every other field keeps its current value |
| `message_signing_policy` provided | success; that policy is applied |
| `inbound_flows` provided | success; the flows are set (in order) |
| an empty request | success; the profile is unchanged and the version is not bumped |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise a `snake_case` body | enums bind (`status`, `message_signing_policy`, …); flow arrays bind |
| deserialise a body omitting fields | the omitted fields are null (mapper treats null as "keep current") |
| deserialise a body with an **unknown** field | rejected |

---

## Update SAML2 Artifact Resolution profile — `PATCH /v1/trust/config/trust-relationships/{id}/profiles/saml2-artifact-resolution`

Request DTO: `Saml2ArtifactResolutionProfileConfigurationRequest` — all optional; Logout's fields plus
`assertion_signing_policy`, `assertion_encryption_policy`, `attribute_encryption_policy`. Mapper:
`TrustRelationshipMapper.updateSaml2ArtifactResolutionProfileConfiguration(existing, request)` — same
seed-from-current, override-present pattern. Response: `TrustRelationshipSummary`.

### Mapper

| Given | Then |
|-------|------|
| only `assertion_signing_policy` provided | success; that field changes, others (status, attribute encryption, …) unchanged |
| `status` + `attribute_encryption_policy` provided | success; both applied |
| an empty request | success; profile unchanged, version not bumped |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise a `snake_case` body | the own enums (`assertion_signing_policy`, `assertion_encryption_policy`, …) bind |
| deserialise a body omitting fields | omitted fields are null |
| deserialise a body with an **unknown** field | rejected |

---

## Update SAML2 Attribute Query profile — `PATCH /v1/trust/config/trust-relationships/{id}/profiles/saml2-attribute-query`

Request DTO: `Saml2AttributeQueryProfileConfigurationRequest` — all optional; adds the SamlAssertion
fields (`assertion_time_condition`, `assertion_lifetime` ISO-8601 duration string, `assertion_signing_policy`),
`friendly_name_randomization_policy`, and the encryption policies. Mapper:
`TrustRelationshipMapper.updateSaml2AttributeQueryProfileConfiguration(existing, request)` — seed-from-current,
override-present; `assertion_lifetime` parsed via `Duration.parse` (`InvalidDurationSyntax` on malformed).

### Mapper

| Given | Then |
|-------|------|
| `assertion_lifetime` = `"PT5M"` | success; the profile's assertion lifetime is 5 minutes |
| `assertion_lifetime` malformed | failure (`InvalidDurationSyntax`) |
| only `friendly_name_randomization_policy` provided | success; that field changes, others (assertion lifetime, status, …) unchanged |
| an empty request | success; profile unchanged, version not bumped |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise a `snake_case` body | the ISO-8601 duration string and enums (`assertion_time_condition`, `friendly_name_randomization_policy`, …) bind |
| deserialise a body omitting fields | omitted fields are null |
| deserialise a body with an **unknown** field | rejected |

---

## Update Shibboleth SSO profile — `PATCH /v1/trust/config/trust-relationships/{id}/profiles/shibboleth-sso`

Request DTO: `ShibbolethSsoProfileConfigurationRequest` — all optional; adds the Authentication fields
(`post_authentication_flows`, `authentication_result_reuse_policy`, `max_authentication_age` duration),
`attribute_statement_policy`, and `nameid_format_precedence` (ordered string array). Two ISO-8601
duration fields. Mapper: `TrustRelationshipMapper.updateShibbolethSsoProfileConfiguration(existing, request)`
— seed-from-current, override-present; durations via `Duration.parse` (`InvalidDurationSyntax` on malformed).

### Mapper

| Given | Then |
|-------|------|
| `max_authentication_age` + `assertion_lifetime` durations | success; both parsed to `Duration` |
| a malformed `max_authentication_age` | failure (`InvalidDurationSyntax`) |
| `nameid_format_precedence` array | success; the ordered NameID formats are set |
| only `attribute_statement_policy` provided | success; that field changes, others (max auth age, status, …) unchanged |
| an empty request | success; profile unchanged, version not bumped |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise a `snake_case` body | durations (strings), enums, and the `nameid_format_precedence` array bind |
| deserialise a body omitting fields | omitted fields are null |
| deserialise a body with an **unknown** field | rejected |

---

## Update SAML2 ECP profile — `PATCH /v1/trust/config/trust-relationships/{id}/profiles/saml2-ecp`

Request DTO: `Saml2EcpProfileConfigurationRequest` — 19 optional fields; adds the Saml2Sso-capability
fields (`authentication_result_reuse_policy`, `assertion_encryption_policy`, `attribute_encryption_policy`,
`maximum_sp_session_lifetime` duration, `endpoint_validation_policy`, `attribute_statement_policy`,
`friendly_name_randomization_policy`, `nameid_format_precedence`, `request_signing_requirement`). Two
ISO-8601 durations (`assertion_lifetime`, `maximum_sp_session_lifetime`). Mapper:
`TrustRelationshipMapper.updateSaml2EcpProfileConfiguration(existing, request)` — seed-from-current,
override-present.

### Mapper

| Given | Then |
|-------|------|
| `assertion_lifetime` + `maximum_sp_session_lifetime` durations | success; both parsed to `Duration` |
| a malformed `maximum_sp_session_lifetime` | failure (`InvalidDurationSyntax`) |
| `endpoint_validation_policy` + `request_signing_requirement` | success; both applied |
| only one field provided | success; that field changes, others unchanged |
| an empty request | success; profile unchanged, version not bumped |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise a `snake_case` body | durations (strings), the Saml2Sso enums and the `nameid_format_precedence` array bind |
| deserialise a body omitting fields | omitted fields are null |
| deserialise a body with an **unknown** field | rejected |

---

## Update SAML2 SSO profile — `PATCH /v1/trust/config/trust-relationships/{id}/profiles/saml2-sso`

Request DTO: `Saml2SsoProfileConfigurationRequest` — the fullest profile, 21 optional fields (ECP's 19
plus the Authentication capability's `post_authentication_flows` and `max_authentication_age`). Three
ISO-8601 durations (`max_authentication_age`, `assertion_lifetime`, `maximum_sp_session_lifetime`).
Mapper: `TrustRelationshipMapper.updateSaml2SsoProfileConfiguration(existing, request)` —
seed-from-current, override-present.

### Mapper

| Given | Then |
|-------|------|
| all three durations | success; each parsed to `Duration` |
| a malformed `assertion_lifetime` | failure (`InvalidDurationSyntax`) |
| an Authentication-capability field (`authentication_result_reuse_policy`) | success; applied |
| only one field provided | success; that field changes, others unchanged |
| an empty request | success; profile unchanged, version not bumped |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise a `snake_case` body | all three durations (strings), the Authentication fields, enums and arrays bind |
| deserialise a body omitting fields | omitted fields are null |
| deserialise a body with an **unknown** field | rejected |

---

## Set released attributes — `PUT /v1/trust/config/trust-relationships/{id}/released-attributes`

Request DTO: `UpdateReleasedAttributesRequest` — `{ attributes: [ReleasedAttributeDto] }`, a full
replacement (empty array clears). Items reuse `ReleasedAttributeDto` (`{id, display_name}`), now
bidirectional. Mapper: `TrustRelationshipMapper.updateReleasedAttributes(existing, request)` — builds a
domain `ReleasedAttributes` (each item needs an id and non-blank display name), then delegates.
Response: `TrustRelationshipSummary`.

### Mapper

| Given | Then |
|-------|------|
| a list with one `{id, display_name}` | success; the trust relationship releases exactly that attribute |
| an empty list against a TR that had attributes | success; the released attributes are cleared |
| an item with a blank display name | failure (`RequiredValueMissing`) |
| an item with no id | failure (`RequiredValueMissing`) |
| a body with no `attributes` field | failure (`RequiredValueMissing`) |
| an empty list against an already-empty TR | success; version not bumped |

### JSON — wire contract

| Given | Then |
|-------|------|
| deserialise `{ attributes: [{id, display_name}] }` | items bind (`id` as UUID, `display_name`) |
| deserialise `{ attributes: [] }` | empty list |
| deserialise an item with an **unknown** field | rejected |

---

## Read metadata source — `GET /v1/trust/config/trust-relationships/{id}/metadata-source`

Response DTO: polymorphic `MetadataSourceView` (`oneOf` + `type`), one subtype per source kind. `FILE`
exposes the stored `file_path`; `MANUAL` returns the full base64 `signing_certificate` (or null) and an
ISO-8601 `valid_until`. Mapper: `TrustRelationshipMapper.toMetadataSourceView(trustRelationship)` —
projects the current domain source; no domain change was needed (the `MANUAL` read gap was already
closed during the MANUAL write work).

### Mapper — `toMetadataSourceView(...)`

| Given | Then |
|-------|------|
| a TR with no metadata source | `NoneMetadataSourceView` |
| a URI source | `UriMetadataSourceView` with the URL |
| an UPSTREAM source | `UpstreamMetadataSourceView` with `parent_id`, `entity_id` |
| an MDQ source | `MdqMetadataSourceView` with the base URL |
| a FILE source | `FileMetadataSourceView` exposing the stored reference as `file_path` |
| a MANUAL source with a certificate | `ManualMetadataSourceView` with entity id, ISO `valid_until`, ACS fields and the base64 certificate |
| a MANUAL source without a certificate | `signing_certificate` is null |

### JSON — wire contract (polymorphic)

| Given | Then |
|-------|------|
| serialise any view | the `type` discriminator is written; fields are `snake_case` |
| serialise `NONE` | `{ "type": "NONE" }` only |
| serialise `MANUAL` | nested `assertion_consumer_service` (`location`, `binding`, `index`, `is_default`), `valid_until`, `signing_certificate` all bind |

---

## Read profile configs (unified) — `GET /v1/trust/config/trust-relationships/{id}/profiles`

Response DTO: `ProfilesView` — a keyed object (`shibboleth_sso`, `saml2_sso`, `saml2_artifact_resolution`,
`saml2_attribute_query`, `saml2_ecp`, `saml2_logout`); only requested profiles are present. Optional
`profiles` filter (list of `ProfileType`); absent → all six. Mapper:
`TrustRelationshipMapper.toProfilesView(tr, requested)` — builds each requested profile's full view
(enums, ISO-8601 durations, string-list flows). One call replaces per-profile round-trips.

### Mapper — `toProfilesView(tr, requested)`

| Given | Then |
|-------|------|
| `requested = null` | all six profile views are populated |
| `requested = {SAML2_SSO, SAML2_LOGOUT}` | only those two views populated; the other four are absent |
| a TR whose SAML2 SSO was configured (`status=ACTIVE`, `assertion_lifetime=PT5M`) | the SAML2 SSO view reflects those values |
| any profile with duration fields | durations are exposed as ISO-8601 strings (e.g. `max_authentication_age` starts with `PT`) |

### JSON — wire contract

| Given | Then |
|-------|------|
| serialise a `ProfilesView` with only `saml2_logout` set | top-level keys are exactly `saml2_logout` (absent profiles omitted); nested fields are `snake_case`, enums verbatim |
| serialise a view with a duration field | the duration is a string (e.g. `assertion_lifetime` = `"PT5M"`) |
