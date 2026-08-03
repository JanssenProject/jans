# Cedarling JavaScript SDK

`@janssenproject/cedarling` is the Web-native, high-performance TypeScript SDK for Cedarling authorization. It wraps the core Rust-based Cedar engine (compiled to WebAssembly) behind a clean, asynchronous API, returning typed `Result` objects for operational safety.

The SDK provides runtime-specific adapters that share the same public API:

- **Browser** — WebAssembly is loaded through the browser entry.
- **Node.js, Bun, Deno, and Electron main** — the `"node"` export condition loads WebAssembly from the installed package.
- **Cloudflare Workers** — the `"workerd"` export condition uses a statically bundled WebAssembly module.
- **Vercel Edge Runtime** — the `"edge-light"` export condition uses an edge-compatible WebAssembly module.

---

## Table of Contents
- [Cedarling JavaScript SDK](#cedarling-javascript-sdk)
  - [Table of Contents](#table-of-contents)
  - [Installation](#installation)
    - [Build from source](#build-from-source)
  - [Initialization](#initialization)
    - [Raw bootstrap properties](#raw-bootstrap-properties)
    - [Policy Store Loaders](#policy-store-loaders)
  - [Choosing an Authorization Trust Model](#choosing-an-authorization-trust-model)
    - [1. Token-Based Access Control (TBAC - Recommended)](#1-token-based-access-control-tbac---recommended)
    - [2. Application-Asserted Authorization](#2-application-asserted-authorization)
  - [Authorization API Reference](#authorization-api-reference)
    - [Complete authorization decision](#complete-authorization-decision)
    - [`client.authorizeMultiIssuer(request)`](#clientauthorizemultiissuerrequest)
    - [`client.authorizeUnsigned(request)`](#clientauthorizeunsignedrequest)
  - [Client Services (APIs)](#client-services-apis)
    - [Context Data (`client.context`)](#context-data-clientcontext)
      - [Programmatic Usage](#programmatic-usage)
      - [How it is Used in Cedar Policies](#how-it-is-used-in-cedar-policies)
      - [Schema Configuration Requirements](#schema-configuration-requirements)
      - [Request and Stored Context Collisions](#request-and-stored-context-collisions)
    - [Decision Logs (`client.logs`)](#decision-logs-clientlogs)
    - [Trusted Issuers (`client.issuers`)](#trusted-issuers-clientissuers)
    - [Client Shutdown](#client-shutdown)
  - [Error Handling Model](#error-handling-model)
    - [Local raw diagnostics](#local-raw-diagnostics)
  - [Security \& Production Warnings](#security--production-warnings)
  - [Runtime Constraints \& Capabilities](#runtime-constraints--capabilities)

---

## Installation

Install the package via your preferred package manager:

> [!NOTE]
> Install and import only `@janssenproject/cedarling`. A published SDK package
> declares its coordinated `@janssenproject/cedarling_wasm` dependency and
> keeps the generated binding behind the public JavaScript API.

```bash
# npm
npm install @janssenproject/cedarling

# pnpm
pnpm add @janssenproject/cedarling

# yarn
yarn add @janssenproject/cedarling

# bun
bun add @janssenproject/cedarling
```

For **Deno**, import it directly from npm:

```ts
import { createCedarling } from "npm:@janssenproject/cedarling";
```

### Build from source

To build the SDK from repository source, follow the
[Cedarling JavaScript maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_js/docs/README.md#repository-location-and-build-order).

---

## Initialization

Initialize a Cedarling client using `createCedarling(options)`. The factory
returns a `Result` containing either the active `CedarlingClient` or a stable
initialization error.

```ts
import { createCedarling } from "@janssenproject/cedarling";

const result = await createCedarling({
  applicationName: "task-manager",
  jwt: {
    allowedAlgorithms: ["RS256"],
  },
  policyStore: {
    type: "url",
    url: "https://raw.githubusercontent.com/JanssenProject/CedarlingQuickstart/main/tarpDemo/policy-store.cjar",
    refresh: { intervalSeconds: 300 },
  },
});

if (!result.ok) {
  throw result.error;
}

const client = result.value;
```

JWT signature and status validation are enabled by default, while logging is
off by default. `jwt.dangerouslyDisableSignatureValidation` is the typed,
inverse control for the existing `CEDARLING_JWT_SIG_VALIDATION` bootstrap
property; it does not add a second validation mechanism. Never set it to
`true` in production. Similarly, `logging.type` selects the underlying
`CEDARLING_LOG_TYPE` only when logging is configured. Omit `logging` for the
default off behavior, and enable memory logging only when the application
needs `client.logs`. The complete internal mapping is maintained in the
[typed configuration mapping](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_js/docs/README.md#typed-configuration-mapping).

### Raw bootstrap properties

The same factory also accepts Cedarling bootstrap properties directly when
cross-binding parity or a newly introduced core property is more important
than the curated Web-native configuration:

```ts
const rawResult = await createCedarling({
  bootstrapProperties: {
    CEDARLING_APPLICATION_NAME: "task-manager",
    CEDARLING_POLICY_STORE_URI:
      "https://raw.githubusercontent.com/JanssenProject/CedarlingQuickstart/main/tarpDemo/policy-store.cjar",
    CEDARLING_LOG_TYPE: "memory",
  },
});
```

Choose exactly one initialization shape: either the typed Web-native options
or raw `bootstrapProperties`. Both shapes may also include the SDK-only
`debug` option. The SDK validates and detaches the raw JSON object but does not
rename or insert Cedarling bootstrap properties. It only derives the client
capabilities needed to enforce services such as memory-log access and context
TTL limits; `debug` is never forwarded to Cedarling core. See the
[Cedarling bootstrap property reference](https://docs.jans.io/nightly/cedarling/reference/cedarling-properties/)
for the authoritative keys and values.

### Policy Store Loaders

The Web-native configuration supports four portable policy sources without
exposing filesystem paths:

1. **`inline`**: A complete policy-store JSON document.
2. **`url`**: Cedarling-managed loading of JSON or `.cjar` content from HTTPS,
   or loopback HTTP for local development, with optional refresh.
3. **`archive`**: Non-empty `.cjar` bytes supplied as a `Uint8Array`.
4. **`loader`**: An async application callback returning non-empty `.cjar`
   bytes. It is invoked at most once during initialization.

```ts
// Archive: pre-fetched .cjar bytes (e.g., from authenticated fetch)
const archiveResult = await createCedarling({
  applicationName: "my-app",
  policyStore: {
    type: "archive",
    bytes: new Uint8Array([80, 75, 3, 4, /* ... .cjar bytes */]),
  },
});

// Loader: async callback for custom fetching with auth headers
const loaderResult = await createCedarling({
  applicationName: "my-app",
  policyStore: {
    type: "loader",
    load: async () => {
      const response = await fetch(
        "https://internal-policy.example/policy.cjar",
        { headers: { Authorization: "Bearer " + internalToken } },
      );
      return new Uint8Array(await response.arrayBuffer());
    },
  },
});
```

---

## Choosing an Authorization Trust Model

Cedarling supports two distinct trust paradigms depending on where claims originate:

### 1. Token-Based Access Control (TBAC - Recommended)

Uses `authorizeMultiIssuer(request)` to validate signed identity and access
JWTs against trusted issuers. Cedarling parses the claims, constructs token
entities, and evaluates the policy. Signature and status validation are
enabled by default; the dangerous opt-outs documented below are intended only
for isolated development.

### 2. Application-Asserted Authorization

Uses `authorizeUnsigned(request)` when the application has already established
the caller's identity and asserts trusted Cedar entities directly.

---

## Authorization API Reference

Both authorization methods return the canonical `Result` shape. A successful
`value` contains `decision` (`true` for Allow, `false` for Deny), `requestId`,
and complete `diagnostics` with `reasons` and policy-evaluation `errors`.
Operational failures are available only through `error`.

The SDK rejects unknown fields on requests, entities, actions, and token
inputs. Application-defined entity `attributes` and authorization `context`
remain open Cedar data objects and are validated recursively.

### Complete authorization decision

The complete, detached JavaScript decision is `authResult.value`. It can be
read directly or serialized without accessing the generated WebAssembly
wrapper:

```ts
if (authResult.ok) {
  const {
    decision,
    requestId,
    diagnostics: { reasons, errors },
  } = authResult.value;

  console.log({ decision, requestId, reasons, errors });
  console.log(JSON.stringify(authResult.value, null, 2));
}
```

The serialized public shape is:

```json
{
  "decision": false,
  "requestId": "0195c7f0-example",
  "diagnostics": {
    "reasons": [],
    "errors": [
      {
        "policyId": "allow-update-task",
        "message": "Policy evaluation failed."
      }
    ]
  }
}
```

The SDK normalizes the generated Cedarling `request_id` to `requestId`,
`diagnostics.reason` to `diagnostics.reasons`, and diagnostic `{ id, error }`
entries to `{ policyId, message }`. The generated response remains an internal
implementation detail; consumers receive this stable JavaScript-owned value.

### `client.authorizeMultiIssuer(request)`
Pass incoming signed OAuth/OIDC JWT access tokens to validate and authorize.

```ts
const authResult = await client.authorizeMultiIssuer({
  tokens: [
    {
      mapping: "Authorization::AccessToken",
      payload: jwtAccessTokenString, // e.g., Bearer token from headers
    },
  ],
  action: 'Action::"UpdateTask"',
  resource: {
    type: "Resource::Task",
    id: "task_101",
    attributes: {
      owner: "alice",
    },
  },
  context: {},
});

if (!authResult.ok) {
  console.error("Operational failure:", authResult.error.code);
} else if (authResult.value.decision) {
  console.log(`Decision: ALLOW (Request ID: ${authResult.value.requestId})`);
} else {
  console.log(`Decision: DENY (Request ID: ${authResult.value.requestId})`);
  console.log("Reasons:", authResult.value.diagnostics.reasons);
  console.log("Policy errors:", authResult.value.diagnostics.errors);
}
```

### `client.authorizeUnsigned(request)`
Directly evaluate user claims asserted by the application context.

```ts
const authResult = await client.authorizeUnsigned({
  principal: {
    type: "User",
    id: "alice",
    attributes: {
      role: "editor",
      department: "engineering",
    },
  },
  action: 'Action::"ViewTask"',
  resource: {
    type: "Resource::Task",
    id: "task_101",
    attributes: {
      owner: "alice",
      status: "open",
    },
  },
  context: {
    network_location: "internal_vpn",
  },
});

if (!authResult.ok) {
  reportAuthorizationFailure(authResult.error);
} else if (authResult.value.decision) {
  showTask();
} else {
  showAccessDenied(authResult.value.diagnostics);
}
```

Both unsigned `principal` and `resource` values use the `CedarEntity` shape
`{ type, id, attributes? }`. Attribute names and Cedar value types must match
the entity declarations in the active policy-store schema. The reserved
`cedar_entity_mapping` attribute is owned by the SDK and cannot be supplied by
the caller. For multi-issuer authorization, Cedarling derives principals and
their attributes from validated token mappings; the request supplies the
resource and its optional attributes.

---

## Client Services (APIs)

Once initialized, the `CedarlingClient` exposes modular context, log, and
issuer services. Like authorization, every service method returns a `Result`.

### Context Data (`client.context`)

Used to store and query transient, client-local metadata facts injected into subsequent authorization requests. **These are non-durable and exist only for the lifespan of the active isolate memory.**

#### Programmatic Usage

```ts
// Set a transient fact with TTL (in seconds)
const stored = await client.context.set(
  "device_trusted",
  true,
  { ttlSeconds: 300 },
);
if (!stored.ok) {
  console.error(stored.error.code);
}

// Retrieve a fact
const read = await client.context.get("device_trusted");
if (read.ok) {
  console.log(read.value); // true, or undefined when missing/expired
}

// Get entry metadata (includes TTL/timestamp/inferred type info)
const entry = await client.context.getEntry("device_trusted");

// List entries and inspect capacity/metrics
const entries = await client.context.entries();
const stats = await client.context.stats();

// Remove a key or clear the store; each call returns a Result
const removed = await client.context.delete("device_trusted");
const cleared = await client.context.clear();
```

#### How it is Used in Cedar Policies

Any value stored via `client.context.set(key, value)` is automatically injected under the **`context.data`** namespace during policy evaluation.

To use these values in your policies, you must query them inside the `context.data` record:
```cedar
permit(
    principal,
    action == Action::"UpdateTask",
    resource
) when {
    context has data &&
    context.data has device_trusted &&
    context.data.device_trusted == true
};
```

#### Schema Configuration Requirements

Cedar is strictly typed and rejects undeclared record attributes during policy evaluation. Therefore, if you push data keys dynamically, your **Policy Store Schema** must explicitly declare the `data` record as an optional field in the action's context:

```cedar
namespace TaskApp {
  // Define all possible dynamic fields that can be pushed via client.context.set
  // Use optional fields (?) since they might not be present in every request
  type DataContext = {
    "device_trusted"?: Bool,
    "user_location"?: String
  };

  action "UpdateTask" appliesTo {
    principal: [User],
    resource: [Task],
    context: {
      "data"?: DataContext
    }
  };
}
```

#### Request and Stored Context Collisions

Retained facts are injected below `context.data`. Do not also provide the same
`context.data` key in an authorization request: request and stored values are
not treated as an overwrite hierarchy, and a collision can return an
`AUTHORIZATION_FAILED` result. Use distinct keys or update the retained value
before authorizing.

### Decision Logs (`client.logs`)

Queries in-memory authorization logs. Requires `logging: { type: "memory" }` at initialization.

```ts
// Search logs non-destructively by requestId
const found = await client.logs.find({
  requestId: "some-req-id",
  tag: "decision",
});
if (found.ok) {
  console.log(found.value);
}

// Retrieve list of log entry IDs
const ids = await client.logs.ids();

// Destructively drain the log storage buffer
const drainedLogs = await client.logs.drain();
```

If memory logging is not enabled, these methods return
`LOG_STORAGE_UNAVAILABLE` rather than an ambiguous empty result.

### Trusted Issuers (`client.issuers`)

Inspects the JWKS load status of identity providers defined in the configuration or loaded policy store.

```ts
// Query issuer readiness by policy-store ID or issuer URL claim
const byId = await client.issuers.isLoaded({ id: "JanssenIssuer" });
const byIssuer = await client.issuers.isLoaded({
  iss: "https://your-jans-server/jans-auth",
});

if (byId.ok) {
  console.log(byId.value); // false is a normal not-loaded observation
}
```

### Client Shutdown

Free up memory allocations and safely dispose of the compiled WebAssembly instance:

```ts
const shutDown = await client.shutDown();
if (!shutDown.ok) {
  console.error("Shutdown failed:", shutDown.error.code);
}
```

---

## Error Handling Model

To prevent runtime application crashes, the SDK does not throw errors on expected operational failures (e.g. signature errors, malformed tokens, network timeouts). Instead, they are returned as typed `Result` variants:

```ts
type Result<T, E> = { ok: true; value: T } | { ok: false; error: E };
```

> [!NOTE]
> An authorization **Deny** is a successful policy evaluation, not an operational failure. Therefore, if `authResult.ok` is `true`, `authResult.value.decision` may still be `false` (Deny).

### Local raw diagnostics

SDK errors are redacted by default. When a Cedarling, WebAssembly, loader, or
runtime failure cannot be represented safely, the SDK retains it privately.
For local development only, you can expose that original failure as a
non-enumerable `error.cause`:

```ts
const result = await createCedarling({
  applicationName: "local-debugging",
  policyStore: { type: "inline", document: policyStoreDocument },
  debug: { dangerouslyExposeRawErrors: true },
});

if (!result.ok) {
  console.error(result.error.cause);
}
```

The raw cause is intentionally excluded from object spreading and JSON
serialization, but direct access and runtime inspection can still disclose
tokens, policy material, URLs, filesystem paths, or other secrets. Never
enable this option in production or send the raw cause to logs or telemetry.

---

## Security & Production Warnings

<!-- markdownlint-disable MD028 -->

> [!WARNING]
> Setting `jwt.dangerouslyDisableSignatureValidation: true` turns off cryptographic verification. **Never use this setting in production environments.** It is intended solely for local testing and debugging.

> [!WARNING]
> Omitting `jwt.allowedAlgorithms` enables every signature algorithm supported
> by Cedarling core, including symmetric `HS256`, `HS384`, and `HS512`.
> Production applications should configure the smallest allowlist required by
> their trusted issuers and normally prefer asymmetric algorithms for remote
> OIDC issuers.

> [!WARNING]
> Setting `debug.dangerouslyExposeRawErrors: true` makes Cedarling's original,
> potentially secret-bearing failures directly accessible through
> `error.cause`. Use it only for local debugging and disable it before
> deployment.

> [!WARNING]
> Setting `authorization.dangerouslyDisableSchemaValidation: true` disables
> Cedar schema validation. It can allow schema-incompatible entity attributes
> or context fields to reach policy evaluation and fail later as
> `AUTHORIZATION_FAILED`. Use it only for isolated debugging, never as a
> production compatibility mechanism.

<!-- markdownlint-enable MD028 -->

- Use HTTPS for every remote OIDC, JWKS, Lock, and policy-store endpoint.
  Loopback HTTP is accepted only to support local development.
- In-memory logs can contain sensitive claim attributes. Drain and transfer
  audit data only to an appropriately protected destination.

---

## Runtime Constraints & Capabilities

- **Node.js:** The supported version floor is declared in the package
  `engines` field (`>=20.19`).
- **Dual Format:** The primary distribution is ESM. Bundled CommonJS entries
  are provided for compatible Node consumers through conditional exports.
- **WebAssembly Requirements:** The package requires modern JavaScript engines
  supporting WebAssembly `BigInt`-to-`i64` integration, reference types, and
  sign extension.
- **Browser CSP:** Content Security Policy must permit WebAssembly compilation.
  In strict configurations, ensure your headers include:

    ```http
    Content-Security-Policy: script-src 'self' 'wasm-unsafe-eval';
    ```

- **Serverless Lifecycle (AWS Lambda / Vercel Edge):** These environments use
  ephemeral isolates or processes. Cached clients may improve warm starts, but
  in-memory logs and context disappear with the isolate. Transfer critical
  audit data to a durable service such as the
  [Cedarling Lock Server](https://docs.jans.io/stable/cedarling/reference/cedarling-lock-server/).
