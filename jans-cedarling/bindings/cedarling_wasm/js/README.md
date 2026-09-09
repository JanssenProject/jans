# Cedarling for JavaScript

The Cedarling JavaScript package, `@janssenproject/cedarling_wasm`, embeds
[Cedarling](https://docs.jans.io/stable/cedarling/) in JavaScript and TypeScript
applications. Cedarling evaluates authorization policies within the application's
process, whether it runs in a browser, on a server, or at the edge. It answers
whether a principal or token set may perform an action on a resource.

Use it when an application needs consistent authorization decisions, policy
diagnostics, retained decision logs, context data, or token-based policy
evaluation in its browser, server, or edge runtime.

The package supports browser applications, Node.js ESM, Node.js CommonJS, and
supported edge deployments. Normal integrations initialize WebAssembly
automatically and keep the packaged WebAssembly asset internal.

## Install

```sh
npm install @janssenproject/cedarling_wasm
```

Node.js consumers require Node 22, 24, or 26. Browser and edge consumers need
an ESM-aware build or deployment tool.

## Build from source

For local generation, package build, and qualification instructions, see the
[maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/js/docs/maintainer.md).

## Choose an integration

| Environment                                 | Import                                              | WebAssembly setup           |
| ------------------------------------------- | --------------------------------------------------- | --------------------------- |
| Browser applications and supported bundlers | `@janssenproject/cedarling_wasm`                    | Automatic                   |
| Node.js ESM                                 | `@janssenproject/cedarling_wasm`                    | Automatic                   |
| Node.js CommonJS                            | `require("@janssenproject/cedarling_wasm")`         | Automatic                   |
| Cloudflare Workers and Vercel Edge          | `@janssenproject/cedarling_wasm/edge`               | Automatic static module     |
| Other ESM bundlers                          | `@janssenproject/cedarling_wasm/manual` and `/wasm` | Application emits the asset |

The root export selects a browser, Node ESM, or Node CommonJS build. It is
qualified as an installed browser bundle with esbuild and as installed Node
ESM/CommonJS consumers, so most applications should use it directly. Do not
import files below `dist/`: only the package root, `./edge`, `./manual`, and
`./wasm` are public entry points.

## Key concepts

<details>
<summary>Authorization terms used in this guide</summary>

| Term                                     | Meaning                                                                                                                                                                                                                        |
| ---------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Principal                                | The entity requesting access, such as a user or service.                                                                                                                                                                       |
| Action                                   | The operation being requested, such as reading a document.                                                                                                                                                                     |
| Resource                                 | The entity the action targets, such as a document.                                                                                                                                                                             |
| Request context                          | Additional facts supplied with an authorization request, such as a tenant or network address.                                                                                                                                  |
| [Policy annotation](#policy-annotations) | Key-value metadata attached to a policy for applications to read; it does not affect Cedar's authorization decision.                                                                                                           |
| Context data                             | Reusable values stored on a Cedarling instance through its data API and made available to policies under `context.data`.                                                                                                       |
| Policy store                             | Cedar policies and related configuration, such as a schema, trusted issuers, and default entities. A `.cjar` file packages a policy store as a Cedar archive.                                                                  |
| Trusted issuer                           | A token issuer configured in the policy store, with metadata for token validation and mapping claims into Cedar entities. Trusting an issuer does not itself grant access: policies still decide.                              |
| Unsigned authorization                   | Policy evaluation using application-supplied entity data, without JWT verification. The application is responsible for establishing trust in that input.                                                                       |
| Multi-issuer authorization               | Policy evaluation using one or more tokens, potentially from different issuers, with validation governed by configuration. Token entities are available through `context.tokens`; this method does not use a principal entity. |

References: [Cedar's principal, action, resource, and context model](https://docs.cedarpolicy.com/auth/authorization.html),
[Cedarling authorization methods](https://docs.jans.io/stable/cedarling/reference/cedarling-authz/),
[policy stores and trusted issuers](https://docs.jans.io/stable/cedarling/reference/cedarling-policy-store/),
and the [Context Data API](https://docs.jans.io/stable/cedarling/reference/cedarling-interfaces/#context-data-api).

</details>

## Quick start: unsigned authorization

To create a Cedarling instance, call `init()` with an object or `Map` containing
canonical uppercase Cedarling bootstrap properties. Use a Cedar archive URL for
the policy store. The request passed to an authorization method is a JSON string.

Replace the placeholder archive URL below with a real policy store whose schema
defines `Task::User`, `Task::Document`, and `Task::Action::"Read"`, including the
attributes and context used in the request, and whose policies govern that action.

```ts
import { init } from "@janssenproject/cedarling_wasm";

const cedarling = await init({
  CEDARLING_APPLICATION_NAME: "task-api",
  CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar",
  CEDARLING_LOG_TYPE: "memory",
  CEDARLING_LOG_TTL: 120,
  CEDARLING_JWT_SIG_VALIDATION: "disabled",
  CEDARLING_JWT_STATUS_VALIDATION: "disabled",
});

try {
  const result = await cedarling.authorizeUnsigned(JSON.stringify({
    principal: {
      cedar_entity_mapping: { entity_type: "Task::User", id: "alice" },
      role: "member",
    },
    action: 'Task::Action::"Read"',
    resource: {
      cedar_entity_mapping: { entity_type: "Task::Document", id: "document-1" },
      owner: "alice",
    },
    context: { tenant: "example" },
  }));

  if (result.decision) {
    console.log("Allowed", result.request_id);
  } else {
    console.log("Denied", result.request_id);
  }
} finally {
  await cedarling.shutDown();
}
```

A denied authorization request returns `decision: false` rather than throwing
solely because access was denied. Invalid configuration, malformed requests,
failed policy loading, or failed token validation reject their promise and
should be handled with `try`/`catch`.

In a browser, a cross-origin CJar server must allow the application origin with
CORS response headers. Use a same-origin CJar URL or the application-controlled
archive path below when that is not possible.

The disabled JWT checks in this example are appropriate only for an unsigned
local demonstration. Production token authorization must use the validation
configuration and trusted-issuer settings required by its security model.

## Configuration

The package passes raw bootstrap properties to Cedarling without a
JavaScript-specific configuration layer. Refer to the
[Cedarling property reference](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
for the full, authoritative property contract.

<details>
<summary>Common bootstrap properties</summary>

| Property                                                                           | Purpose                                                           |
| ---------------------------------------------------------------------------------- | ----------------------------------------------------------------- |
| `CEDARLING_APPLICATION_NAME`                                                       | Required application identifier.                                  |
| `CEDARLING_POLICY_STORE_URI`                                                       | Policy-store URL; detects Cedar archive or legacy JSON content.   |
| `CEDARLING_LOG_TYPE`                                                               | Log destination, such as `memory` or `std_out`.                   |
| `CEDARLING_LOG_TTL`                                                                | Required retention period in seconds when using `memory` logging. |
| `CEDARLING_LOG_MAX_ITEMS` / `CEDARLING_LOG_MAX_ITEM_SIZE`                          | Optional retained-log limits; `0` means no limit.                 |
| `CEDARLING_JWT_SIG_VALIDATION`                                                     | Enables or disables JWT signature validation.                     |
| `CEDARLING_JWT_STATUS_VALIDATION`                                                  | Enables or disables JWT status-list validation.                   |
| `CEDARLING_STRICT_SCHEMA_VALIDATION`                                               | Enables or disables Cedar schema validation.                      |
| `CEDARLING_DATA_STORE_MAX_ENTRIES` / `CEDARLING_DATA_STORE_MAX_ENTRY_SIZE`         | Optional context-data capacity limits; `0` means no limit.        |
| `CEDARLING_DATA_STORE_DEFAULT_TTL`                                                 | Default context-data lifetime in seconds.                         |
| `CEDARLING_DATA_STORE_MAX_TTL`                                                     | Maximum permitted context-data lifetime in seconds.               |
| `CEDARLING_TRUSTED_ISSUER_LOADER_TYPE` / `CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS` | Controls trusted-issuer loading.                                  |

The raw configuration accepts JSON-compatible values. Numeric and boolean
properties may be passed as their JSON values or as strings accepted by the
core parser. Do not pass more than one policy-store source property.

</details>

### Load a Cedar archive with application-controlled fetch

Use `initFromArchiveBytes` when the application needs to fetch a `.cjar` file
itself. A same-origin application endpoint can keep any upstream authorization
headers and credentials on the server. Do not set a policy-store source property
when passing archive bytes.

```ts
import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";

const response = await fetch("/api/policy-store");
if (!response.ok) throw new Error("Policy-store download failed");

const cedarling = await initFromArchiveBytes(
  {
    CEDARLING_APPLICATION_NAME: "task-api",
    CEDARLING_LOG_TYPE: "memory",
    CEDARLING_LOG_TTL: 120,
  },
  new Uint8Array(await response.arrayBuffer()),
);
```

Keep policy-store credentials and other secrets on a trusted server. A browser
bundle exposes its configuration and any data included in it to the browser
user.

## Initialization API

| Export                                | Input                                                                | Result                | Use                                                                                                  |
| ------------------------------------- | -------------------------------------------------------------------- | --------------------- | ---------------------------------------------------------------------------------------------------- |
| `init(config)`                        | Bootstrap-property `object` or `Map`                                 | `Promise<Cedarling>`  | Standard automatic initialization.                                                                   |
| `initFromArchiveBytes(config, bytes)` | Bootstrap-property `object` or `Map`, and `Uint8Array` `.cjar` bytes | `Promise<Cedarling>`  | Application-controlled archive download.                                                             |
| default export / `initWasm(input?)`   | Optional wasm-bindgen initialization input                           | `Promise<InitOutput>` | Automatic with no input; explicit generated initialization when an application owns the module.      |
| `initSync(input)`                     | `WebAssembly.Module` or `BufferSource`                               | `InitOutput`          | Synchronous generated initialization.                                                                |
| `Cedarling.new(config)`               | Bootstrap-property object                                            | `Promise<Cedarling>`  | Generated constructor from the ESM-only `./manual` entry after WebAssembly has been initialized.     |
| `Cedarling.newFromMap(config)`        | Bootstrap-property `Map`                                             | `Promise<Cedarling>`  | Generated map constructor from the ESM-only `./manual` entry after WebAssembly has been initialized. |

For automatic initialization, use `init()` or `initFromArchiveBytes()`: each
first ensures the packaged WebAssembly module is initialized. Applications that
manage their own WebAssembly loading can use `initWasm` and `initSync`.

The root and `./edge` entries expose generated classes as TypeScript types only.
Import `Cedarling` as a runtime value from `./manual` when using its generated
static constructors directly.

### Node.js CommonJS

```js
const { init } = require("@janssenproject/cedarling_wasm");

async function start(config) {
  return init(config);
}
```

### Edge deployments

Use the ESM-only `./edge` entry in a runtime that statically imports
WebAssembly as part of its deployment bundle.

```ts
import { init } from "@janssenproject/cedarling_wasm/edge";

const cedarling = await init({
  CEDARLING_APPLICATION_NAME: "edge-api",
  CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar",
  CEDARLING_LOG_TYPE: "memory",
  CEDARLING_LOG_TTL: 120,
});
```

The edge entry accepts no caller-supplied `initWasm` input; it obtains the
static module from the host runtime.

### Manual bundler integration

Use `./manual` and `./wasm` only when a bundler cannot use the root export's
automatic asset handling. Configure that bundler to emit `./wasm` as one
URL-addressable binary asset, then initialize the generated glue once with
that URL.

```ts
import initWasm, {
  initFromArchiveBytes,
} from "@janssenproject/cedarling_wasm/manual";
import wasmUrl from "@janssenproject/cedarling_wasm/wasm";

let initialization: Promise<unknown> | undefined;

function ensureWasm() {
  initialization ??= initWasm(wasmUrl);
  return initialization;
}

await ensureWasm();
const cedarling = await initFromArchiveBytes(config, policyArchiveBytes);
```

The manual and WASM entries are ESM-only. Do not inline the binary as base64:
emit the single package-provided `.wasm` file as an asset. The package does not
support deep imports of its build output.

## Authorization

Every authorization method receives a JSON string. Build a normal JavaScript
object, validate application-specific fields before the call, then use
`JSON.stringify` at the generated binding boundary.

<details>
<summary>Unsigned authorization</summary>

```ts
const result = await cedarling.authorizeUnsigned(JSON.stringify({
  principal: {
    cedar_entity_mapping: { entity_type: "Task::User", id: "alice" },
    department: "engineering",
  },
  action: 'Task::Action::"Read"',
  resource: {
    cedar_entity_mapping: { entity_type: "Task::Document", id: "document-1" },
    classification: "internal",
  },
  context: { ip: "203.0.113.10" },
}));

console.log(result.decision, result.request_id);
console.log(result.response.diagnostics.reason);
```

`principal` is optional. When omitted or `null`, Cedarling uses partial
evaluation; an unresolved principal-dependent request fails closed as a deny.
`resource` has the same entity shape as `principal`, and additional object
properties become Cedar entity attributes.

</details>

<details>
<summary>Multi-issuer token authorization</summary>

```ts
const result = await cedarling.authorizeMultiIssuer(JSON.stringify({
  tokens: [
    { mapping: "Jans::Access_Token", payload: accessToken },
  ],
  action: 'Task::Action::"Read"',
  resource: {
    cedar_entity_mapping: { entity_type: "Task::Document", id: "document-1" },
  },
  context: { tenant: "example" },
}));
```

Each token has a configured Cedar entity `mapping` and its JWT `payload`.
`tokens` must not be empty. `context` is optional but, if provided, must be an
object.

</details>

<details>
<summary>Batch authorization</summary>

`authorizeUnsignedBatch` evaluates one optional `principal` against multiple
items. `authorizeMultiIssuerBatch` validates one `tokens` set and evaluates it
against multiple items. Each item has `resource`, `action`, and an optional
object `context`.

```ts
const batch = await cedarling.authorizeUnsignedBatch(JSON.stringify({
  principal: {
    cedar_entity_mapping: { entity_type: "Task::User", id: "alice" },
  },
  items: [
    {
      action: 'Task::Action::"Read"',
      resource: {
        cedar_entity_mapping: { entity_type: "Task::Document", id: "document-1" },
      },
      context: {},
    },
  ],
}));

for (const item of batch.results) {
  if (item.is_ok) {
    console.log(item.unwrap().decision);
  } else {
    console.error(item.error?.category, item.error?.message);
  }
}
```

`batch.results[i]` corresponds to `items[i]`. A per-item build failure has
`is_ok === false` and `error`; a valid Cedar denial has `is_ok === true` and
`item.unwrap().decision === false`. `batch_id` correlates decision-log entries
created by the batch.

</details>

### Authorization results

| Type                                                                   | Fields and methods                                                         |
| ---------------------------------------------------------------------- | -------------------------------------------------------------------------- |
| `AuthorizeResult` / `MultiIssuerAuthorizeResult`                       | Mutable `decision`, `request_id`, `response`, and `jsonString()`.          |
| `AuthorizeResultResponse`                                              | Read-only `decision` and `diagnostics`.                                    |
| `Diagnostics`                                                          | Read-only `reason` policy IDs and unordered `errors`.                      |
| `PolicyEvaluationError`                                                | Read-only policy `id` and diagnostic `error`.                              |
| `BatchAuthorizeUnsignedResponse` / `BatchAuthorizeMultiIssuerResponse` | Read-only `batch_id` and ordered `results`.                                |
| `BatchItemUnsignedResult` / `BatchItemMultiIssuerResult`               | Read-only `is_ok`, optional `error`, and `unwrap()` for a successful item. |
| `BatchItemError`                                                       | Read-only `category`, `item_index`, and safe-to-log `message`.             |

## Context data

Context data is application-scoped data available to Cedar evaluation.

| Method                               | Behavior                                                                                                              |
| ------------------------------------ | --------------------------------------------------------------------------------------------------------------------- |
| `pushDataCtx(key, value, ttl_secs?)` | Stores a JSON-compatible value. `ttl_secs` is a `bigint`, for example `3600n`; omit it to use the configured default. |
| `getDataCtx(key)`                    | Returns the stored JavaScript value or `null` if absent or expired.                                                   |
| `getDataEntryCtx(key)`               | Returns a `DataEntry` wrapper or `undefined`.                                                                         |
| `listDataCtx()`                      | Returns all `DataEntry` wrappers.                                                                                     |
| `removeDataCtx(key)`                 | Removes one value and returns whether it existed.                                                                     |
| `clearDataCtx()`                     | Removes every stored value.                                                                                           |
| `getStatsCtx()`                      | Returns a `DataStoreStats` wrapper.                                                                                   |

```ts
cedarling.pushDataCtx("user:alice", { plan: "pro" }, 300n);
const value = cedarling.getDataCtx("user:alice");
const entry = cedarling.getDataEntryCtx("user:alice");
const stats = cedarling.getStatsCtx();
```

`DataEntry` exposes `key`, `value()`, `data_type`, `created_at`, optional
`expires_at`, `access_count`, and `jsonString()`. `DataStoreStats` exposes
entry counts, configured limits, size metrics, memory-alert fields, and
`jsonString()`; its numeric field names remain generated snake_case names.

## Retained logs

Use `CEDARLING_LOG_TYPE: "memory"` and `CEDARLING_LOG_TTL` to retain logs.
Log records are returned as plain JavaScript objects.

| Method                                      | Behavior                                                |
| ------------------------------------------- | ------------------------------------------------------- |
| `popLogs()`                                 | Returns every retained log and removes it from storage. |
| `getLogById(id)`                            | Returns one log or `null`.                              |
| `getLogIds()`                               | Returns every log ID.                                   |
| `getLogsByTag(tag)`                         | Returns logs tagged by `log_kind` or `log_level`.       |
| `getLogsByRequestId(request_id)`            | Returns logs for one request correlation ID.            |
| `getLogsByRequestIdAndTag(request_id, tag)` | Returns logs matching both values.                      |

Call a read method when logs must remain available for later inspection; call
`popLogs()` only when the application is ready to consume and clear them.

## Trusted issuer readiness

These methods report the state of trusted issuer initialization for
token-based authorization.

| Method                                   | Result                                       |
| ---------------------------------------- | -------------------------------------------- |
| `isTrustedIssuerLoadedByName(issuer_id)` | Whether a configured issuer ID loaded.       |
| `isTrustedIssuerLoadedByIss(iss_claim)`  | Whether an issuer loaded for an `iss` value. |
| `totalIssuers()`                         | Number of trusted issuer entries discovered. |
| `loadedTrustedIssuersCount()`            | Number loaded successfully.                  |
| `loadedTrustedIssuerIds()`               | Loaded issuer IDs.                           |
| `failedTrustedIssuerIds()`               | Issuer IDs that failed to load.              |

## Policy annotations

Annotations attach metadata to a Cedar policy using `@key("value")`. They can
provide descriptions or other information for your application; they do not
change whether Cedar allows or denies a request. See the
[Cedar annotation reference](https://docs.cedarpolicy.com/policies/syntax-policy.html#annotations).

For example, a policy in your policy store can include a description:

```cedar
@description("Allows Alice to read document-1")
permit (
    principal == Task::User::"alice",
    action == Task::Action::"Read",
    resource == Task::Document::"document-1"
);
```

After an unsigned authorization call, use `result.response.diagnostics.reason`
(the determining policy IDs) to retrieve their descriptions:

```ts
const descriptions = cedarling.annotationValues(
  result.response.diagnostics.reason,
  "description",
);
console.log(descriptions); // e.g. ["Allows Alice to read document-1"]
```

| Method                              | Result                                                   |
| ----------------------------------- | -------------------------------------------------------- |
| `annotationsMap(policy_ids)`        | One merged object. Duplicate keys are lossy.             |
| `annotationValues(policy_ids, key)` | Every value of an annotation key, preserving duplicates. |
| `annotationsByPolicy(policy_ids)`   | Annotation objects grouped by policy ID.                 |

## Shutdown

When the Cedarling instance is no longer needed, shut it down:

```ts
await cedarling.shutDown();
```

## TypeScript

The package publishes generated declarations for ESM, CommonJS, edge, and
manual imports. Import types from the same public path used at runtime:

```ts
import type {
  AuthorizeResult,
  Cedarling,
  DataEntry,
  DataStoreStats,
  InitInput,
  InitOutput,
} from "@janssenproject/cedarling_wasm";
```

The generated declarations preserve canonical data names such as `request_id`
and `access_count`; callable helpers use camelCase, including `jsonString()`.
`ReadableStreamType` and the `IntoUnderlying*` types are wasm-bindgen stream
adapters: their constructors are private and no Cedarling client method returns
them, so applications do not use them directly. Editor hover documentation is
the most complete reference for generated result and wrapper types.
