# Cedarling WebAssembly for JavaScript

`@janssenproject/cedarling_wasm` runs [Cedarling](https://docs.jans.io/stable/cedarling/)
authorization in JavaScript and TypeScript. It exposes the Cedarling WebAssembly
API with automatic WebAssembly initialization for browsers and Node.js, one
packaged WebAssembly asset, CommonJS support, and an edge-specific entry.

Use it to evaluate Cedar policies locally, retain decision logs, manage
context data, and validate token-based authorization requests.

## Install

```sh
npm install @janssenproject/cedarling_wasm
```

Node.js consumers require Node 22, 24, or 26. Browser and edge consumers need
an ESM-aware build or deployment tool.

## Choose an integration

| Environment | Import | WebAssembly setup |
| --- | --- | --- |
| Browser applications and supported bundlers | `@janssenproject/cedarling_wasm` | Automatic |
| Node.js ESM | `@janssenproject/cedarling_wasm` | Automatic |
| Node.js CommonJS | `require("@janssenproject/cedarling_wasm")` | Automatic |
| Cloudflare Workers and Vercel Edge | `@janssenproject/cedarling_wasm/edge` | Automatic static module |
| Other ESM bundlers | `@janssenproject/cedarling_wasm/manual` and `/wasm` | Application emits the asset |

The root export selects a browser, Node ESM, or Node CommonJS build. It is
qualified with esbuild, Vite, webpack, and packaged Node ESM/CommonJS consumers, so
most applications should use it directly. Do not import files below `dist/`:
only the package root, `./edge`, `./manual`, and `./wasm` are public entry
points.

## Quick start: unsigned authorization

`init` accepts an object or `Map` containing the canonical uppercase Cedarling
bootstrap properties. The policy store property is a **serialized JSON string**;
the request passed to an authorization method is also a JSON string.

```ts
import { init } from "@janssenproject/cedarling_wasm";

const policyStore = {
  // A valid Cedarling policy-store document.
  // Replace this placeholder with your application policy store.
};

const cedarling = await init({
  CEDARLING_APPLICATION_NAME: "task-api",
  CEDARLING_POLICY_STORE_LOCAL: JSON.stringify(policyStore),
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

`decision: false` is a valid policy decision, not an exception. Invalid
configuration, malformed requests, failed policy loading, or failed token
validation reject their promise and should be handled with `try`/`catch`.

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

| Property | Purpose |
| --- | --- |
| `CEDARLING_APPLICATION_NAME` | Required application identifier. |
| `CEDARLING_POLICY_STORE_LOCAL` | One serialized JSON policy-store document. Exactly one policy-store source must be selected. |
| `CEDARLING_POLICY_STORE_URI` | URL of a JSON policy-store document. |
| `CEDARLING_POLICY_STORE_CJAR_URL` | URL of a Cedar archive policy store. |
| `CEDARLING_LOG_TYPE` | Log destination, such as `memory` or `std_out`. |
| `CEDARLING_LOG_TTL` | Required retention period in seconds when using `memory` logging. |
| `CEDARLING_LOG_MAX_ITEMS` / `CEDARLING_LOG_MAX_ITEM_SIZE` | Optional retained-log limits; `0` means no limit. |
| `CEDARLING_JWT_SIG_VALIDATION` | Enables or disables JWT signature validation. |
| `CEDARLING_JWT_STATUS_VALIDATION` | Enables or disables JWT status-list validation. |
| `CEDARLING_STRICT_SCHEMA_VALIDATION` | Enables or disables Cedar schema validation. |
| `CEDARLING_DATA_STORE_MAX_ENTRIES` / `CEDARLING_DATA_STORE_MAX_ENTRY_SIZE` | Optional context-data capacity limits; `0` means no limit. |
| `CEDARLING_DATA_STORE_DEFAULT_TTL` | Default context-data lifetime in seconds. |
| `CEDARLING_DATA_STORE_MAX_TTL` | Maximum permitted context-data lifetime in seconds. |
| `CEDARLING_TRUSTED_ISSUER_LOADER_TYPE` / `CEDARLING_TRUSTED_ISSUER_LOADER_WORKERS` | Controls trusted-issuer loading. |

The raw configuration accepts JSON-compatible values. Numeric and boolean
properties may be passed as their JSON values or as strings accepted by the
core parser. Do not pass more than one policy-store source property.

</details>

### Load a Cedar archive with application-controlled fetch

Use `initFromArchiveBytes` when the application needs to fetch a `.cjar` file
itself, for example to attach authorization headers. Do not set a policy-store
source property when passing archive bytes.

```ts
import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";

const response = await fetch("/api/policy-store", {
  headers: { Authorization: `Bearer ${policyStoreToken}` },
});
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

| Export | Input | Result | Use |
| --- | --- | --- | --- |
| `init(config)` | Bootstrap-property `object` or `Map` | `Promise<Cedarling>` | Standard automatic initialization. |
| `initFromArchiveBytes(config, bytes)` | Bootstrap properties and `Uint8Array` `.cjar` bytes | `Promise<Cedarling>` | Application-controlled archive download. |
| default export / `initWasm(input?)` | Optional wasm-bindgen initialization input | `Promise<InitOutput>` | Automatic with no input; explicit generated initialization when an application owns the module. |
| `initSync(input)` | `WebAssembly.Module` or `BufferSource` | `InitOutput` | Synchronous generated initialization. |
| `Cedarling.new(config)` | Bootstrap-property object | `Promise<Cedarling>` | Generated constructor after WebAssembly has been initialized. |
| `Cedarling.newFromMap(config)` | Bootstrap-property `Map` | `Promise<Cedarling>` | Generated map constructor after WebAssembly has been initialized. |

`init` and `initFromArchiveBytes` are the normal application APIs: each first
ensures the packaged WebAssembly module is initialized. `initWasm` and
`initSync` remain useful for applications that already manage their own
WebAssembly loading.

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
  CEDARLING_POLICY_STORE_LOCAL: JSON.stringify(policyStore),
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

| Type | Fields and methods |
| --- | --- |
| `AuthorizeResult` / `MultiIssuerAuthorizeResult` | Mutable `decision`, `request_id`, `response`, and `json_string()`. |
| `AuthorizeResultResponse` | Read-only `decision` and `diagnostics`. |
| `Diagnostics` | Read-only `reason` policy IDs and unordered `errors`. |
| `PolicyEvaluationError` | Read-only policy `id` and diagnostic `error`. |
| `BatchAuthorizeUnsignedResponse` / `BatchAuthorizeMultiIssuerResponse` | Read-only `batch_id` and ordered `results`. |
| `BatchItemUnsignedResult` / `BatchItemMultiIssuerResult` | Read-only `is_ok`, optional `error`, and `unwrap()` for a successful item. |
| `BatchItemError` | Read-only `category`, `item_index`, and safe-to-log `message`. |

## Context data

Context data is application-scoped data available to Cedar evaluation.

| Method | Behavior |
| --- | --- |
| `pushDataCtx(key, value, ttl_secs?)` | Stores a JSON-compatible value. `ttl_secs` is a `bigint`, for example `3600n`; omit it to use the configured default. |
| `getDataCtx(key)` | Returns the stored JavaScript value or `null` if absent or expired. |
| `getDataEntryCtx(key)` | Returns a `DataEntry` wrapper or `undefined`. |
| `listDataCtx()` | Returns all `DataEntry` wrappers. |
| `removeDataCtx(key)` | Removes one value and returns whether it existed. |
| `clearDataCtx()` | Removes every stored value. |
| `getStatsCtx()` | Returns a `DataStoreStats` wrapper. |

```ts
cedarling.pushDataCtx("user:alice", { plan: "pro" }, 300n);
const value = cedarling.getDataCtx("user:alice");
const entry = cedarling.getDataEntryCtx("user:alice");
const stats = cedarling.getStatsCtx();
```

`DataEntry` exposes `key`, `value()`, `data_type`, `created_at`, optional
`expires_at`, `access_count`, and `json_string()`. `DataStoreStats` exposes
entry counts, configured limits, size metrics, memory-alert fields, and
`json_string()`; its numeric field names remain generated snake_case names.

## Retained logs

Use `CEDARLING_LOG_TYPE: "memory"` and `CEDARLING_LOG_TTL` to retain logs.
Log records are returned as plain JavaScript objects.

| Method | Behavior |
| --- | --- |
| `popLogs()` | Returns every retained log and removes it from storage. |
| `getLogById(id)` | Returns one log or `null`. |
| `getLogIds()` | Returns every log ID. |
| `getLogsByTag(tag)` | Returns logs tagged by `log_kind` or `log_level`. |
| `getLogsByRequestId(request_id)` | Returns logs for one request correlation ID. |
| `getLogsByRequestIdAndTag(request_id, tag)` | Returns logs matching both values. |

Call a read method when logs must remain available for later inspection; call
`popLogs()` only when the application is ready to consume and clear them.

## Trusted issuer readiness

These methods report the state of trusted issuer initialization for
token-based authorization.

| Method | Result |
| --- | --- |
| `isTrustedIssuerLoadedByName(issuer_id)` | Whether a configured issuer ID loaded. |
| `isTrustedIssuerLoadedByIss(iss_claim)` | Whether an issuer loaded for an `iss` value. |
| `totalIssuers()` | Number of trusted issuer entries discovered. |
| `loadedTrustedIssuersCount()` | Number loaded successfully. |
| `loadedTrustedIssuerIds()` | Loaded issuer IDs. |
| `failedTrustedIssuerIds()` | Issuer IDs that failed to load. |

## Policy annotations

Use the policy IDs in `result.response.diagnostics.reason` to inspect Cedar
policy annotations.

| Method | Result |
| --- | --- |
| `annotationsMap(policy_ids)` | One merged object. Duplicate keys are lossy. |
| `annotationValues(policy_ids, key)` | Every value of an annotation key, preserving duplicates. |
| `annotationsByPolicy(policy_ids)` | Annotation objects grouped by policy ID. |

## Lifecycle and generated resources

Call `await cedarling.shutDown()` during orderly shutdown so Cedarling can
close Lock Server connections and deliver available logs. The generated
wrappers (`Cedarling`, authorization results, diagnostics, data entries, and
stats) provide `free()` and `[Symbol.dispose]()` for deterministic WebAssembly
resource release. wasm-bindgen also registers finalizers, so ordinary
short-lived application code normally does not need to call `free()`.

If an application deliberately calls `free()`, it must not read or reuse that
wrapper afterwards. `shutDown()` and `free()` have different responsibilities:
the former closes Cedarling services; the latter releases a JavaScript wrapper.

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

The generated declarations intentionally preserve generated data names such as
`request_id`, `json_string()`, and `access_count`. `ReadableStreamType` and the
`IntoUnderlying*` types are wasm-bindgen stream adapters: their constructors are
private and no Cedarling client method returns them, so applications do not use
them directly. Editor hover documentation is the most complete reference for
the generated result and wrapper types.

## Maintainers

For package generation, test commands, and release-artifact qualification, see
the [maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/js/docs/readme.md).
