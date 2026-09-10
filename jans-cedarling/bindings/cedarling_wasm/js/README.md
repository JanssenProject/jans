# Cedarling for JavaScript

The Cedarling JavaScript package, `@janssenproject/cedarling_wasm`, embeds
[Cedarling](https://docs.jans.io/stable/cedarling/) in JavaScript and TypeScript
applications. Cedarling evaluates authorization policies within the application's
process, whether it runs in a browser, on a server, or at the edge. It answers
whether a principal or token set may perform an action on a resource.

Use it when an application needs consistent authorization decisions, policy
diagnostics, retained decision logs, context data, or token-based policy
evaluation.

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

For browser or Node.js ESM applications:

```ts
import { init } from "@janssenproject/cedarling_wasm";
```

For Node.js CommonJS applications:

```js
const { init } = require("@janssenproject/cedarling_wasm");
```

For other integrations, follow [Edge deployments](#edge-deployments) or
[Manual bundler integration](#manual-bundler-integration).

## Quick start: unsigned authorization

To create a Cedarling instance, call `init()` with an object or `Map` containing
canonical uppercase Cedarling bootstrap properties. Use a Cedar archive URL for
the policy store. The request passed to an authorization method is a JSON string.

Replace the placeholder archive URL below with a real policy store whose schema
defines `Task::User`, `Task::Document`, and `Task::Action::"Read"`, including the
attributes and context used in the request, and whose policies govern that action.

For policy and schema authoring, use the
[Agama Lab Policy Designer](https://cloud.gluu.org/agama-lab/dashboard/policy-designer).
For packaging your policy store, see the
[Cedar Archive (.cjar) format](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#cedar-archive-cjar-format).

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

Read `decision` to distinguish allowed (`true`) and denied (`false`) requests.
Handle rejected promises from invalid configuration, malformed requests, failed
policy loading, or failed token validation with `try`/`catch`.

For browser policy downloads, use a same-origin CJar URL or configure the
cross-origin server's CORS response headers to allow the application origin.

Use the disabled JWT checks in this example for unsigned local demonstrations
only. Configure production token authorization with the validation and
trusted-issuer settings required by the application's security model.

## Configuration

The package passes canonical bootstrap properties directly to Cedarling. Refer to the
[Cedarling property reference](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
for the full, authoritative property contract.

### Load a Cedar archive with application-controlled fetch

Use `initFromArchiveBytes` when the application needs to fetch a `.cjar` file
itself. A same-origin application endpoint can keep any upstream authorization
headers and credentials on the server. Supply the policy store through the
archive bytes and use `config` for the remaining bootstrap properties.

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
| `initSync({ module })`                | `module`: `WebAssembly.Module` or `BufferSource`                     | `InitOutput`          | Synchronous initialization; edge requires a precompiled module.                                      |
| `Cedarling.new(config)`               | Bootstrap-property object                                            | `Promise<Cedarling>`  | Generated constructor from the ESM-only `./manual` entry after WebAssembly has been initialized.     |
| `Cedarling.newFromMap(config)`        | Bootstrap-property `Map`                                             | `Promise<Cedarling>`  | Generated map constructor from the ESM-only `./manual` entry after WebAssembly has been initialized. |

The root and `./edge` entries expose generated classes as TypeScript types only.
Import `Cedarling` as a runtime value from `./manual` when using its generated
static constructors directly.

### Edge deployments

Use the ESM-only `./edge` entry in a runtime that statically imports
WebAssembly as part of its deployment bundle. In Cloudflare Workers, initialize
inside the request handler so policy downloads run in its
[request context](https://developers.cloudflare.com/workers/runtime-apis/request/#the-request-context):

```ts
import { init } from "@janssenproject/cedarling_wasm/edge";

export default {
  async fetch() {
    const cedarling = await init({
      CEDARLING_APPLICATION_NAME: "edge-api",
      CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar",
      CEDARLING_LOG_TYPE: "memory",
      CEDARLING_LOG_TTL: 120,
    });
    try {
      return new Response("Cedarling initialized");
    } finally {
      await cedarling.shutDown();
    }
  },
};
```

For explicit edge initialization, call `initWasm()` to use the host-provided
module, or pass a precompiled `WebAssembly.Module` to `initSync({ module })`.

### Manual bundler integration

For application-controlled WebAssembly loading, use the ESM-only `./manual`
and `./wasm` entries. Configure the bundler to emit `./wasm` as one
URL-addressable binary asset, then initialize the module with that URL before
creating a Cedarling instance:

```ts
import initWasm, {
  initFromArchiveBytes,
} from "@janssenproject/cedarling_wasm/manual";
import wasmUrl from "@janssenproject/cedarling_wasm/wasm";

await initWasm({ module_or_path: wasmUrl });
const cedarling = await initFromArchiveBytes(config, policyArchiveBytes);
```

## Authorization

Every authorization method receives a JSON string. Build a normal JavaScript
object, validate application-specific fields before the call, then use
`JSON.stringify` at the generated binding boundary.

<details>
<summary>Unsigned authorization</summary>

See the [quick start](#quick-start-unsigned-authorization) for a complete request.
Inspect an authorization result's determining policy IDs with:

```ts
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

Provide at least one token, each with a configured Cedar entity `mapping` and
its JWT `payload`. Use an object for the optional `context`.

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
| `BatchItemError`                                                       | Read-only `category`, `item_index`, and diagnostic `message`.              |

## Context data

Context data is application-scoped data available to Cedar evaluation.

| Method                               | Behavior                                                                                                                       |
| ------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------ |
| `pushDataCtx(key, value, ttl_secs?)` | Stores a non-null JSON-compatible value. `ttl_secs` is a `bigint`, for example `3600n`; omit it to use the configured default. |
| `getDataCtx(key)`                    | Returns the stored JavaScript value or `null` if absent or expired.                                                            |
| `getDataEntryCtx(key)`               | Returns a `DataEntry` wrapper or `undefined`.                                                                                  |
| `listDataCtx()`                      | Returns all `DataEntry` wrappers.                                                                                              |
| `removeDataCtx(key)`                 | Removes one value and returns whether it existed.                                                                              |
| `clearDataCtx()`                     | Removes every stored value.                                                                                                    |
| `getStatsCtx()`                      | Returns a `DataStoreStats` wrapper.                                                                                            |

```ts
cedarling.pushDataCtx("user:alice", { plan: "pro" }, 300n);
const value = cedarling.getDataCtx("user:alice");
const entry = cedarling.getDataEntryCtx("user:alice");
const stats = cedarling.getStatsCtx();
```

`DataEntry` exposes `key`, `value()`, `data_type`, `created_at`, optional
`expires_at`, `access_count`, and `jsonString()`. `DataStoreStats` exposes
entry counts, configured limits, size metrics, memory-alert fields, and
`jsonString()`.

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
provide descriptions or other information for your application. Cedar evaluates
policy rules independently of this metadata. See the
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

When the application finishes using a Cedarling instance, shut it down:

```ts
await cedarling.shutDown();
```

## Upgrading from snake_case method names

Update calls such as `authorize_unsigned()` to `authorizeUnsigned()`,
`shut_down()` to `shutDown()`, and `json_string()` to `jsonString()` using the
API names documented above. Bootstrap keys (`CEDARLING_*`), request JSON,
result/data fields such as `request_id`, and serialized JSON retain their names.
