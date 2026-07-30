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

---

## Initialization

Initialize a Cedarling client using `createCedarling(options)`. This returns a `Result` containing the active `CedarlingClient`.

```ts
import { createCedarling } from "@janssenproject/cedarling";

const result = await createCedarling({
  applicationName: "task-manager",
  // JWT validation configuration (nested object)
  jwt: {
    // Enable signature validation in production
    dangerouslyDisableSignatureValidation: false,
  },
  // Configuration options for policy loading
  policyStore: {
    type: "url",
    url: "https://raw.githubusercontent.com/JanssenProject/CedarlingQuickstart/main/tarpDemo/policy-store.cjar",
    refresh: { intervalSeconds: 300 },
  },
  // Enable logging store
  logging: {
    type: "memory",
  },
});

if (!result.ok) {
  console.error("Initialization failed:", result.error.code);
  process.exit(1);
}

const client = result.value;
```

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
or `bootstrapProperties`. The SDK validates and detaches the raw JSON object,
but does not rename, default, or interpret its properties before passing it to
Cedarling core. See the
[Cedarling bootstrap property reference](https://docs.jans.io/nightly/cedarling/reference/cedarling-properties/)
for the authoritative keys and values.

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

---

## Choosing an Authorization Trust Model

Cedarling supports two distinct trust paradigms depending on where claims originate:

### 1. Token-Based Access Control (TBAC - Recommended)
Uses `authorizeMultiIssuer(request)` to validate incoming signed cryptographically secure identity/access JSON Web Tokens (JWTs) against trusted issuers. Cedarling parses the claims, constructs token entities, performs authorization decisions, and ensures token expiration/signature rules are strictly enforced.

### 2. Application-Asserted Authorization
Uses `authorizeUnsigned(request)` when the application has already unpacked/validated the user's identities and asserts them as raw, trusted Cedar entities.

---

## Authorization API Reference

Both authorization methods return the canonical `Result` shape. A successful
`value` contains `decision` (`true` for Allow, `false` for Deny), `requestId`,
and complete `diagnostics` with `reasons` and policy-evaluation `errors`.
Operational failures are available only through `error`.

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
  },
  action: 'Action::"ViewTask"',
  resource: {
    type: "Resource::Task",
    id: "task_101",
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
