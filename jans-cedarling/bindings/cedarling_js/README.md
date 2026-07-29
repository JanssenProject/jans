# Cedarling JavaScript SDK

`@janssenproject/cedarling` is the Web-native, high-performance TypeScript SDK for Cedarling authorization. It wraps the core Rust-based Cedar engine (compiled to WebAssembly) behind a clean, asynchronous API, returning typed `Result` objects for operational safety.

The SDK provides runtime-specific adapters that share the same public API:
- **Browser** — WebAssembly is loaded through the browser entry.
- **Node.js, Bun, Deno, and Electron main** — the `"node"` export condition loads WebAssembly from the installed package.
- **Cloudflare Workers** — the `"workerd"` export condition uses a statically bundled WebAssembly module.
- **Vercel Edge Runtime** — the `"edge-light"` export condition uses an edge-compatible WebAssembly module.

---

## Table of Contents
1. [Installation](#installation)
2. [Initialization](#initialization)
3. [Choosing an Authorization Trust Model](#choosing-an-authorization-trust-model)
4. [Authorization API Reference](#authorization-api-reference)
5. [Client Services (APIs)](#client-services-apis)
   - [Context Data (`client.context`)](#context-data-clientcontext)
   - [Decision Logs (`client.logs`)](#decision-logs-clientlogs)
   - [Trusted Issuers (`client.issuers`)](#trusted-issuers-clientissuers)
6. [Error Handling Model](#error-handling-model)
7. [Security & Production Warnings](#security--production-warnings)
8. [Runtime Constraints & Capabilities](#runtime-constraints--capabilities)

---

## Installation

Install the package via your preferred package manager:

> [!NOTE]
> Version `1.0.0` is prepared for coordinated publication with `@janssenproject/cedarling_wasm@1.0.0`; these commands require both packages to be available in the configured registry.

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

For contributor setup and repository builds, see the
[Cedarling JavaScript maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_js/docs/README.md#repository-location-and-build-order).

---

## Initialization

Initialize a Cedarling client using `createCedarling(config)`. This returns a `Result` containing the active `Cedarling` instance.

```ts
import { createCedarling } from "@janssenproject/cedarling";

const result = await createCedarling({
  applicationName: "task-manager",
  policyStore: {
    type: "url",
    url: "https://raw.githubusercontent.com/JanssenProject/CedarlingQuickstart/main/tarpDemo/policy-store.cjar",
    refresh: {
      intervalSeconds: 300,
    },
  },
});

if (!result.ok) {
  console.error("Initialization failed:", result.error.code);
  process.exit(1);
}

const client = result.value;
```

JWT signature and status validation are enabled by default, and logging is off
by default. You therefore do not need to specify either setting in the common
production case.

### Policy Store Loaders
Cedarling supports multiple portable policy store formats (without direct filesystem dependencies):

1.  **`inline`**: Raw policy store JSON object configuration.
2.  **`url`**: Fetch policy `.cjar` archive remotely via HTTPS.
3.  **`archive`**: Load policy stores as a static `Uint8Array` binary buffer.
4.  **`loader`**: Provide an async callback function returning a `Uint8Array` of `.cjar` bytes.

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

### Configuration options

`createCedarling()` exposes JavaScript-native, typed configuration groups for
policy loading, logging, authorization, context storage, JWT validation, token
caching, issuer loading, HTTP behavior, and optional Lock integration. Unknown
fields are rejected so misspelled or unsupported options fail during
initialization.

`jwt.dangerouslyDisableSignatureValidation` is not an additional validation
mechanism. It is the SDK option for controlling Cedarling signature validation,
and it is optional: omit it to keep signature verification enabled. Set it to
`true` only for controlled local tests or debugging.

Logging is also optional and defaults to off. Provide
`logging: { type: "memory" }` to query logs through `client.logs`, or
`logging: { type: "console" }` to emit them through the runtime console.

The SDK intentionally hides raw bootstrap and WebAssembly initialization
details. Maintainers can find the complete internal mapping in the
[Cedarling JavaScript maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_js/docs/README.md#configuration-mapping-boundary).

---

## Choosing an Authorization Trust Model

Cedarling supports two distinct trust paradigms depending on where claims originate:

### 1. Token-Based Access Control (TBAC - Recommended)
Uses `authorizeMultiIssuer(request)` to process signed identity/access JSON Web
Tokens (JWTs) from trusted issuers. Cedarling parses the claims, constructs
token entities, and performs the authorization decision. Signature and status
validation are enabled by default and can be disabled only through the
explicitly dangerous test/debug options described above.

### 2. Application-Asserted Authorization
Uses `authorizeUnsigned(request)` when the application has already unpacked/validated the user's identities and asserts them as raw, trusted Cedar entities.

---

## Authorization API Reference

All authorization methods return a unified authorization result containing a `decision` (`true` for `Allow`, `false` for `Deny`), diagnostic reasoning, and an execution request ID.

### Complete authorization decision

On success, the complete normalized authorization result is available as
`authResult.value`. It is a plain JavaScript value and can be serialized
directly:

```ts
const authResult = await client.authorizeUnsigned(request);

if (!authResult.ok) {
  console.error("Authorization failed:", authResult.error);
  return;
}

const { decision, requestId, diagnostics } = authResult.value;

console.log("Allowed:", decision);
console.log("Request ID:", requestId);
console.log("Reasons:", diagnostics.reasons);
console.log("Policy errors:", diagnostics.errors);
console.log(JSON.stringify(authResult.value, null, 2));
```

The serialized shape is:

```json
{
  "decision": true,
  "requestId": "<request-id>",
  "diagnostics": {
    "reasons": ["allow-view-task"],
    "errors": []
  }
}
```

The public SDK uses stable JavaScript naming. When comparing this value with
core Cedarling output, `request_id` corresponds to `requestId`,
`diagnostics.reason` to `diagnostics.reasons`, and each diagnostic
`{ "id", "error" }` to `{ "policyId", "message" }`. When memory logging is
enabled, use `client.logs.find({ requestId })` to find log entries for the same
decision.

### Optional decision shortcuts

The standard `ok`/`value`/`error` shape remains available and is recommended when you need complete explicit error handling. For concise authorization checks, all three authorization entry points also expose flat shortcuts:

```ts
const result = await client.authorizeUnsigned(request);
const { ok, allowed, denied, err } = result;

if (allowed) {
  performAction(result.value.requestId);
} else if (denied) {
  showAccessDenied();
} else {
  reportAuthorizationFailure(err);
}
```

`allowed` is `true` only when the operation succeeded and the policy allowed the request. `denied` is `true` only for a successful policy denial. Both are `false` for operational or validation failures. `err` is a short alias for `error`; the original `result.value.decision` and `result.error` properties remain supported.

The same shortcuts are available on `authorizeMultiIssuer(request)` and the discriminated `authorize(envelope)` dispatcher. These are optional additions; the existing explicit result pattern remains supported.


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
  },
  context: {},
});

const { allowed, denied, err } = authResult;

if (allowed) {
  console.log(`Decision: ALLOW (Request ID: ${authResult.value.requestId})`);
} else if (denied) {
  console.log(`Decision: DENY (Request ID: ${authResult.value.requestId})`);
} else {
  console.error("Operational failure:", err.code);
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
      department: "engineering",
      clearance: 3,
    },
  },
  action: 'Action::"ViewTask"',
  resource: {
    type: "Resource::Task",
    id: "task_101",
    attributes: {
      owner: "alice",
      confidential: false,
    },
  },
  context: {
    network_location: "internal_vpn",
  },
});

if (authResult.allowed) {
  showTask();
} else if (authResult.denied) {
  showAccessDenied();
} else {
  reportAuthorizationFailure(authResult.err);
}
```

Both `principal` and `resource` use the `CedarEntity` shape:
`{ type, id, attributes? }`. The optional `attributes` are included in the
authorization evaluation. Attribute names and value types must match the entity
definitions in the policy-store Cedar schema; the reserved
`cedar_entity_mapping` attribute is managed by the SDK and cannot be supplied
by the caller.

For `authorizeMultiIssuer()`, the principals and their attributes are derived
from validated token claims according to the policy-store mappings, so the
request does not accept a caller-provided `principal`. Its `resource` accepts
the same optional `attributes` field shown above.

### `client.authorize(envelope)`
Unified envelope dispatcher accepting a discriminated union. Recommended if the request source is determined dynamically at runtime.

```ts
const result = await client.authorize({
  type: "unsigned",
  request: {
    principal: { type: "User", id: "alice" },
    action: 'Action::"ViewTask"',
    resource: { type: "Resource::Task", id: "task_101" },
  },
});

if (result.allowed) {
  showTask();
}
```

---

## Client Services (APIs)

Once initialized, the `Cedarling` instance exposes specific modular services:

### Context Data (`client.context`)
Used to store and query transient, client-local metadata facts injected into subsequent authorization requests. **These are non-durable and exist only for the lifespan of the active isolate memory.**

#### Programmatic Usage
```ts
// Set a transient fact with TTL (in seconds)
await client.context.set("device_trusted", true, { ttlSeconds: 300 });

// Retrieve a fact
const isTrusted = await client.context.get("device_trusted"); // returns true

// Get entry metadata (includes TTL/timestamp/inferred type info)
const entry = await client.context.getEntry("device_trusted");

// Remove a key or clear the store
await client.context.delete("device_trusted");
await client.context.clear();
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

#### Context Resolution Precedence
When resolving attributes inside `context.data`, Cedarling enforces a three-tier resolution hierarchy:
1. **Inline Request Context (Highest):** Values passed directly inside the request `context` parameter at evaluation time.
2. **Pushed Context Data:** Values stored in the active client via `client.context.set()`.
3. **Default Bootstrap Context (Lowest):** Values configured in the initial bootstrap properties.


### Decision Logs (`client.logs`)
Queries in-memory authorization logs. Requires `logging: { type: "memory" }` at initialization.

```ts
// Search logs non-destructively by requestId
const logs = await client.logs.find({
  requestId: "some-req-id",
  tag: "decision",
});

// Retrieve list of log entry IDs
const ids = await client.logs.ids();

// Destructively drain the log storage buffer
const drainedLogs = await client.logs.drain();
```

### Trusted Issuers (`client.issuers`)
Inspects the JWKS load status of identity providers defined in the configuration or loaded policy store.

```ts
// Query issuer readiness by policy-store ID or issuer URL claim
const isReady = await client.issuers.isLoaded({ id: "JanssenIssuer" });
const isReadyByUrl = await client.issuers.isLoaded({ iss: "https://your-jans-server/jans-auth" });
```

### Client Closure
Free up memory allocations and safely dispose of the compiled WebAssembly instance:
```ts
await client.close();
```

---

## Error Handling Model

To prevent runtime application crashes, the SDK does not throw errors on expected operational failures (e.g. signature errors, malformed tokens, network timeouts). Instead, they are returned as typed `Result` variants:

```ts
type Result<T, E> = { ok: true; value: T } | { ok: false; error: E };
```

> [!NOTE]
> An authorization **Deny** is a successful policy evaluation, not an operational failure. Therefore, if `authResult.ok` is `true`, the decision may still be `false` (Deny). The optional `allowed` and `denied` shortcuts make these policy outcomes explicit; both are `false` when `ok` is `false`.

---

## Security & Production Warnings

> [!WARNING]
> Setting `jwt.dangerouslyDisableSignatureValidation: true` turns off cryptographic verification. **Never use this setting in production environments.** It is intended solely for local testing and debugging.

> [!WARNING]
> The `authorization.dangerouslyDisableSchemaValidation` option disables Cedar schema validation for authorization requests. When enabled (`true`), `authorizeMultiIssuer()` may produce **`AUTHORIZATION_FAILED`** errors even for valid requests. This is a known incompatibility between the multi-issuer flow and schema validation bypass; it is **not safe for production** and should only be used for debugging with unsigned requests.

*   Always use secure OIDC transport channels (HTTPS) to resolve remote JWKS and policy archives.
*   Log level data stored in memory can contain sensitive claim attributes. Ensure `client.logs.drain()` is periodically called and written to a secure external audit ledger.

---

## Runtime Constraints & Capabilities

*   **Dual Format:** The primary distribution is ESM. A bundled CommonJS entry is also provided (via esbuild) for compatibility with CJS consumers; both resolve through the `exports` field.
*   **WebAssembly Requirements:** The WebAssembly bundle requires modern V8/JS engines supporting post-MVP features (`BigInt`-to-`i64` integration, reference types, and sign extension).
*   **Browser CSP:** Standard Content Security Policy settings must permit WASM loading. In strict configurations, ensure your headers include:
    ```http
    Content-Security-Policy: script-src 'self' 'wasm-unsafe-eval';
    ```
*   **Serverless Lifecycle (AWS Lambda / Vercel Edge):** These run inside ephemeral container processes. Cached instances might improve warm-start performance, but logs/context state are evicted when the container shuts down. Write critical decision logs to external databases such as the [Cedarling Lock Server](https://docs.jans.io/stable/cedarling/reference/cedarling-lock-server/) or any other logging mechanism which is decoupled from the runtime lifecycle.
