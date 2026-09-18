---
tags:
  - cedarling
  - javascript
  - WASM
  - browser app
  - getting-started
---

# Getting Started with Cedarling in a JavaScript app

This guide explains how to install, build, and use the Cedarling WebAssembly
(WASM) package from JavaScript.

## Installation

### Using the package manager

You can easily install Cedarling using WASM.

```sh
npm i @janssenproject/cedarling_wasm
```

Alternatively, see [here](#build-from-source), if you want to build Cedarling from the source.

### Build from Source

#### Requirements

Rust 1.63 or Greater. Ensure that you have `Rust` version 1.63 or higher installed.
You can check your current version of Rust using the command below.

```bash title="Command"
rustc --version
```

Installed `wasm-pack` via `Cargo`. You can install it with the following command:

```bash title="Command"
cargo install wasm-pack
```

Ensure that Clang is installed with support for WebAssembly targets. You can check the installation and available targets with:

```bash title="Command"
clang -print-targets
```

Check `clang` version

```bash title="Command"
clang --version
```

#### Building

Clone the Janssen server repository from the GitHub and change the directory to the `cedarling_wasm` directory:

```bash title="Command"
cd /path/to/jans/jans-cedarling/bindings/cedarling_wasm
```

Generate the web-target binding used as the JavaScript package's build input:

```bash title="Command"
wasm-pack build --release --locked --target web --scope janssenproject
```

This creates the intermediate `pkg/` directory. Assemble the publishable package
from the handwritten source in `js/`:

```bash title="Command"
cd js
npm ci --ignore-scripts
npm run build
```

See the [JavaScript package maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/js/docs/maintainer.md)
for the complete build and verification workflow.

## Usage

!!! info "Sample Apps"

    See the [maintained JavaScript examples](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_wasm/js/examples)
    for Node.js ESM, Node.js CommonJS, React with Vite, webpack, and Cloudflare
    Workers integrations.

### Initialization

The package loads its bundled WASM module when `init()` creates a Cedarling
instance.

```js
import { init } from "@janssenproject/cedarling_wasm";

let cedarling = await init({
  "CEDARLING_APPLICATION_NAME": "My App",
  // make sure to update this with your own policy store
  "CEDARLING_POLICY_STORE_URI": "https://raw.githubusercontent.com/...",
  "CEDARLING_LOG_TYPE": "std_out",
  "CEDARLING_LOG_LEVEL": "DEBUG",
  "CEDARLING_JWT_SIG_VALIDATION": "disabled",
});
```

### Policy Store Sources (WASM)

Choose the loading method that matches how your application obtains the policy
archive.

#### Load an archive from a URL

Pass the archive URL through `CEDARLING_POLICY_STORE_URI` when calling `init()`:

```javascript
import { init } from "@janssenproject/cedarling_wasm";

const cedarling = await init({
  CEDARLING_APPLICATION_NAME: "My App",
  CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar",
});
```

#### Load archive bytes

Use `initFromArchiveBytes()` when the application fetches or otherwise obtains
the archive itself:

```javascript
import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";

const token = "<your-bearer-token>";
const config = {
  CEDARLING_APPLICATION_NAME: "My App",
  // ... other configuration properties
};

const response = await fetch("https://example.com/policy-store.cjar", {
  headers: { Authorization: `Bearer ${token}` },
});
const bytes = new Uint8Array(await response.arrayBuffer());
const cedarling = await initFromArchiveBytes(config, bytes);
```

For the directory-based format, package your policy store as a `.cjar` file and host it:

```bash
cd policy-store && zip -r ../policy-store.cjar .
```

See [Policy Store Formats](../reference/cedarling-policy-store.md#policy-store-formats) for details.

### Authorization

Cedarling provides two main interfaces for performing authorization checks: **Token-Based Authorization** and **Unsigned Authorization**. Both methods involve evaluating access requests based on various factors, including principals (entities), actions, resources, and context. The difference lies in how the Principals are provided.

- [**Token-Based Authorization**](#token-based-authorization-multi-issuer) is the standard method where principals are extracted from JSON Web Tokens (JWTs), typically used in scenarios where you have existing user authentication and authorization data encapsulated in tokens.
- [**Unsigned Authorization**](#unsigned-authorization) allows you to pass principals directly, bypassing tokens entirely. This is useful when you need to authorize based on internal application data, or when tokens are not available.

#### Token-Based Authorization (Multi-Issuer)

For token-based authorization, use `authorizeMultiIssuer` which processes JWT tokens and maps them to Cedar entities based on the `token_metadata` configuration in your policy store.

**1. Prepare tokens**

Tokens are provided as an array of `TokenInput` objects, each specifying a mapping name and the JWT payload:

```js
const tokens = [
  { mapping: "Jans::Access_token", payload: "<access_token_jwt>" },
  { mapping: "Jans::Id_token", payload: "<id_token_jwt>" },
];
```

The `mapping` field corresponds to the entity type name defined in your policy store's `token_metadata`.

**2. Define the resource**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```js
const resource = {
  cedar_entity_mapping: {
    entity_type: "Jans::Application",
    id: "app_id_001",
  },
  name: "App Name",
  url: {
    host: "example.com",
    path: "/admin-dashboard",
    protocol: "https",
  },
};
```

**3. Define the action**

An _action_ represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```js
const action = 'Jans::Action::"Read"';
```

**4. Define Context (optional)**

The _context_ represents additional data that may affect the authorization decision.

```js
const context = {
  current_time: Math.floor(Date.now() / 1000),
};
```

**5. Build and execute the request**

```js
const request = {
  tokens: tokens,
  action: action,
  resource: resource,
  context: context,
};

// the request is passed as a JSON string
const result = await cedarling.authorizeMultiIssuer(JSON.stringify(request));
```

See [Multi-Issuer Authorization](../reference/cedarling-multi-issuer.md) for more details.

#### Unsigned Authorization

In unsigned authorization, you pass a Principal directly, without relying on tokens. This can be useful when the application needs to perform authorization based on internal data, or when token-based data is not available. The principal is optional — omit it (or pass `null`) to evaluate the request with partial evaluation.

**1. Define the Principal**

```js
const principal = {
  cedar_entity_mapping: {
    entity_type: "Jans::User",
    id: "random_user_id",
  },
  roles: ["admin", "manager"],
};
```

**2. Define the Resource**

This represents the _resource_ that the action will be performed on, such as a protected API endpoint or file.

```js
const resource = {
  cedar_entity_mapping: {
    entity_type: "Jans::Application",
    id: "app_id_001",
  },
  name: "App Name",
  url: {
    host: "example.com",
    path: "/admin-dashboard",
    protocol: "https",
  },
};
```

**3. Define the Action**

An _action_ represents what the principal is trying to do to the resource. For example, read, write, or delete operations.

```js
const action = 'Jans::Action::"Write"';
```

**4. Define the Context**

The _context_ represents additional data that may affect the authorization decision, such as time, location, or user-agent.

```js
const context = {
  current_time: Math.floor(Date.now() / 1000),
  device_health: ["Healthy"],
  location: "US",
  network: "127.0.0.1",
  operating_system: "Linux",
};
```

**5. Build the Request**

Now you'll construct the **_request_** by including the _principal_, _action_, and _context_.

```js
const request = {
  principal: principal,
  action: action,
  resource: resource,
  context: context,
};
```

**6. Perform Authorization**

Finally, call the `authorizeUnsigned` function to check whether the principal is allowed to perform the specified action on the resource. The request is passed as a JSON string.

```js
const result = await cedarling.authorizeUnsigned(JSON.stringify(request));
```

#### Batch Authorization

Each entry in `results` is a `BatchItemUnsignedResult` — `.is_ok` reports whether Cedar reached a decision; `.unwrap()` returns the `AuthorizeResult` on Ok, `.error` returns the `BatchItemError` on Err. Positional mapping to `items[i]` is preserved for both branches; the shared `batch_id` (UUIDv7) is stamped on every per-item decision-log entry.

```js
const request = {
  principal: principal,
  items: [
    { resource: doc1Resource, action: 'Jans::Action::"View"', context: {} },
    { resource: doc2Resource, action: 'Jans::Action::"View"', context: {} },
  ],
};

const response = await cedarling.authorizeUnsignedBatch(JSON.stringify(request));

console.log('batch_id:', response.batch_id);
response.results.forEach((r, i) => {
  if (r.is_ok) {
    const ok = r.unwrap();
    console.log(`item ${i}: ${ok.decision ? 'allow' : 'deny'}`);
  } else {
    // r.error carries { category, item_index, message }
    console.log(`item ${i}: build error: ${r.error.category} at index ${r.error.item_index}`);
  }
});
```

For multi-issuer, swap `{ principal, items }` for `{ tokens, items }` and call `authorizeMultiIssuerBatch`. `context` is optional on each item and defaults to `{}`. See [Batch Authorization](../reference/cedarling-authz.md#batch-authorization) for the request / response shape, failure model, and `BatchItemError` variant list.

### Logging

The logs could be retrieved using the `popLogs` function.

```js
const logs = cedarling.popLogs();
console.log(logs);
```

## API reference

`@janssenproject/cedarling_wasm` ships TypeScript declarations for every public
export. The package exposes them through its public entry points, so editors and
TypeScript resolve the authoritative API automatically after installation.

See the Cedarling references for [authorization](../reference/cedarling-authz.md),
[bootstrap properties](../reference/cedarling-properties.md),
[policy stores](../reference/cedarling-policy-store.md), and
[logs](../reference/cedarling-logs.md).

---

## See Also

- [Cedarling TBAC quickstart](../quick-start/cedarling-quick-start.md#implement-rbac-using-signed-tokens-tbac)
- [Cedarling Unsigned quickstart](../quick-start/cedarling-quick-start.md#step-1-create-the-cedar-policy-and-schema)
- [Cedarling Sidecar Tutorial](../developer/sidecar/cedarling-sidecar-tutorial.md)
