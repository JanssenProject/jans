# File Staging API — Implementation Guide

> **Issue draft.** Restates the contract for the implementer; it will drift. Source of truth:
> `openapi/file-staging/file-staging-api.yaml` and the Java public interfaces named below.
> Read [`README.md`](./README.md) first for the shared model and `Result`/`problem+json` conventions.

## What this API is

A small, deliberately **trust-agnostic** service for moving file bytes into a shared document store,
so that no bytes ever ride the JSON contracts of the config or activation APIs. It lives at a neutral
base path `/v1/files` (not under `/trust`) because it is shared infrastructure, not owned by any trust
relationship — it can later be promoted to its own deployable when a second consumer appears. The
trust config API is its first consumer: a `FILE` metadata source is declared with an upload token,
which the config server claims.

**Two calls, two principals:**

- **`upload`** — an external client (UI/CLI) stages raw bytes and gets back an opaque, short-lived
  **token** plus integrity/lifecycle metadata. Until claimed, a staged file is reaped by a background
  cleaner once it expires.
- **`claim`** — a store-connected backend (the config server) takes ownership of a token. This is a
  **control-plane call: no bytes cross HTTP.** Staging *moves* the entry within the shared document
  store to the destination the caller names and returns the durable **handle** (the resulting path).
  The caller stores that handle and later reads it **directly from the store**. There is no download
  (`resolve`) endpoint and no consumer-initiated delete.

The store is a networked, shared, permission-less drive, so the **scope check at upload/claim is the
only enforceable authorization and audit point for files.** Destination-path validation on claim is
namespace hygiene (no accidental clobbering), not a security sandbox.

## Endpoint catalog

| Method | Path | Operation |
|---|---|---|
| `POST` | `/` | Stage a file (`application/octet-stream` body). |
| `POST` | `/{token}/claim` | Claim a staged file (take ownership + move to durable storage). |

**Authorization is OAuth2 client-credentials with two scopes per call** — a coarse service scope
**and** a per-operation scope, both required (an AND):

- `upload` requires `…/files` + `…/files.upload`.
- `claim` requires `…/files` + `…/files.claim`.

An uploader holds only `files.upload`; a claimer holds only `files.claim`.

## Public interfaces you call

**Application service** — `io.jans.staging.FileStagingService` (`final`; build via the factory, which
returns a `Result`). All methods return `io.jans.kernel.Result<T>`:

```java
static Result<FileStagingService> create(
    StagedFileRepository repository, ContentStore contentStore, FileStorageLayout layout,
    TimeSource timeSource, TokenGenerator tokenGenerator, Duration ttl);

Result<StagedFile> stage(ContentSource content, ContentType contentType);   // upload
Result<Handle>     claim(Token token, Destination destination);             // claim (idempotent per (token, destination))
Result<Integer>    reapExpired();                                           // background cleaner — NOT an endpoint
```

**Adapters are already provided** in `file-staging-adapters` — construct these and pass them to
`create`:

| Port | Provided impl | Notes |
|---|---|---|
| `ContentStore` | `DocumentStoreContentStore(DocumentStore<?>)` | streams + SHA-256 hashes in one pass |
| `FileStorageLayout` | `DefaultFileStorageLayout` / `.withDefaults(Destination)` | filename derived from token |
| `TimeSource` | `SystemTimeSource` | |
| `TokenGenerator` | `UuidTokenGenerator` | |
| `StagedFileRepository` | `StagedFileRepositoryImpl(PersistenceEntryManager, String baseDn)` | jans-orm |

**DTOs & mapper** (`io.jans.staging.dto`): `StagedFileView` (upload response), `ClaimRequest` /
`ClaimResult` (claim), and `StagedFileMapper` (static): `toView(StagedFile)`,
`toClaimResult(StagedFile)`, `toDestination(ClaimRequest) → Result<Destination>`.

**Boundary value types** you build from the HTTP request:
`ContentSource` (`@FunctionalInterface InputStream open()` — pass `() -> request.getInputStream()`, or
`ContentSource.ofBytes(byte[])`), `ContentType.of(String)` / `none()`, `Token.of(String) → Result`,
`Destination.of(String) → Result`.

## Wiring recipe

- **`POST /`** — read the request `Content-Type` into a `ContentType` (or `none()`), wrap the request
  stream in a `ContentSource`, call `stage(...)`, and on success return `201` +
  `Location: /v1/files/{token}/claim` + `StagedFileMapper.toView(stagedFile)`.
- **`POST /{token}/claim`** — parse the path via `Token.of`, build the `Destination` via
  `StagedFileMapper.toDestination(claimRequest)`, call `claim(token, destination)`, return `200` +
  `ClaimResult`.

### ⚠️ Known gap to resolve: building the full `ClaimResult`

`FileStagingService.claim(...)` returns **only a `Handle`**, but `ClaimResult` (and
`StagedFileMapper.toClaimResult`) need the claimed `StagedFile` (its `size`, `content_type`,
`sha256`). There is currently no path from `claim`'s return value to a full `ClaimResult`. The
implementer must either (a) re-fetch the claimed file via the repository to assemble the view, or
(b) extend the service to return the claimed `StagedFile`. **Flag this in the issue** — option (b) is
the cleaner fix and is a small domain change, not transport-only.

## Error → HTTP mapping

| Domain error | HTTP | `code` |
|---|---|---|
| `InvalidDestination` | `400` | (malformed/blank/relative/`..` destination) |
| `RequiredValueMissing` (empty upload body, null token/destination) | `400` | `required_value_missing` |
| missing/invalid token | `401` | |
| valid token, missing scope | `403` | `insufficient_scope` |
| `TokenNotFound` (unknown token) | `404` | `file_not_found` |
| `TokenExpired` / `AlreadyClaimed` (expired or already reaped/claimed) | `409` | `file_expired` |
| oversized upload | `413` | `file_too_large` |
| unaccepted `content_type` | `415` | `unsupported_media_type` |
| `ContentUnreadable` | `500` | |

(The size-limit and content-type-accept checks are transport-layer policy the implementer enforces
before/around the service call; the service itself raises the `Token*`/`Content*`/`RequiredValueMissing`
errors.)

## Worked example: file-backed metadata, end to end

The client stages the metadata file:
```http
POST /v1/files?content_type=application/samlmetadata+xml
Content-Type: application/octet-stream

<raw XML bytes>
```
```http
201 Created
Location: /v1/files/f1a2b3c4-…-6a7b/claim

{ "token": "f1a2b3c4-…-6a7b", "size": 20480,
  "content_type": "application/samlmetadata+xml",
  "sha256": "9f86d081…0a08", "expires_at": "2026-08-12T13:00:00Z" }
```
The client hands the `token` to the config API as a `FILE` metadata source
(see [`trust-config-api.md`](./trust-config-api.md)). The config server then **claims** it — no bytes
move over HTTP; staging relocates the entry inside the shared store:
```http
POST /v1/files/f1a2b3c4-…-6a7b/claim
{ "destination": "/opt/shibboleth-idp/metadata/" }
```
```http
200 OK
{ "handle": "/opt/shibboleth-idp/metadata/f1a2b3c4-…-6a7b.xml", "size": 20480,
  "content_type": "application/samlmetadata+xml", "sha256": "9f86d081…0a08" }
```
The config server stores that `handle` as the source's `file_path`; the activation worker later reads
it directly from the store. A retry of the same claim yields the same handle (idempotent); a claim
after expiry gets `409 file_expired`.

## Provisioning

The installer must create the staged-file branch under `o=jans` (name it consistently with the
`StagedFileRepositoryImpl` base DN you configure), and the deployment must grant the two OAuth2 scopes
to the uploader and claimer clients respectively.
