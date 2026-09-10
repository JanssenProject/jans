![Cedarling PARC authorization inputs flow into local policy evaluation and produce allow or deny](https://raw.githubusercontent.com/JanssenProject/jans/main/jans-cedarling/bindings/cedarling_js/docs/assets/cedarling-parc-boundary.png)

# Cedarling JavaScript SDK

[![npm](https://img.shields.io/npm/v/%40janssenproject%2Fcedarling)](https://www.npmjs.com/package/@janssenproject/cedarling)
[![license](https://img.shields.io/npm/l/%40janssenproject%2Fcedarling)](https://github.com/JanssenProject/jans/blob/main/LICENSE)

Add fast, local Cedar policy authorization to JavaScript and TypeScript
applications. `@janssenproject/cedarling` evaluates access decisions in your
application process, validates OAuth/OIDC tokens when requested, and returns
typed decisions without a network round trip to a remote policy service.

## What Cedarling evaluates

Cedarling applies Cedar policies to the PARC authorization model:

- **Principal**: who is requesting access;
- **Action**: what operation they want to perform;
- **Resource**: which protected object they want to access; and
- **Context**: request-time facts that may affect the decision.

The result is either `ALLOW` or `DENY`, accompanied by a request ID and policy
diagnostics.

## Install

```bash
npm install @janssenproject/cedarling
```

The package also installs with `pnpm add`, `yarn add`, `bun add`, or
`deno add npm:@janssenproject/cedarling`.

## Choose the runtime entry

| Environment | Import | Notes |
| --- | --- | --- |
| Node.js 22, 24, or 26 | `@janssenproject/cedarling` | ESM and CommonJS |
| Bun current stable | `@janssenproject/cedarling` | ESM |
| Deno LTS or current stable | `@janssenproject/cedarling` | ESM; allow reading the installed SDK asset |
| Modern browsers | `@janssenproject/cedarling` | Use Vite, webpack, esbuild, or another compatible bundler |
| Cloudflare Workers or Vercel Edge | `@janssenproject/cedarling/edge` | ESM-only explicit edge entry |

Use ES modules in Node.js, Bun, Deno, and bundled browser applications:

```ts
import { createCedarling } from "@janssenproject/cedarling";
```

CommonJS is available from the package root on Node.js:

```js
const { createCedarling } = require("@janssenproject/cedarling");
```

Edge hosts use:

```ts
import { createCedarling } from "@janssenproject/cedarling/edge";
```

The edge entry cannot be loaded with CommonJS `require`. Browser consumers
must bundle the package rather than serve its module directly. Deno consumers
using a local `node_modules` directory can grant the narrow runtime permission:

```bash
deno run --allow-read=node_modules/@janssenproject/cedarling/dist/wasm/cedarling_wasm_bg.wasm app.ts
```

Adjust the path for another Deno dependency layout. The package uses modern
`exports`; TypeScript projects should use `node16`, `nodenext`, or `bundler`
module resolution. Legacy `node10` resolution is not supported.

## Quick start

Create one client with an application name and policy store, authorize a
request, and shut down the client when it is no longer needed:

```ts
import { createCedarling } from "@janssenproject/cedarling";

const initialized = await createCedarling({
  applicationName: "task-api",
  policyStore: {
    type: "inline",
    document: policyStoreDocument,
  },
});

if (!initialized.ok) {
  throw initialized.error;
}

const cedarling = initialized.value;

try {
  const result = await cedarling.authorizeUnsigned({
    principal: {
      type: "Task::User",
      id: "alice",
      attributes: { role: "editor" },
    },
    action: { namespace: "Task", id: "Read" },
    resource: {
      type: "Task::Document",
      id: "document-1",
      attributes: { owner: "alice" },
    },
  });

  if (!result.ok) {
    throw result.error;
  }

  if (result.value.decision) {
    console.log("Allowed", result.value.requestId);
  } else {
    console.log("Denied", result.value.diagnostics.reasons);
  }
} finally {
  await cedarling.shutDown();
}
```

Replace `policyStoreDocument` with your Cedarling policy-store document.

## Choose an authorization method

Cedarling exposes two explicit authorization methods. There is no generic
`authorize()` method.

### Application-asserted authorization

Use `authorizeUnsigned` after your application has already authenticated the
caller and can safely assert the principal, resource, and request context.

```ts
const result = await cedarling.authorizeUnsigned({
  principal: { type: "Task::User", id: "alice" },
  action: 'Task::Action::"Update"',
  resource: {
    type: "Task::Document",
    id: "document-1",
    attributes: { owner: "alice" },
  },
  context: {
    request: { method: "PATCH", network: "internal" },
  },
});
```

The principal is optional when policies support Cedar partial evaluation.
Actions can be formal Cedar UID strings or structured values such as
`{ namespace: "Task", id: "Update" }`.

### Token-validating authorization

Use `authorizeMultiIssuer` when Cedarling should validate signed OAuth or OIDC
tokens before policy evaluation. Despite its name, the method accepts one or
more mapped tokens and does not require multiple issuers.

```ts
const result = await cedarling.authorizeMultiIssuer({
  tokens: [
    {
      mapping: "Task::AccessToken",
      payload: accessToken,
    },
  ],
  action: { namespace: "Task", id: "Read" },
  resource: { type: "Task::Document", id: "document-1" },
  context: { request: { method: "GET" } },
});
```

Each `mapping` must match token metadata in the policy store. Keep signature,
status, schema, and algorithm validation enabled in production.

## Handle decisions and failures

Every public operation returns the same discriminated result shape:

```ts
type Result<T> =
  | { ok: true; value: T }
  | { ok: false; error: CedarlingError };
```

An authorization decision of `false` is a successful policy denial, not an SDK
failure:

```ts
const result = await cedarling.authorizeUnsigned(request);

if (!result.ok) {
  console.error(result.error.code, result.error.operation);
} else if (!result.value.decision) {
  console.info("Denied by policy", result.value.requestId);
} else {
  console.info("Allowed", result.value.requestId);
}
```

Authorization decisions contain `decision`, `requestId`, and `diagnostics` with
policy reasons and errors. Operational failures expose `code`, `operation`,
optional `path` and `details`, and a standard `message`. The SDK exports no
error constructor, so branch on `error.code` rather than `instanceof`:

```ts
if (!result.ok) {
  if (result.error.code === "INPUT_OUT_OF_RANGE") {
    console.error("Invalid value at", result.error.path);
  } else {
    console.error(result.error.code, result.error.operation);
  }
}
```

Policy diagnostics use the same error shape. Read `message` explicitly because
standard `Error` properties are not all enumerable. Raw causes are hidden by
default and should remain disabled in production.

### Inspect raw engine diagnostics locally

When diagnosing a local Cedarling, WebAssembly, loader, or runtime failure, opt
in during initialization and read `error.cause` directly:

```ts
const initialized = await createCedarling({
  applicationName: "task-api",
  policyStore: {
    type: "inline",
    document: policyStoreDocument,
  },
  debug: { dangerouslyExposeRawErrors: true },
});

if (!initialized.ok) {
  throw initialized.error;
}

const result = await initialized.value.authorizeUnsigned(request);
if (!result.ok) {
  console.error(result.error.message);
  console.error(result.error.cause);
}
```

The raw cause is non-enumerable and can contain secrets. Keep this option off
outside local debugging; do not serialize or log `error.cause` in production.

## Configure policy sources

Choose exactly one policy source when creating a client.

### Inline document

```ts
policyStore: {
  type: "inline",
  document: policyStoreDocument,
}
```

### Managed URL

```ts
policyStore: {
  type: "url",
  url: "https://configuration.example/policy-store.cjar",
  refresh: { intervalSeconds: 300 },
}
```

URL sources require HTTPS, except loopback HTTP during local development.
URLs containing credentials are rejected.

### Archive bytes

```ts
policyStore: {
  type: "archive",
  bytes: archiveBytes,
}
```

### Application loader

Use a loader when the application owns authenticated retrieval or another
custom loading process. Keep authenticated endpoints fixed and trusted. If an
application selects them dynamically, require HTTPS, reject embedded
credentials, and enforce an explicit trusted-host allowlist before attaching
credentials:

```ts
policyStore: {
  type: "loader",
  load: async () => {
    const response = await fetch(
      "https://configuration.example/policy-store.cjar",
      {
        headers: { Authorization: `Bearer ${configurationToken}` },
      },
    );
    if (!response.ok) {
      throw new Error(`Policy request failed: ${response.status}`);
    }
    return new Uint8Array(await response.arrayBuffer());
  },
}
```

The loader must resolve to a non-empty `Uint8Array`.

## Store reusable context data

The context service stores detached Cedar-compatible values for later
authorization requests:

```ts
const stored = await cedarling.context.set(
  "profile_alice",
  { department: "engineering", active: true },
  { ttlSeconds: 300 },
);

if (!stored.ok) throw stored.error;

const value = await cedarling.context.get("profile_alice");
const entry = await cedarling.context.getEntry("profile_alice");
const entries = await cedarling.context.entries();
const stats = await cedarling.context.stats();

await cedarling.context.delete("profile_alice");
await cedarling.context.clear();
```

Stored values are automatically available to policies under `context.data`:

```cedar
permit(principal, action == Task::Action::"Read", resource)
when {
  context has data &&
  context.data has profile_alice &&
  context.data.profile_alice.active == true
};
```

Declare retained fields in the Cedar schema. Avoid passing request-owned
`context.data` values that collide with stored keys.

## Query retained logs

Enable memory logging when the application needs retained authorization logs:

```ts
const initialized = await createCedarling({
  applicationName: "task-api",
  logging: { type: "memory", level: "info" },
  policyStore: { type: "inline", document: policyStoreDocument },
});
```

Query or drain retained entries:

```ts
const ids = await cedarling.logs.ids();
const all = await cedarling.logs.find();
const one = await cedarling.logs.find({ id: "log-id" });
const byRequest = await cedarling.logs.find({ requestId: "request-id" });
const decisions = await cedarling.logs.find({ tag: "decision" });
const drained = await cedarling.logs.drain();
```

`find` accepts one query form: `id`, `requestId` with an optional tag, or
`tag`. Retained-log operations return `LOG_STORAGE_UNAVAILABLE` when memory
logging is not enabled.

## Observe trusted issuer readiness

Check a configured issuer by policy-store ID or exact issuer URL:

```ts
const byId = await cedarling.issuers.isLoaded({ id: "TaskIssuer" });
const byIssuer = await cedarling.issuers.isLoaded({
  iss: "https://id.example",
});
```

A successful value of `false` means that the issuer is unknown, pending, or
failed to load. It is not an SDK error.

## Configuration reference

| Option | Purpose |
| --- | --- |
| `applicationName` | Required application identity |
| `policyStore` | One URL, inline document, archive, or application loader |
| `logging` | Off, console, or retained memory logs with level and limits |
| `authorization` | Schema-validation control and decision-log token ID claim |
| `contextStore` | Entry, size, TTL, metrics, and memory-alert limits |
| `jwt` | Signature/status validation, algorithms, and refresh intervals |
| `tokenCache` | Token TTL, capacity, and eviction behavior |
| `issuerLoading` | Synchronous/asynchronous loading and worker count |
| `http` | Retry, delay, and response-size limits |
| `lock` | Lock configuration URL, credentials, and reporting intervals |
| `debug` | Explicit opt-in to potentially sensitive raw error causes |
| `bootstrapProperties` | Mutually exclusive advanced core configuration |

Typed configuration data is validated and detached during initialization; the
`policyStore.load` callback is retained and invoked by the SDK.
`contextStore.maxTtlSeconds` defaults to 3,600 seconds, and an explicit
`defaultTtlSeconds` cannot exceed it. Do not combine `bootstrapProperties` with
typed fields.

<details>
<summary>Full typed configuration reference</summary>

All typed configuration records reject unknown fields. Integer limits below
require safe JavaScript integers.

### `applicationName`

| Field | Required | Accepted value | Behavior |
| --- | --- | --- | --- |
| `applicationName` | Yes | Non-blank string | Identifies this Cedarling instance. Leading and trailing whitespace is removed. |

### `policyStore`

Choose exactly one source.

| `type` | Required fields | Optional fields | Behavior |
| --- | --- | --- | --- |
| `"inline"` | `document: JsonObject` | — | A detached Cedarling policy-store document. |
| `"url"` | `url: string \| URL` | `refresh.intervalSeconds` | Cedarling fetches and validates a remote document or Cedar Archive. URLs must be HTTPS, or loopback HTTP for local development, and cannot contain credentials. Refresh defaults to `0` (disabled); otherwise it must be at least `5` seconds. |
| `"archive"` | `bytes: Uint8Array` | — | A non-empty Cedar Archive byte array. |
| `"loader"` | `load: () => Promise<Uint8Array>` | — | Application-owned retrieval. The promise must resolve to a non-empty byte array. |

### `logging`

| `type` | Additional fields | Defaults and limits |
| --- | --- | --- |
| `"off"` | — | Logging is disabled. No other logging fields are accepted. |
| `"console"` | `level?` | `level` defaults to `"warn"`. Accepted levels: `"trace"`, `"debug"`, `"info"`, `"warn"`, `"error"`, and `"fatal"`. |
| `"memory"` | `level?`, `ttlSeconds?`, `maxItems?`, `maxItemSizeBytes?` | `level` defaults to `"warn"`; `ttlSeconds` defaults to `60` and must be `1`–`3,600` seconds; `maxItems` defaults to `10,000`; `maxItemSizeBytes` defaults to `500,000`. The two memory limits accept `0`–`4,294,967,295`; `0` removes that limit. |

When `logging` is omitted, logging is off. Use `"memory"` before calling the
retained-log service.

### `authorization`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `dangerouslyDisableSchemaValidation` | `false` | Boolean | Disables Cedar schema validation. Keep `false` outside prototyping. |
| `decisionLogTokenIdClaim` | `"jti"` | Non-blank string | Token claim retained as the decision-log token identifier. |

### `contextStore`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `maxEntries` | `10,000` | `0`–`4,294,967,295` | Maximum retained context-data entries; `0` is unlimited. |
| `maxEntrySizeBytes` | `1,048,576` | `0`–`4,294,967,295` | Maximum serialized entry size; `0` is unlimited. |
| `defaultTtlSeconds` | Unset | Positive safe integer | Expiry for entries without a per-entry TTL; it cannot exceed `maxTtlSeconds`. |
| `maxTtlSeconds` | `3,600` | Positive safe integer | Maximum allowed entry TTL in seconds. |
| `metrics` | `true` | Boolean | Enables access-count and capacity metrics. |
| `memoryAlertThresholdPercent` | `80` | Finite number from `0` to `100` | Sets the threshold reported by `context.stats()`. |

### `jwt`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `dangerouslyDisableSignatureValidation` | `false` | Boolean | Disables token signature validation; local testing only. |
| `dangerouslyDisableStatusValidation` | `false` | Boolean | Disables token-status validation; local testing only. |
| `allowedAlgorithms` | All listed algorithms | Non-empty unique list | `HS256`, `HS384`, `HS512`, `ES256`, `ES384`, `RS256`, `RS384`, `RS512`, `PS256`, `PS384`, `PS512`, or `EdDSA`. Use the smallest trusted set. |
| `jwksRefreshIntervalSeconds` | Unset | Safe integer of at least `5` | Overrides JWKS cache settings; omit it to use Cedarling's endpoint-driven refresh behavior. |
| `jwksRefreshMinIntervalSeconds` | `30` | Safe integer of at least `5` | Minimum interval between on-demand JWKS refreshes. |
| `statusListRefreshMaxSeconds` | `300` | Safe integer of at least `5` | Maximum status-list refresh interval. |

### `tokenCache`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `maxTtlSeconds` | `5` | `0`–`4,294,967,295` | Maximum token-cache TTL; `0` disables token caching. |
| `capacity` | `100` | `0`–`4,294,967,295` | Maximum cached tokens; `0` removes the capacity limit. |
| `evictEarliestExpiration` | `true` | Boolean | Removes the token nearest expiry when the cache is full. |

### `issuerLoading`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `mode` | `"sync"` | `"sync"` or `"async"` | Loads trusted issuers during initialization or in the background. |
| `workers` | `2` | Integer from `1` to `6` | Concurrent issuer-loading workers. |

### `http`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `maxRetries` | `3` | Integer from `0` to `31` | Maximum retry attempts for Cedarling HTTP requests. |
| `retryDelaySeconds` | `3` | `0`–`4,294,967,295` | Base delay between retries in seconds. |
| `maxResponseSizeBytes` | `10,485,760` | `0`–`9,007,199,254,740,991` | Maximum buffered HTTP response size; `0` disables the limit. |

### `lock`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `configurationUrl` | — | HTTPS or loopback HTTP `string \| URL` without credentials | Required when enabling Lock integration. |
| `ssaJwt` | Unset | Non-blank string | Software statement assertion used for dynamic client registration. |
| `logIntervalSeconds` | `0` | Non-negative safe integer | Interval for Lock log transmission; `0` disables it. |
| `healthIntervalSeconds` | `0` | Non-negative safe integer | Interval for Lock health transmission; `0` disables it. |
| `telemetryIntervalSeconds` | `0` | Non-negative safe integer | Interval for Lock telemetry transmission; `0` disables it. |
| `logChannelCapacity` | `100` | Integer from `1` to `4,294,967,295` | Buffered Lock log-entry capacity. |
| `logMaxRetries` | `5` | Integer from `0` to `31` | Lock log-delivery retry attempts. |

### `debug`

| Field | Default | Accepted value | Behavior |
| --- | --- | --- | --- |
| `dangerouslyExposeRawErrors` | `false` | Boolean | May expose a retained original failure in non-enumerable `error.cause`. Local debugging only; raw causes can contain secrets. |

</details>

### Use raw Cedarling bootstrap properties

Use `bootstrapProperties` when an application needs the Cedarling core
bootstrap-property contract directly. It passes a detached JSON object to the
engine without SDK property mapping, so it is mutually exclusive with every
typed option except `debug`. Consult the [Cedarling bootstrap-property
documentation](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
for supported keys and values.

```ts
const initialized = await createCedarling({
  bootstrapProperties: {
    CEDARLING_APPLICATION_NAME: "task-api",
    CEDARLING_POLICY_STORE_LOCAL: JSON.stringify(policyStoreDocument),
    CEDARLING_LOG_TYPE: "off",
  },
});

if (!initialized.ok) {
  throw initialized.error;
}

const cedarling = initialized.value;
```

<details>
<summary>Client API reference</summary>

All public operations return `Promise<Result<T>>`. A result with `ok: false`
is an SDK failure; an authorization result with `ok: true` and `decision: false`
is a successful policy denial.

`createCedarling(options)` returns `Result<CedarlingClient>`. Supply either the
typed configuration above or raw `bootstrapProperties`, never both.

### Authorization requests

| Method | Required request fields | Optional request fields | Result |
| --- | --- | --- | --- |
| `authorizeUnsigned(request)` | `action`, `resource` | `principal`, `context` | A policy decision using application-asserted identity. |
| `authorizeMultiIssuer(request)` | Non-empty `tokens`, `action`, `resource` | `context` | A policy decision after Cedarling validates mapped OAuth/OIDC tokens. |

`principal` and `resource` are Cedar entities with a non-blank `type`, a
non-blank `id`, and optional Cedar-compatible `attributes`. `action` is either
a formal Cedar action UID or `{ namespace?: string, id: string }`; omit
`namespace` for the root `Action` entity type. `context` and entity attributes
are Cedar objects: safe-integer numbers, strings, booleans, nested arrays or
objects, entity markers, and explicit Cedar extension markers.

Each multi-issuer token has a non-blank policy-store `mapping` and non-blank
JWT `payload`. The successful decision contains `decision`, `requestId`, and
policy `diagnostics` (`reasons` and normalized `errors`).

### Context service

| Method | Input | Successful value |
| --- | --- | --- |
| `context.set(key, value, options?)` | Non-blank `key`, Cedar-compatible `value`, optional `{ ttlSeconds }` | `void` |
| `context.get(key)` | Non-blank `key` | Stored value or `undefined` |
| `context.getEntry(key)` | Non-blank `key` | Value plus type, creation, expiry, and access metadata, or `undefined` |
| `context.delete(key)` | Non-blank `key` | Whether an entry was deleted |
| `context.clear()` | — | `void` |
| `context.entries()` | — | All context-data entries |
| `context.stats()` | — | Capacity and metric observation |

`context.set` accepts only `options.ttlSeconds`: a positive safe integer no
higher than the client's configured maximum TTL.

### Retained-log service

| Method | Input | Successful value |
| --- | --- | --- |
| `logs.ids()` | — | Retained log IDs |
| `logs.find(query?)` | Optional exact query | Matching retained entries |
| `logs.drain()` | — | Retained entries and clears them from storage |

`logs.find` accepts exactly one of `{ id }`, `{ requestId, tag? }`, or
`{ tag }`. Tags are `decision`, `system`, `metric`, or the documented log
levels. These methods require `logging.type: "memory"`; otherwise they return
`LOG_STORAGE_UNAVAILABLE`.

### Issuer readiness and lifecycle

| Method | Input | Successful value |
| --- | --- | --- |
| `issuers.isLoaded(reference)` | Exactly one of `{ id }` or `{ iss }` | `true` when loaded; `false` when unknown, pending, or failed |
| `shutDown()` | — | `void` |

`shutDown()` is idempotent. Once shutdown begins, new client operations return
`CLIENT_CLOSED`.

</details>

## Shut down the client

Always release a client when the application no longer needs it:

```ts
const stopped = await cedarling.shutDown();
if (!stopped.ok) {
  console.error(stopped.error.code);
}
```

Shutdown is idempotent. Once it begins, new operations return `CLIENT_CLOSED`.

## Security guidance

- Treat authorization requests, tokens, policy stores, loaders, and URLs as
  untrusted input.
- Use HTTPS outside loopback development.
- Keep token signature, status, schema, and algorithm validation enabled.
- Configure the smallest token-signature algorithm allowlist your issuers need.
- Ensure browser Content Security Policy permits WebAssembly execution.
- Never log tokens, policy material, or raw failure causes.
- Call `shutDown()` so retained state and client resources are released.

## Troubleshooting and support

For initialization or operation failures, inspect `error.code`,
`error.operation`, and `error.path` first. Browser startup also requires a
compatible bundler and a Content Security Policy that permits WebAssembly;
Deno requires read access to the installed SDK asset as shown above.

- [Cedarling documentation](https://docs.jans.io/stable/cedarling/)
- [SDK source](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_js)
- [Issue tracker](https://github.com/JanssenProject/jans/issues)
- [Apache 2.0 license](https://github.com/JanssenProject/jans/blob/main/LICENSE)
