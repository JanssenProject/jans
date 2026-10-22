---
tags:
  - administration
  - lock
  - authorization / authz
  - Cedar
  - Cedarling
  - policy store
---

# Cedarling Policy Store

The Policy Store provides:

1. **Cedar Schema**: The Cedar schema encoded in Base64.
2. **Cedar Policies**: The Cedar policies encoded in Base64.
3. **Trusted Issuers**: Every accepted token type, JWT and custom, with its issuer (see [`trusted-issuers.json`](#trusted-issuersjson)).
4. **Default Entities**: Optional static entities that are loaded at startup and available for all policy evaluations (see [below](#default-entities)).

For a JSON schema defining the legacy single-file policy store structure used in test fixtures, see: [policy_store_schema.json](https://raw.githubusercontent.com/JanssenProject/jans/refs/heads/main/jans-cedarling/schema/policy_store_schema.json). For production environments, use the [folder-based policy store format](#2-new-directory-based-format).

## Policy Store Formats

Cedarling supports the folder-based policy store format (as a directory or `.cjar` archive) and automatically detects the format based on the file extension (for local files) or response body content (for URIs):

| Configuration | Detection |
|---------------|-----------|
| `CEDARLING_POLICY_STORE_URI` | Cedar Archive (`.cjar` or zip archive) from URL |
| `CEDARLING_POLICY_STORE_LOCAL_FN` pointing to directory | Directory-based format |
| `CEDARLING_POLICY_STORE_LOCAL_FN` with `.cjar` extension | Cedar Archive file |
| `CEDARLING_POLICY_STORE_LOCAL_FN` with `.yaml`/`.yml` extension | YAML file (supported for test suites) |

!!! warning "Legacy JSON Format Removed"
    Support for converting legacy JSON policy stores (originally produced by Agama Lab or Lock Server) has been removed. Consumers must migrate to the folder-based policy store format (directory or `.cjar` archive). YAML remains supported exclusively for test suites.

### 1. Legacy Single-File Format (YAML Test Suites)

The legacy single-file format stores policies and schema in a single YAML file with Base64-encoded content, supported exclusively for test suites. This structure is documented in detail in the sections below.

### 2. New Directory-Based Format

The new directory-based format uses human-readable Cedar files organized in a structured directory:

```text
policy-store/
├── metadata.json           # Required: Store identification and versioning
├── schema.cedarschema      # Option A: Single schema file (takes precedence)
├── schemas/                # Option B: Split schema directory (fallback)
│   ├── users.cedarschema
│   └── resources.cedarschema
├── policies/               # Required: Directory containing .cedar policy files
│   ├── allow-read.cedar
│   └── deny-guest.cedar
├── templates/              # Optional: Directory containing .cedar template files
├── entities/               # Optional: Directory containing .json entity files
└── trusted-issuers.json    # Optional: Every accepted token type (JWT and custom)
```

The schema can be provided in one of two ways (but not both):

- **Option A — single file**: `schema.cedarschema` at the store root. This is the original format.
- **Option B — split across multiple files**: `schemas/*.cedarschema` directory. Each file is parsed as an independent `SchemaFragment`, then merged via `Schema::from_schema_fragments`. This is useful for large schemas with multiple namespaces.

If both exist, the single file `schema.cedarschema` takes precedence and the `schemas/` directory is ignored. If neither exists, behavior depends on `CEDARLING_STRICT_SCHEMA_VALIDATION`: with strict mode enabled (the default), loading fails with a `MissingSchemaSource` error; with strict mode disabled, the store loads successfully and policies run without schema-based attribute validation.

#### metadata.json

Contains policy store identification and versioning:

```json
{
  "policy_store_spec_version": 2,
  "cedar_version": "4.4.0",
  "policy_store": {
    "id": "abc123def456",
    "name": "My Application Policies",
    "description": "Optional description",
    "version": "1.0.0",
    "created_date": "2024-01-01T00:00:00Z",
    "updated_date": "2024-01-02T00:00:00Z"
  }
}
```

`policy_store_spec_version` declares which revision of the policy store file
format the store is written in. See
[Policy store specification versions](#policy-store-specification-versions).

For `.cjar`/directory stores the ID comes from `metadata.json`. It must be
hex (8-64 characters), otherwise loading fails with `InvalidPolicyStoreId`.
It may be omitted, in which case the getter reports none.

#### Policy Files

Policies are stored as human-readable `.cedar` files in the `policies/` directory:

```cedar
@id("allow-read")
permit(
    principal,
    action == MyApp::Action::"read",
    resource
);
```

When a `.cedar` file contains **multiple** policies, each policy must have its
own Cedar `@id("...")` annotation so every policy has a stable, unique id (the
loader does not fall back to the filename in that case).

For a **single-policy** file, `@id("...")` is optional: if it is omitted, the
parser still accepts the file and derives the policy id from the filename
(without the `.cedar` extension), after validation.

##### Multiple policies per file

A single `.cedar` file may contain more than one policy. In that case each policy
must have its own `@id("...")` annotation:

```cedar
@id("researcher-search")
permit(
    principal == User::"researcher",
    action == Action::"search",
    resource
);

@id("researcher-edit")
permit(
    principal == User::"researcher",
    action == Action::"edit",
    resource
);
```

#### Template Files

Templates are stored as human-readable `.cedar` files in the `templates/` directory:

```cedar
@id("resource-access-template")
permit(
    principal == ?principal,
    action,
    resource == ?resource
);
```

Each template file must have an `@id` annotation and use Cedar's template slot syntax (`?principal`, `?resource`).

#### Entity Files

Entity files in the `entities/` directory use the Cedar JSON entity format as a **JSON array**. Each file can contain one or more entity definitions:

```json
[
  {
    "uid": {
      "type": "Jans::Organization",
      "id": "acme-dolphins"
    },
    "attrs": {
      "name": "Acme Dolphins Division",
      "org_id": "100129",
      "domain": "acme-dolphin.sea",
      "regions": ["Atlantic", "Pacific", "Indian"]
    },
    "parents": []
  },
  {
    "uid": {
      "type": "Jans::Role",
      "id": "admin"
    },
    "attrs": {
      "name": "Administrator",
      "permissions": ["read", "write", "delete"]
    },
    "parents": []
  }
]
```

Each entity requires:

- **`uid`**: Object with `type` (Cedar entity type name, e.g., `"Jans::Organization"`) and `id` (unique entity identifier)
- **`attrs`**: Object containing entity attributes matching your Cedar schema
- **`parents`**: Optional array of parent entity references for hierarchical relationships

Example with parent relationships (`entities/users.json`):

```json
[
  {
    "uid": {
      "type": "Jans::User",
      "id": "alice"
    },
    "attrs": {
      "name": "Alice Smith",
      "email": "alice@example.com"
    },
    "parents": [
      {"type": "Jans::Role", "id": "admin"},
      {"type": "Jans::Organization", "id": "acme-dolphins"}
    ]
  }
]
```

#### trusted-issuers.json

`trusted-issuers.json` lists every token type the store accepts, JWT and custom
alike, as one JSON object keyed by Cedar entity type. Each entry fully describes
one token type, including its issuer and its kind. A store without the file
accepts no tokens.

```json
{
  "Acme::Access_token": {
    "type": "jwt",
    "issuer": "Acme",
    "configuration_endpoints": {
      "configuration": "https://idp.acme.com/.well-known/openid-configuration"
    },
    "token_id": "jti",
    "required_claims": ["jti", "iss", "aud", "sub", "exp"]
  },
  "Acme::Id_token": {
    "type": "jwt",
    "issuer": "Acme",
    "configuration_endpoints": {
      "configuration": "https://idp.acme.com/.well-known/openid-configuration"
    },
    "required_claims": ["iss", "aud", "sub", "exp"]
  },
  "Partner::Access_token": {
    "type": "jwt",
    "issuer": "Partner",
    "configuration_endpoints": {
      "jwks": "https://auth.partner.io/keys",
      "iss": "https://auth.partner.io"
    },
    "required_claims": ["iss", "exp"]
  },
  "Dolphin::Dolphin_token": {
    "type": "custom",
    "issuer": "Dolphin",
    "required_claims": ["sub"]
  }
}
```

| Field | Kind | Required | Meaning |
| --- | --- | --- | --- |
| key | all | yes | Cedar entity type. A request `TokenInput.mapping` must equal it exactly. Its last segment, lowercased, is the type part of the `context.tokens` key. |
| `type` | all | yes | `jwt` or `custom`; selects which other fields are allowed. |
| `issuer` | all | yes | Issuer identity, a single Cedar identifier (`^[A-Za-z_][A-Za-z0-9_]*$`) that is not a Cedar reserved word (`if`, `true`, `in`, `__cedar`, ...). It is the namespace of the issuer's `TrustedIssuer` entity (`Acme::TrustedIssuer`) and, lowercased, the prefix of its `context.tokens` keys (`acme_access_token`), so renaming it changes what policies see. |
| `configuration_endpoints.configuration` | jwt | one of two forms | OIDC discovery URL: the issuer URL followed by `/.well-known/openid-configuration`. The expected `iss` is that issuer URL, path included; keys come from the discovered `jwks_uri`. |
| `configuration_endpoints.jwks` + `configuration_endpoints.iss` | jwt | one of two forms | Static form, without discovery: keys come from `jwks`, and the expected `iss` is given explicitly. |
| `token_id` | jwt | no, default `jti` | Claim used as the token entity id. |
| `required_claims` | all | no, default `[]` | JWT: the claims must be present, and registered claims ([RFC 7519 §4.1](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1)) such as `exp` or `nbf` are validated. Custom: the claims must be present in the processor output. |

Unknown fields are rejected, so a misspelled field, or a JWT-only field such as
`configuration_endpoints` on a `custom` entry, fails the load with a message
naming it.

These rules are checked when the store loads, and a violation fails the load:

- Each entity type appears once. A duplicate key is an error rather than
  "last one wins".
- All entries with the same `issuer` describe one issuer: they must have the same
  `type` and, for JWT, the same `iss` and key source. Cedarling fetches the
  discovery document and the keys once per issuer.
- Different issuers may share an `iss` and its keys, for example one identity
  provider serving two applications with different schemas. Each gets its own
  `TrustedIssuer` entity. Two static entries that declare the same `iss` with
  different `jwks` are rejected.
- Every `context.tokens` key is unique: `Acme` and `ACME` would both produce
  `acme_access_token`.
- An entry uses exactly one key-source form: `configuration` together with
  `jwks`, or `jwks` without `iss`, is rejected.

Two key sources that only turn out to share an `iss` after discovery, with
different JWKS, are both rejected: there is no order between them, so neither
issuer loads, and each is logged at `ERROR` naming the issuers and both JWKS.
An issuer whose discovery fails takes no part in this check.

A JWT is accepted only for its entry's issuer: its `iss` must equal the expected
`iss`, and the discovery document's `issuer` must equal it too, or that issuer
fails to load. Custom tokens are validated by a registered `CustomTokenProcessor`
instead of the JWT pipeline, see
[Custom (Non-JWT) Token Processing](./cedarling-multi-issuer.md#custom-non-jwt-token-processing).
A custom issuer has no `TrustedIssuer` entity, and a custom token's `iss`
attribute is its lowercased `issuer` string.

Every token type is optional: a token that fails validation or processing is
dropped and the request continues with the remaining tokens. A policy that needs
a token must require it, see
[Token failures and `forbid` policies](./cedarling-multi-issuer.md#token-failures-and-forbid-policies).

#### Cedar Archive (.cjar) Format

The directory structure can be packaged as a `.cjar` file (ZIP archive) for distribution:

```bash
# Create a .cjar archive from a policy store directory
cd policy-store && zip -r ../policy-store.cjar .
```

**Note:** In WASM environments, only URL-based and inline string sources are available. Use `CEDARLING_POLICY_STORE_URI` with a `.cjar` URL or `initFromArchiveBytes()` for custom fetch scenarios.

## Policy store specification versions

`metadata.json` carries `policy_store_spec_version`, an integer naming the
revision of the **file format** a store is written in. It is distinct from the
two other versions in the file: `policy_store.version` is your own content
version, and `cedar_version` is the Cedar language version. Cedarling reads the
spec version before anything else in the store, so an unsupported format fails
with a version error instead of a confusing schema or policy error.

The current version is **2**. The oldest version Cedarling still reads is **0**.

| `policy_store_spec_version` | Behavior |
| --- | --- |
| omitted, or `null` | Read as version 0, migrated to the current version, with a warning |
| `0` | Read as version 0, migrated to the current version, with a warning |
| `1` | Read as version 1, migrated to the current version, with a warning |
| `2` | Read as version 2, no warning |
| `3` or higher | Error: newer than this Cedarling supports |
| not a non-negative integer (`"1"`, `-1`, `1.5`) | Error |

Each version below is a numbered entry of the specification. Any change to the
policy store file format bumps `policy_store_spec_version` and adds its entry to
this section in the same pull request.

The directory layout is part of the file format. A spec version fixes where
each file lives (`policies/`, `trusted-issuers.json`, `schema.cedarschema` and
so on), so moving, renaming, or merging files is also a new spec version. The one
exception is `metadata.json`: Cedarling reads it before it knows the version, so
it stays at the root of the store in every version.

### Version 0

**Status:** supported, outdated. **Change type:** baseline.

The original directory-based and `.cjar` format. It predates
`policy_store_spec_version`, so any store that omits the field is read as
version 0. An explicit `0` means the same thing.

```json
{
  "cedar_version": "4.4.0",
  "policy_store": {
    "id": "abc123def456",
    "name": "My Application Policies"
  }
}
```

### Version 1

**Status:** supported, outdated. **Change type:** additive.

**What changed:** `metadata.json` gains the required `policy_store_spec_version`
field. No other file changes.

**Why:** without a declared format version, Cedarling cannot tell which revision
of the format a store follows, so a breaking change would be silently misread
instead of migrated or rejected.

Before (version 0):

```json
{
  "cedar_version": "4.4.0",
  "policy_store": {
    "id": "abc123def456",
    "name": "My Application Policies"
  }
}
```

After (version 1):

```json
{
  "policy_store_spec_version": 1,
  "cedar_version": "4.4.0",
  "policy_store": {
    "id": "abc123def456",
    "name": "My Application Policies"
  }
}
```

**Upgrading from version 0:**

1. Add `"policy_store_spec_version": 1` at the root of `metadata.json`.
2. No other file in the store needs to change.
3. Run `cedarling-cli validate` against the store. It should pass with no
   "outdated" warning.

A version 0 store keeps loading, so upgrading is not urgent, but every load logs
the warning below.

### Version 2

**Status:** current. **Change type:** breaking, migrated automatically.

**What changed:** the `trusted-issuers/` and `custom-issuers/` directories are
replaced by one [`trusted-issuers.json`](#trusted-issuersjson), a map keyed by
Cedar entity type in which each entry describes one token type and its issuer.
A version 2 store that still contains either directory loads, ignores it and
logs a warning.

**Why:** version 1 described JWT and custom tokens in two shapes keyed
differently (JWT tokens by a token name with a separate `entity_type_name`,
custom tokens by entity type), while a request names a token by its entity type.
One map keyed by entity type removes the token name, the issuer id and the
lookup between them, and lets every conflict be checked in one place when the
store loads.

Before (version 1), `trusted-issuers/acme.json`:

```json
{
  "name": "Acme",
  "description": "Acme identity provider",
  "configuration_endpoint": "https://idp.acme.com/.well-known/openid-configuration",
  "token_metadata": {
    "access_token": {
      "entity_type_name": "Acme::Access_token",
      "token_id": "jti",
      "required_claims": ["exp"]
    },
    "id_token": { "trusted": false, "entity_type_name": "Acme::Id_token" }
  }
}
```

and `custom-issuers/dolphin.json`:

```json
{
  "tokens_mappings": {
    "Dolphin::Dolphin_token": { "required": true, "required_claims": ["sub"] }
  }
}
```

After (version 2), `trusted-issuers.json`:

```json
{
  "Acme::Access_token": {
    "type": "jwt",
    "issuer": "Acme",
    "configuration_endpoints": {
      "configuration": "https://idp.acme.com/.well-known/openid-configuration"
    },
    "token_id": "jti",
    "required_claims": ["exp"]
  },
  "Dolphin::Dolphin_token": {
    "type": "custom",
    "issuer": "dolphin",
    "required_claims": ["sub"]
  }
}
```

and `"policy_store_spec_version": 2` in `metadata.json`.

**Migration:** a version 0 or 1 store is migrated in memory when it loads:

| Version 1 | Version 2 |
| --- | --- |
| `trusted-issuers/X.json`, `token_metadata.<key>` | entry keyed by its `entity_type_name`: `type: jwt`, `issuer` = the file's `name`, `configuration_endpoints.configuration` = its endpoint, same `token_id` and `required_claims` |
| `custom-issuers/X.json`, `tokens_mappings.<type>` | entry keyed by `<type>`: `type: custom`, `issuer` = the file's id, sanitized as before so its `context.tokens` keys stay the same, same `required_claims` |
| `trusted: false` | entry dropped, as version 1 never accepted those tokens; an issuer left with no tokens is dropped |
| `required: true` | ignored, with a warning: every token is optional |
| `description`, issuer `id`, token key | dropped |
| `name` that is not a single Cedar identifier (`Gluu Server`, `A::B`) | error |
| endpoint not of the form `<iss>/.well-known/openid-configuration` | error |
| endpoint with a path before `/.well-known/openid-configuration` | warning: `iss` keeps the path, see below |
| the same entity type in two files, in either directory | error naming both files |

**Behavior changes to review when upgrading:**

- **`required` is gone.** A missing or failing custom token no longer fails the
  request. A policy that depends on a token must require it:

    ```cedar
    forbid (principal, action, resource)
    unless { context has tokens.acme_access_token };
    ```

- **A request `mapping` must equal a configured entity type.** There is no
  fallback from token names such as `access_token` to default entity types.
- **`iss` keeps its path.** Version 1 matched tokens against the origin of the
  configuration endpoint only. Version 2 expects the endpoint without
  `/.well-known/openid-configuration`, path included, so path-based issuers
  (Keycloak realms, `login.microsoftonline.com/{tenant}/v2.0`) are told apart.
  For those issuers the `TrustedIssuer` entity id and `issuer_entity_id.path` now
  carry the path. Issuers at the origin root are unaffected.
- **Discovery must match.** The discovery document's `issuer` must equal the
  expected `iss`, or that issuer fails to load.
- **Issuers are identified by name.** `TrustedIssuerLoadingInfo`
  (`is_trusted_issuer_loaded_by_name`, `loaded_trusted_issuer_ids`,
  `failed_trusted_issuer_ids`) reports issuer names (`Acme`) instead of
  version 1 file ids (`acme`).
- **Conflicts fail the load.** Files that shared an id are no longer rejected:
  JWT files stay separate issuers by name, and custom files merge into one
  issuer. A duplicate entity type, two descriptions of one issuer that disagree,
  or colliding `context.tokens` keys fail the load instead of one silently
  replacing another.

**Upgrading from version 1:**

1. Write `trusted-issuers.json` from your issuer files as in the table above,
   then delete the `trusted-issuers/` and `custom-issuers/` directories.
2. Set `"policy_store_spec_version": 2` in `metadata.json`.
3. Add `forbid … unless` policies for tokens that were `required`.
4. Run `cedarling-cli validate` against the store. It should pass with no
   warning.

A version 1 store keeps loading through the migration, so upgrading is not
urgent, but every load logs the warnings below.

### Unknown fields

Unknown keys in `metadata.json` are ignored in every version. In version 2,
unknown keys in `trusted-issuers.json` entries are rejected with an error naming
them. In versions 0 and 1, unknown keys in trusted-issuer files are ignored, so a
misspelled optional key is silently dropped, and unknown keys in custom-issuer
files are rejected. Migration carries over every field the newer version has a
place for and reports what it cannot carry over.

### Warnings

Warnings are non-fatal: the store loads and is migrated to the current version
in memory. Cedarling logs them once at startup at `WARN`, and the
`cedarling-cli validate` report lists them as diagnostics that do not change
whether validation passes. The background refresh worker de-duplicates them, so
a periodic refresh of an outdated store does not repeat the same warning.

When the field is missing:

```text
policy store format is outdated: metadata.json has no policy_store_spec_version, so it was read as spec version 0; please update the policy store to spec version 2
```

When the field declares a supported but older version:

```text
policy store format is outdated: policy_store_spec_version is 1; please update the policy store to spec version 2
```

When migrating a version 1 custom token that was `required`:

```text
'required' on custom token '<entity type>' in 'custom-issuers/<file>' is ignored: every token is optional; require it in policy with `forbid (principal, action, resource) unless { context has tokens.<key> };`
```

When migrating a version 1 issuer whose endpoint has a path:

```text
issuer in 'trusted-issuers/<file>' is now matched against iss '<iss>', path included, and its TrustedIssuer entity id carries the path; v1 used only the origin
```

When a version 2 store still contains `trusted-issuers/` or `custom-issuers/`:

```text
policy store directory 'trusted-issuers/' is ignored: this spec version reads trusted issuers from trusted-issuers.json only
```

### Errors

These fail closed — the store is not loaded.

```text
Invalid metadata in file metadata.json: policy_store_spec_version must be a non-negative integer, got <value>
```

```text
Unsupported policy store: policy_store_spec_version <found> is newer than the latest version this Cedarling supports (2); upgrade Cedarling
```

```text
Unsupported policy store: policy_store_spec_version <found> is no longer supported; the oldest supported version is 0
```

The last error cannot occur while version 0 is still supported; it appears once a
release drops support for the oldest version.

A version 0 or 1 store that cannot be expressed in version 2 (see the
[migration table](#version-2)) fails with:

```text
Failed to migrate the policy store: <reason naming the file>
```

## Advanced: Loading from Bytes

For scenarios requiring custom fetch logic (e.g., auth headers), archive bytes can be loaded directly:

- **WASM**: Use `initFromArchiveBytes(config, bytes)` function
- **Rust**: Use `PolicyStoreSource::ArchiveBytes(Vec<u8>)` or `load_policy_store_archive_bytes()` function

```javascript
// WASM example with custom fetch
const response = await fetch(url, { headers: { Authorization: "..." } });
const bytes = new Uint8Array(await response.arrayBuffer());
const cedarling = await initFromArchiveBytes(config, bytes);
```

## Verifying policy store signatures

Agama Lab publishes each `.cjar` with a Sigstore (cosign keyless) bundle, `<store>.cjar.sigstore.json`, that signs the whole archive. With [`CEDARLING_POLICY_STORE_SIGSTORE_MODE`](./cedarling-properties.md#verifying-policy-store-signatures) set to `enforce`, Cedarling loads a `.cjar` only if that bundle verifies for the configured signer:

```json
"CEDARLING_POLICY_STORE_SIGSTORE_MODE": "enforce",
"CEDARLING_POLICY_STORE_SIGSTORE_CERT_IDENTITY": "asset-signer@prod-gluu-cloud-platform.iam.gserviceaccount.com",
"CEDARLING_POLICY_STORE_SIGSTORE_CERT_OIDC_ISSUER": "https://accounts.google.com"
```

The bundle is verified offline against the public Sigstore trust root (Fulcio, Rekor and CT log keys) built into Cedarling. The check runs on the downloaded or read archive bytes before they are parsed, and the same bytes are then loaded. It is equivalent to:

```bash
cosign verify-blob store.cjar \
  --bundle store.cjar.sigstore.json \
  --certificate-identity "asset-signer@prod-gluu-cloud-platform.iam.gserviceaccount.com" \
  --certificate-oidc-issuer https://accounts.google.com
```

If the bundle is missing, does not verify, or was signed by someone else, Cedarling fails to start. During [background refresh](#background-refresh) the current store is kept and the attempt is recorded as `VerificationError`. Each verified load is logged at `INFO`:

```text
policy store signature verified: identity=asset-signer@... issuer=https://accounts.google.com archive_sha256=<hex> rekor_integrated_time=<unix seconds>
```

### Where the bundle comes from

| Policy store source | Bundle location |
|---|---|
| `CEDARLING_POLICY_STORE_URI` or `CEDARLING_POLICY_STORE_CJAR_URL` | The archive URL with `.sigstore.json` appended to its path |
| `CEDARLING_POLICY_STORE_LOCAL_FN` with a `.cjar` file | The archive path with `.sigstore.json` appended |
| [Lock Server](./cedarling-lock-server.md#loading-policy-stores-from-lock-server) | Set `CEDARLING_POLICY_STORE_SIGSTORE_BUNDLE_URI` to the full URL `https://<lock-host>/jans-lock/api/v1/policy/{policy_store_id}/sigstore`. It is fetched with the same client as the store, so the Lock access token is sent |
| Archive bytes (`initFromArchiveBytes`, `load_from_json_with_archive_bytes`) | `CEDARLING_POLICY_STORE_SIGSTORE_BUNDLE_URI` is required |

`CEDARLING_POLICY_STORE_SIGSTORE_BUNDLE_URI` overrides the location for any source. Directory and YAML policy stores cannot be signed, so `enforce` with them fails at startup.

### Limitations

- A signature proves who published an archive, not that it is the latest one. Someone who controls the policy store URL can serve an older, validly signed archive.
- Refresh re-checks the signature only when the archive changes. A new bundle for the same archive is not fetched.
- If a new archive is published before its bundle, the next refresh fails and is retried with backoff.
- Bundles must carry a Rekor v1 transparency log entry with an inclusion proof, as `cosign sign-blob --bundle` writes them today.
- The trust root is built into each Cedarling release. A build older than a Sigstore root rotation stops verifying new signatures until it is upgraded.

## Background refresh

For URL-based policy store sources (`CEDARLING_POLICY_STORE_URI` pointing at a Lock Server endpoint or a `.cjar`), Cedarling can periodically re-fetch the policy store and atomically swap the in-memory `Authz` instance so long-running processes see policy updates without restart. Refresh is **opt-in** via [`CEDARLING_POLICY_STORE_REFRESH_INTERVAL`](./cedarling-properties.md#refreshing-the-policy-store) and is silently ignored for local sources.

### Per-request consistency

Every public Cedarling method snapshots the current `Authz` via `Arc<ArcSwap<Authz>>::load()` at entry and uses that snapshot for the duration of the call. A refresh that lands mid-request can never produce a partially-updated view — in-flight authorizations always finish against the pre-swap policy store. The post-swap store is visible to all *subsequent* calls.

The `policy_store_id()` getter reports the currently published store, so the
value can change after a refresh to a store with a different ID and callers
must not cache it.

### Strategy ladder

The worker selects from three fetch strategies and degrades automatically when the upstream proves it doesn't support the more efficient mode. A periodic probe attempts to upgrade back so a transiently misconfigured upstream doesn't permanently lock the worker into a heavier path.

| Strategy | Wire behavior | When the worker uses it |
|---|---|---|
| `Conditional` (default) | Conditional GET with `If-None-Match` / `If-Modified-Since`; expects `304 Not Modified` on no-op | Starting state. Lock Server / CDN deployments stay here. |
| `HeadThenGet` | HEAD first; compare returned `ETag` / `Last-Modified` against cache; full GET only when they differ | Auto-degraded into after three consecutive ticks where the server returned `200` with an identical body despite conditional headers |
| `PlainGet` | Plain GET every tick; relies on body-hash short-circuit to detect no-op reloads | Auto-degraded into after three consecutive HEAD responses with no useful validators, or immediately on HEAD `405` / `501` |

Across all three strategies the worker maintains a body-hash short-circuit: even when the server returns `200` with byte-identical bytes (some servers ignore conditional headers, some emit non-deterministic `ETag`s), Cedarling skips parse / rebuild / swap entirely.

A `Cache-Control: max-age` / `Expires` hint in any successful response may **shorten** the next sleep but never lengthens it past the operator-configured interval.

### Failure handling

Failures (network, HTTP, parse, rebuild, and, with `CEDARLING_POLICY_STORE_SIGSTORE_MODE=enforce`, a missing or invalid signature bundle) leave the previously loaded `Authz` in place and exponentially back off the next attempt — `interval × 2^failures`, capped at 600 seconds, with ±10% jitter. The worker recovers automatically on the next success.

### Metrics

The refresh worker emits the following keys into the `operational_stats` map of each metrics snapshot. Keys are emitted **sparsely** — only when the underlying value is non-zero — so dashboards can distinguish "no observation yet" from a genuine zero reading.

| Key | Meaning |
|---|---|
| `policy_store_refresh.last_attempt_secs` | Unix timestamp of the most recent tick attempt |
| `policy_store_refresh.last_success_secs` | Unix timestamp of the most recent `Success` or `NotModified` outcome |
| `policy_store_refresh.consecutive_failures` | Current failure streak (resets to zero on success) |
| `policy_store_refresh.last_outcome` | Integer enum: `1`=Success, `2`=NotModified, `3`=HttpError, `4`=NetworkError, `5`=ParseError, `6`=RebuildError, `7`=DecodeError, `8`=VerificationError |
| `policy_store_refresh.strategy_current` | Integer enum: `1`=Conditional, `2`=HeadThenGet, `3`=PlainGet |
| `policy_store_refresh.conditional_to_head_transitions` | Cumulative count of `Conditional → HeadThenGet` degrades |
| `policy_store_refresh.head_to_plain_transitions` | Cumulative count of `HeadThenGet → PlainGet` degrades |
| `policy_store_refresh.upgrade_to_head_transitions` | Cumulative count of probes that upgraded `PlainGet → HeadThenGet` |
| `policy_store_refresh.upgrade_to_conditional_transitions` | Cumulative count of probes that upgraded back to `Conditional` |
| `policy_store_refresh.outcome_success` | Cumulative count of `Success` outcomes |
| `policy_store_refresh.outcome_not_modified` | Cumulative count of `NotModified` outcomes |
| `policy_store_refresh.outcome_http_error` | Cumulative count of `HttpError` outcomes |
| `policy_store_refresh.outcome_network_error` | Cumulative count of `NetworkError` outcomes |
| `policy_store_refresh.outcome_parse_error` | Cumulative count of `ParseError` outcomes (body couldn't be parsed) |
| `policy_store_refresh.outcome_rebuild_error` | Cumulative count of `RebuildError` outcomes (body parsed, but `JwtService` / `EntityBuilder` / trusted-issuer rebuild failed) |
| `policy_store_refresh.outcome_decode_error` | Cumulative count of `DecodeError` outcomes (HTTP transaction succeeded but reading the response body failed — e.g. TCP drop mid-stream, transfer-encoding issue) |
| `policy_store_refresh.outcome_verification_error` | Cumulative count of `VerificationError` outcomes (the refreshed `.cjar` failed its Sigstore signature check, including a missing bundle). Failing to fetch the bundle counts like any other fetch failure instead: no response (timeout, connection refused) is `NetworkError`, a `5xx` is `HttpError`, and a failure while reading the bundle body is `DecodeError` |

## Legacy Single-File Format (YAML Test Suites)

!!! note "YAML Test Fixtures Only"
    The legacy JSON format has been removed in favor of folder-based policy stores (`.cjar` archives or directories). The single-file schema below is supported exclusively via YAML for automated test suites.

### Structure

The structure accepted for YAML test fixtures is defined as follows:

```json
{
  "cedar_version": "v4.0.0",
  "policy_stores": {
      "some_unique_string_id": {
          "name": "DoveCRM Policy Store",
          "description": "DoveCRM policies, schema, and trusted JWT issuers.",
          "policies": { ... },
          "schema": { ... },
          "trusted_issuers": { ... },
          "custom_issuers": { ... },
          "default_entities": { ... }
      }
  }
}
```

The ID is the `policy_stores` map key, is not required to be hex, and is not
validated.

- **cedar_version** : (_String_) The version of [Cedar policy](https://docs.cedarpolicy.com/). The protocols of this version will be followed when processing Cedar schema and policies.
- **policies** : (_Object_) Base64 encoded object containing one or more policy IDs as keys, with their corresponding objects as values. See: [policies schema](#cedar-policies-schema).
- **schema** : (_String_ | _Object_) Base64 encoded JSON Object. See [schema](#schema) below.
- **trusted_issuers** : (_Object of {unique_id => IdentitySource}_) Metadata for JWT issuers. See [Trusted Issuers Schema](#trusted-issuers-schema).
- **custom_issuers** : (_Object of {issuer_name => CustomIssuer}_) Optional map of custom (non-JWT) issuers validated by a registered `CustomTokenProcessor`, each `{ "tokens_mappings": { "<entity type>": { "required_claims": [...] } } }`. The issuer is identified by its name, sanitized (lowercased, with `.`, space and `-` replaced by `_`), which is also its `context.tokens` key prefix. `required` is accepted but ignored. See [Custom (Non-JWT) Token Processing](./cedarling-multi-issuer.md#custom-non-jwt-token-processing).
- **default_entities** : (_Object_) Optional map of entity IDs to encoded/default entity payloads. See [Default Entities](#default-entities).

### `schema`

Either _String_ or _Object_, where _Object_ is preferred.

Where _Object_ - An object with `encoding`, `content_type` and `body` keys. For example:

```json
"schema": {
    "encoding": "none", // can be one of "none" or "base64"
    "content_type": "cedar", // can be one of "cedar" or "cedar-json"
    "body": "namespace Jans {\ntype Url = {"host": String, "path": String, "protocol": String};..."
}
```

Where _String_ - The schema in cedar-json format, encoded as Base64. For example:

```json
"schema": "cGVybWl0KAogICAgc..."
```

## Default Entities

Default entities allow you to preload static entity records that are available to every authorization request. Cedarling loads these at startup alongside policies and schema.

Format:

- Each entry is a simple key-value pair where the key is the entity ID and the value is a Base64-encoded JSON object representing the entity payload.
- Default entities support two formats:
  - **Cedar format**: `{"uid": {"type": "...", "id": "..."}, "attrs": {...}, "parents": [...]}`
  - **Legacy format**: `{"entity_type": "...", "entity_id": "...", ...attributes...}` (for backward compatibility)

```json
"default_entities": {
  "1694c954f8d9": "eyJ1aWQiOnsidHlwZSI6IkphbnM6Ok9yZ2FuaXphdGlvbiIsImlkIjoiMTY5NGM5NTRmOGQ5In0sImF0dHJzIjp7Im8iOiJBY21lIERvbHBoaW5zIERpdmlzaW9uIiwib3JnX2lkIjoiMTAwMTI5IiwiZG9tYWluIjoiYWNtZS1kb2xwaGluLnNlYSIsInJlZ2lvbnMiOlsiQXRsYW50aWMiLCJQYWNpZmljIiwiSW5kaWFuIl19LCJwYXJlbnRzIjpbXX0=",
  "74d109b20248": "eyJ1aWQiOnsidHlwZSI6IkphbnM6OlByaWNlTGlzdCIsImlkIjoiNzRkMTA5YjIwMjQ4In0sImF0dHJzIjp7ImRlc2NyaXB0aW9uIjoiMjAyNSBQcmljZSBMaXN0IiwicHJvZHVjdHMiOnsiMTUwMjAiOjkuOTUsIjE1MDUwIjoxNC45NX0sInNlcnZpY2VzIjp7IjUxMDAxIjo5OS4wLCI1MTAyMCI6Mjk5LjB9fSwicGFyZW50cyI6W119"
}
```

Example of the decoded payloads for two default entities:

```json
{
  "1694c954f8d9": {
    "uid": {
      "type": "Jans::Organization",
      "id": "1694c954f8d9"
    },
    "attrs": {
      "o": "Acme Dolphins Division",
      "org_id": "100129",
      "domain": "acme-dolphin.sea",
      "regions": ["Atlantic", "Pacific", "Indian"]
    },
    "parents": []
  },
  "74d109b20248": {
    "uid": {
      "type": "Jans::PriceList",
      "id": "74d109b20248"
    },
    "attrs": {
      "description": "2025 Price List",
      "products": { "15020": 9.95, "15050": 14.95 },
      "services": { "51001": 99.0, "51020": 299.0 }
    },
    "parents": []
  }
}
```

Notes:

- Cedar policies can reference these entities directly by type and ID, just like any other entity present during evaluation.
- A common use case is defining an organization entity and writing policies that compare runtime attributes (e.g., `resource.org_id`) to attributes on the default entity.

### Entity Conflict Resolution

When request entities have the same UID as default entities, Cedarling automatically resolves conflicts by giving **default entities precedence** over request entities. This ensures that policy-store entities — representing change-controlled, trusted shared state — cannot be overwritten by attacker-controlled request data.

Example: If a resource entity with UID `"org1"` is passed in an authorization request and a default entity with the same UID exists, the default entity's attributes will be used instead of the resource entity's attributes.

## Cedar Policies Schema

The `policies` field describes the Cedar policies that will be used in Cedarling. Multiple policies can be defined, with each policy requiring a `unique_policy_id`.

```json
  "policies": {
    "unique_policy_id": {
      "description": "simple policy example",
      "policy_content": { ... },
    },
    ...
  }
```

- **unique_policy_id**: (_String_) A unique policy ID used to for tracking and auditing purposes.
- **description** : (_String_) A brief description of cedar policy
- **policy_content** : (_String_ | _Object_) The Cedar Policy. See [policy_content](#policy_content) below.

### `policy_content`

Either _String_ or _Object_, where _Object_ is preferred.

Where _Object_ - An object with `encoding`, `content_type` and `body` keys. For example:

```json
"policy_content": {
    "encoding": "none", // can be one of "none" or "base64"
    "content_type": "cedar", // ONLY "cedar" for now due to limitations in cedar-policy crate
    "body": "permit(\n    principal is Jans::User,\n    action in [Jans::Action::\"Update\"],\n    resource is Jans::Issue\n)when{\n    principal.country == resource.country\n};"
}
```

Where _String_ - The policy in cedar format, encoded as Base64. For example:

```json
"policy_content": "cGVybWl0KAogICAgc..."
```

### Example of `policies`

Here is a non-normative example of the `policies` field:

```json
  "policies": {
    "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
      "description": "simple policy example for principal workload",
      "policy_content": "cGVybWl0KAogICAgc..."
    },
    "0fo1kl928Afa0sc9123scma0123891asklajsh1233ab": {
      "description": "another policy example",
      "policy_content": "kJW1bWl0KA0g3CAxa..."
    },
    "1fo1kl928Afa0sc9123scma0123891asklajsh1233ac": {
      "description": "another policy example",
      "policy_content": {
        "encoding": "none",
        "content_type" : "cedar",
        "body": "permit(...) where {...}"
      }
    },
    "2fo1kl928Afa0sc9123scma0123891asklajsh1233ad": {
      "description": "another policy example",
      "policy_content": {
        "encoding": "base64",
        "content_type" : "cedar",
        "body": "kJW1bWl0KA0g3CAxa..."
      }
    },
    ...
  }
```

## Trusted Issuers Schema

This shape is used only by the [legacy single-file YAML format](#legacy-single-file-format-yaml-test-suites).
Directory and `.cjar` stores use [`trusted-issuers.json`](#trusted-issuersjson).
Once loaded, both follow the same rules.

This record contains the information needed to validate tokens from this issuer:

```json
"trusted_issuers": {
  "trusted_issuer_id" : {
    "name": "name_of_the_trusted_issuer",
    "description": "description for the trusted issuer",
    "openid_configuration_endpoint": "https://<trusted-issuer-hostname>/.well-known/openid-configuration",
    "token_metadata": {
      "access_tokens": {
        "trusted": true,
        "token_id": "jti",
        ...
      },
      "id_tokens": { ... },
      "userinfo_tokens": { ... },
      "tx_tokens": { ... },
      ...
    }
  },
  "another_issuer_id": {
    ...
  }
  ...
}
```

- **name** : (_String_) The issuer identity: a single Cedar identifier, used as the `issuer` of each of its token types.
- **description** : (_String_) A brief description of the trusted issuer, providing context for administrators.
- **openid_configuration_endpoint** : (_String_) The OpenID Connect discovery URL: the issuer URL followed by `/.well-known/openid-configuration`. The issuer URL, path included, is the expected `iss`.
- **trusted_issuer_id** : (_Object_, _optional_) Metadata related to a particular issuer. You can add as many trusted issuers you want. The key itself is not used.
- **token_metadata** : (_Object_, _optional_) Tokens metadata in a map of _token name_ -> _token metadata_. See [Token Metadata Schema](#token-metadata-schema).

### Token Metadata Schema

The Token Entity Metadata Schema defines how tokens are mapped and validated within Cedarling. It specifies the Cedar entity type for each token, which claim to use as the entity ID, and which claims are required.

```json
{
  "trusted": true,
  "entity_type_name": "Acme::Access_token",
  "token_id": "jti",
  "required_claims": ["iss", "exp", "some_custom_claim"]
}
```

- `"trusted"` (bool, Default: true): Allows toggling configuration without deleting the object. When set to `false`, tokens of this type will be ignored.
- `"entity_type_name"` (string, required): The type name of the Cedar Entity that will be created from the token; for example: `"Acme::Access_token"`. A request `mapping` must equal it, and it must be unique across all issuers.
- `"token_id"` (string, Default: `"jti"`): The JWT claim that will be used as the ID for the Token Entity.
- `"required_claims"` (array[string], Default: `[]`): A list of claims that must be present within the JWT to be considered valid. Additionally, if a required claim is a registered claim name under [RFC 7519 Section 4.1](https://datatracker.ietf.org/doc/html/rfc7519#section-4.1) (e.g., `exp`, `nbf`), the claim will also be validated according to the standard.

## Example Policy Store (Legacy Single-File Structure)

Here is a non-normative example of the legacy single-file structure (supported in YAML test suites):

```json
{
  "cedar_version": "v4.0.0",
  "policies": {
    "840da5d85403f35ea76519ed1a18a33989f855bf1cf8": {
      "description": "simple policy example",
      "policy_content": "cedar_policy_encoded_in_base64"
    }
  },
  "schema": "schema_encoded_in_base64",
  "trusted_issuers": {
    "08c6c18a654f492adcf3fe069d729b4d9e6bf82605cb": {
      "name": "Google",
      "description": "Consumer IDP",
      "openid_configuration_endpoint": "https://accounts.google.com/.well-known/openid-configuration",
      "token_metadata": {
        "access_token": {
          "trusted": true,
          "entity_type_name": "Jans::Access_token",
          "token_id": "jti"
        },
        "id_token": {
          "trusted": true,
          "entity_type_name": "Jans::Id_token",
          "token_id": "jti"
        },
        "userinfo_token": {
          "trusted": true,
          "entity_type_name": "Jans::Userinfo_token",
          "token_id": "jti"
        }
      }
    }
  },
  "default_entities": {
    "1694c954f8d9": "eyJ1aWQiOnsidHlwZSI6IkphbnM6Ok9yZ2FuaXphdGlvbiIsImlkIjoiMTY5NGM5NTRmOGQ5In0sImF0dHJzIjp7Im8iOiJBY21lIERvbHBoaW5zIERpdmlzaW9uIiwib3JnX2lkIjoiMTAwMTI5IiwiZG9tYWluIjoiYWNtZS1kb2xwaGluLnNlYSIsInJlZ2lvbnMiOlsiQXRsYW50aWMiLCJQYWNpZmljIiwiSW5kaWFuIl19LCJwYXJlbnRzIjpbXX0=",
    "74d109b20248": "eyJ1aWQiOnsidHlwZSI6IkphbnM6OlByaWNlTGlzdCIsImlkIjoiNzRkMTA5YjIwMjQ4In0sImF0dHJzIjp7ImRlc2NyaXB0aW9uIjoiMjAyNSBQcmljZSBMaXN0IiwicHJvZHVjdHMiOnsiMTUwMjAiOjkuOTUsIjE1MDUwIjoxNC45NX0sInNlcnZpY2VzIjp7IjUxMDAxIjo5OS4wLCI1MTAyMCI6Mjk5LjB9fSwicGFyZW50cyI6W119"
  }
}
```

## Multi-Issuer Token Entities

When using the `authorize_multi_issuer` method, Cedarling creates token entities dynamically without requiring predefined User/Workload principals. This section describes how these token entities are structured and made available in policies.

> **⚠️ SCHEMA REQUIREMENT**: Multi-issuer authorization requires specific Cedar schema modifications. Your schema **must** include the multi-issuer token structure or authorization will fail with schema validation errors. See [Schema Requirements](#schema-requirements-for-multi-issuer) below.

### Token Entity Structure

Each validated token in a multi-issuer authorization request becomes a Cedar entity with the following structure:

**Required Attributes** (stored as entity attributes):

- `token_type` (String): The entity type name (e.g., "Jans::Access_Token", "Acme::DolphinToken")
- `jti` (String): Unique token identifier from JWT
- `iss?` (TrustedIssuer): JWT issuer claim value (derived from iss to TrustedIssuer); optional when the token has not been verified or the issuer could not be determined.
- `exp` (Long): Token expiration timestamp
- `validated_at` (Long): Timestamp when Cedarling validated the token

**Optional Attributes** (JWT claims stored as entity tags):

- All other JWT claims are stored as **entity tags** with `Set<String>` type by default
- Examples: `scope`, `aud`, `sub`, `client_id`, custom claims

**Complete Example**:

```cedar
namespace Jans{
  entity Access_token = {
    "token_type": String,        // Required: Entity type name
    "jti": String,               // Required: Token ID
    "iss": Jans::TrustedIssuer,  // Required: JWT issuer
    "exp": Long,                 // Required: Expiration
    "validated_at": Long,        // Required: Validation timestamp
    // Optional JWT claims (must be optional)
    "aud"?: String,
    "iat"?: Long,
    "scope"?: Set<String>,
    // ... other claims
  } tags Set<String>;            // Required: For dynamic claim storage

  entity TrustedIssuer = {
    issuer_entity_id: Url
  };
}
```

**Important Notes**:

- All token entity attributes (except the five required ones) **must be optional** (`?`) to support tokens with varying claim structures
- JWT claims not defined as attributes are stored as **entity tags** (Set<String> by default)
- The `tags Set<String>` declaration is required for dynamic JWT claim access

### Accessing Token Claims in Policies

JWT claims are accessed using Cedar's tag syntax:

```cedar
// Check if token has a claim
context.tokens.acme_access_token.hasTag("scope")

// Get claim value (returns a Set)
context.tokens.acme_access_token.getTag("scope")

// Check if Set contains specific value
context.tokens.acme_access_token.getTag("scope").contains("read:profile")
```

**Examples of claim access**:

```cedar
// Single-valued claim (stored as single-element Set)
context.tokens.acme_access_token.hasTag("sub") &&
context.tokens.acme_access_token.getTag("sub").contains("user123")

// Multi-valued claim (stored as Set)
context.tokens.acme_access_token.hasTag("scope") &&
context.tokens.acme_access_token.getTag("scope").contains("read:profile")

// Audience claim (typically multi-valued)
context.tokens.acme_access_token.hasTag("aud") &&
context.tokens.acme_access_token.getTag("aud").contains("my-client-id")

// Custom claim
context.tokens.dolphin_dolphintoken.hasTag("waiver") &&
context.tokens.dolphin_dolphintoken.getTag("waiver").contains("signed")
```

### Token Collection in Context

Validated tokens are organized into a `tokens` collection in the Cedar context using predictable naming:

```cedar
entity Tokens = {
  // Format: {issuer_name}_{token_type}
  "acme_access_token": Token,           // Access token from Acme
  "acme_id_token": Token,               // ID token from Acme
  "google_id_token": Token,             // ID token from Google
  "dolphin_dolphintoken": Token,  // Custom token type from Dolphin
  "total_token_count": Long,            // Number of validated tokens
};
```

The naming follows this pattern:

- **Issuer name**: The `issuer` of the token's entry in [`trusted-issuers.json`](#trusted-issuersjson), lowercased
- **Token type**: The last segment of the `mapping` field, lowercased (e.g., "Jans::Access_Token" → "access_token")
- The key is computed once when the store loads and must be unique; a token type without a configured entry gets no key

### Schema Requirements for Multi-Issuer

#### 1. Token Entity Schema

All token entities **must** include these five required attributes and declare tags.

Each trusted issuer gets its own Cedar namespace. The `TrustedIssuer` entity type must be defined in each issuer's namespace separately, since Cedar namespaces are independent. Token entities reference their own namespace's `TrustedIssuer` via the `iss` attribute.

For example, if you have two issuers (Acme and Dolphin), you will have `Acme::TrustedIssuer` and `Dolphin::TrustedIssuer` — each with the same structure but in separate namespaces:

```cedar
// Jans namespace: shared infrastructure types used across namespaces.
namespace Jans {
  type Url = {
    host: String,
    path: String,
    protocol: String
  };
  type email_address = {
    domain: String,
    uid: String
  };
};

// Acme namespace: token entities for the Acme IDP.
namespace Acme {
  entity TrustedIssuer = {
    issuer_entity_id: Jans::Url
  };

  entity Access_token = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,        // Entity type (e.g., "Acme::Access_token")
    jti?: String,               // JWT ID - unique token identifier
    iss?: TrustedIssuer,        // Issuer entity reference
    exp?: Long,                 // Token expiration timestamp
    validated_at?: Long,        // Validation timestamp

    // Optional JWT claims - ALL MUST BE OPTIONAL (?)
    aud?: String,               // Audience
    iat?: Long,                 // Issued at
    scope?: Set<String>,        // OAuth scopes
    client_id?: String,         // Client ID
    sub?: String,               // Subject
    // Add other claims as needed
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***

  entity id_token = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,
    jti?: String,
    iss?: TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Optional JWT claims - ALL MUST BE OPTIONAL (?)
    aud?: Set<String>,
    iat?: Long,
    sub?: String,
    email?: Jans::email_address,
    name?: String,
    phone_number?: String,
    role?: Set<String>,
    acr?: String,
    amr?: Set<String>,
    azp?: String,
    birthdate?: String,
    // Add other claims as needed
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***

  entity Userinfo_token = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,
    jti?: String,
    iss?: TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Optional JWT claims - ALL MUST BE OPTIONAL (?)
    aud?: String,
    iat?: Long,
    sub?: String,
    email?: Jans::email_address,
    name?: String,
    birthdate?: String,
    phone_number?: String,
    role?: Set<String>,
    // Add other claims as needed
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***
};

// Dolphin namespace: token entities for the Dolphin IDP.
// Note: TrustedIssuer is defined again — each namespace needs its own copy.
namespace Dolphin {
  entity TrustedIssuer = {
    issuer_entity_id: Jans::Url
  };

  entity Access_token = {
    token_type?: String,
    jti?: String,
    iss?: TrustedIssuer,
    exp?: Long,
    validated_at?: Long,
    aud?: String,
    iat?: Long,
    scope?: Set<String>,
    // Add other claims as needed
  } tags Set<String>;
};
```

#### 2. Custom Token Types

For custom token types, follow the same pattern:

```cedar
// Jans namespace: shared infrastructure types used across namespaces.
namespace Jans {
  type Url = {
    host: String,
    path: String,
    protocol: String
  };
};

// Dolphin namespace: custom token entities for the Dolphin service.
namespace Dolphin {
  entity TrustedIssuer = {
    issuer_entity_id: Jans::Url
  };

  entity DolphinToken = {
    // *** REQUIRED ATTRIBUTES FOR MULTI-ISSUER ***
    token_type?: String,
    jti?: String,
    iss?: TrustedIssuer,
    exp?: Long,
    validated_at?: Long,

    // Custom token-specific attributes (all optional)
    waiver?: String,
    location?: String,
    clearance_level?: Long,
  } tags Set<String>;           // *** REQUIRED: For dynamic claims ***
};
```

#### 3. Context Type Schema

The Context type **must** include a `tokens` field:

```cedar
type Context = {
  // Standard context fields
  network?: String,
  network_type?: String,
  user_agent?: String,
  operating_system?: String,
  device_health?: Set<String>,
  current_time?: Long,
  geolocation?: Set<String>,
  fraud_indicators?: Set<String>,

  // *** REQUIRED FOR MULTI-ISSUER ***
  tokens?: TokensContext,
};

type TokensContext = {
  total_token_count: Long,      // Required field
  // Individual token fields added dynamically:
  // Pattern: {issuer_name}_{token_type}
  // Example: acme_access_token, google_id_token
};
```

#### Key Requirements

1. **Required Attributes**: Add `token_type`, `jti`, `iss`, `exp`, `validated_at` to all token entities
2. **Optional Modifier**: All attributes must be optional (`?`) to support varying token structures
3. **Tags Declaration**: All token entities must declare `tags Set<String>` for dynamic JWT claims
4. **Context Update**: Add `tokens?: TokensContext` field to your Context type
5. **Consistency**: Use the same attribute names across all token entity types

#### Why These Changes Are Required

- **Schema Validation**: Cedar validates all entities against the schema; missing required attributes cause authorization failures
- **Dynamic Claims**: JWT claims vary by issuer and token type; tags allow flexible claim storage
- **Context Structure**: Multi-issuer authorization places token entities in `context.tokens.*` which requires schema support
- **Compatibility**: Optional attributes ensure schema works with both standard and multi-issuer authorization

#### Schema Update Checklist

Before using multi-issuer authorization, verify your schema has:

- Added five required attributes to all token entities (token_type, jti, issuer, exp, validated_at)
- Made all token entity attributes optional (`?`)
- Added `tags Set<String>` declaration to all token entities
- Added `tokens?: TokensContext` field to Context type
- Defined `TokensContext` type with `total_token_count: Long`

### Policy Examples Using Token Entities

**Simple token validation**:

```cedar
permit(
  principal,
  action == Jans::Action::"Read",
  resource in Jans::Document
) when {
  context has tokens.acme_access_token &&
  context.tokens.acme_access_token.hasTag("scope") &&
  context.tokens.acme_access_token.getTag("scope").contains("read:documents")
};
```

**Multi-issuer validation**:

```cedar
permit(
  principal,
  action == Trade::Action::"Vote",
  resource in Trade::Election
) when {
  // Require token from trade association
  context has tokens.trade_association_access_token &&
  context.tokens.trade_association_access_token.hasTag("member_status") &&
  context.tokens.trade_association_access_token.getTag("member_status").contains("Corporate Member") &&
  // AND token from employer
  context has tokens.company_access_token &&
  context.tokens.company_access_token.hasTag("employee_id")
};
```

**Custom token type**:

```cedar
permit(
  principal,
  action == Acme::Action::"SwimWithDolphin",
  resource == Acme::Aquarium::"Miami"
) when {
  context has tokens.dolphin_dolphintoken &&
  context.tokens.dolphin_dolphintoken.hasTag("waiver") &&
  context.tokens.dolphin_dolphintoken.getTag("waiver").contains("signed") &&
  context.tokens.dolphin_dolphintoken.hasTag("clearance_level") &&
  context.tokens.dolphin_dolphintoken.getTag("clearance_level").contains(5)
};
```

### Trusted Issuer Configuration for Multi-Issuer

The `issuer` field in [`trusted-issuers.json`](#trusted-issuersjson) is critical
for multi-issuer authorization:

```json
{
  "Jans::Access_Token": {
    "type": "jwt",
    "issuer": "Acme",
    "configuration_endpoints": {
      "configuration": "https://idp.acme.com/.well-known/openid-configuration"
    }
  },
  "Acme::DolphinToken": {
    "type": "jwt",
    "issuer": "Dolphin",
    "configuration_endpoints": {
      "configuration": "https://idp.dolphin.sea/.well-known/openid-configuration"
    }
  }
}
```

Here `Jans::Access_Token` tokens appear as `context.tokens.acme_access_token` and
`Acme::DolphinToken` tokens as `context.tokens.dolphin_dolphintoken`. The
`issuer` is also the namespace of the `TrustedIssuer` Cedar type
(`Acme::TrustedIssuer`), independent of the token's own namespace.

## Policy and Schema Authoring

You can manually create your Cedar policies and schema in
[Visual Studio](https://marketplace.visualstudio.com/items?itemName=cedar-policy.vscode-cedar).
Make sure you run the cedar command line tool to validate both your schema and policies.

The easiest way to author your policy store is to use the Policy Designer in
[Agama Lab](https://cloud.gluu.org/agama-lab). This tool helps you define the policies, schema and
trusted IDPs and to publish a policy store to a Github repository.

### Minimum Cedar Schema

There are no mandatory entity types — your schema depends on which authorization interface you use:

- **`authorize_unsigned`**: Define any principal, resource, and action types your application needs. Entity types and attributes are entirely up to you.
- **`authorize_multi_issuer`**: Token entities require specific attributes and `tags Set<String>` declaration. See [Schema Requirements for Multi-Issuer](#schema-requirements-for-multi-issuer).

Here is an example of a minimal schema for `authorize_unsigned`:

```cedarschema
// Jans namespace: shared infrastructure types used across namespaces.
namespace Jans {
  type Url = {
    host: String,
    path: String,
    protocol: String
  };
};

// MyApp namespace: your business entities.
namespace MyApp {
  entity Role;
  entity User in [Role] = {
    sub: String,
    email?: String,
  };

  entity Document = {
    owner: String,
  };

  action "Read" appliesTo {
    principal: [User, Role],
    resource: [Document],
    context: {}
  };
};
```

For `authorize_multi_issuer`, add token entities with the required structure. See the [Multi-Issuer Token Entities](#multi-issuer-token-entities) section for a complete example.

## Note on test fixtures

You will notice that test fixtures in the cedarling code base are quite often in yaml rather than in json.

yaml is intended for **cedarling internal use only**.

The rationale is that yaml has excellent support for embedded, indented, multiline string values. That is far easier to read than base64 encoded json strings, and is beneficial for debugging and validation that test cases are correct.
