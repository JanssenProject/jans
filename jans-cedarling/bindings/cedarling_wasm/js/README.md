# Getting Started with Cedarling in a JavaScript App

The `@janssenproject/cedarling_wasm` package brings
[Cedarling](https://docs.jans.io/stable/cedarling/) authorization to JavaScript and
TypeScript applications. Cedarling evaluates Cedar policies within your
application's process, whether it runs in a browser, on a server, or at the edge.
It determines whether a principal or token set may perform an action on a resource.

Use Cedarling to apply consistent access policies, inspect authorization decisions,
retain decision logs, and make application context available to policy evaluation.

## Installation

### Using the Package Manager

```sh
npm install @janssenproject/cedarling_wasm
```

Choose the package entry for your application. The package ships Cedarling's
WebAssembly (`.wasm`) binary; the last column describes how that binary is loaded
and initialized. Your application's policy archive is configured separately.

| Environment                                  | Package Entry                                       | Packaged WASM Loading               |
| -------------------------------------------- | --------------------------------------------------- | ----------------------------------- |
| Browser applications with supported bundlers | `@janssenproject/cedarling_wasm`                    | Automatic                           |
| Node.js 22, 24, or 26, ESM or CommonJS       | `@janssenproject/cedarling_wasm`                    | Automatic                           |
| Cloudflare Workers and Vercel Edge           | `@janssenproject/cedarling_wasm/edge`               | Bundled as a precompiled module     |
| Other ESM bundlers                           | `@janssenproject/cedarling_wasm/manual` and `/wasm` | Application-configured WASM loading |

Browser applications use an ESM-aware bundler. Follow
[Other Integration Paths](#other-integration-paths) for edge deployment and
application-controlled WASM loading.

### Build from Source

For local generation, package build, and qualification instructions, see the
[maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/js/docs/maintainer.md).

## Usage

### Initialization

Create a Cedarling instance with `init()`. It ensures the package's WASM binary is
loaded and initialized, then creates the instance using an object or `Map` of
canonical Cedarling bootstrap properties.

For browser or Node.js ESM applications:

```ts
import { init } from "@janssenproject/cedarling_wasm";

const config = {
  CEDARLING_APPLICATION_NAME: "task-api",
  CEDARLING_LOG_TYPE: "memory",
  CEDARLING_LOG_TTL: 120,
};
let cedarling;
try {
  cedarling = await init({
    ...config,
    CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar",
  });
} catch (error) {
  console.error("Cedarling initialization failed", error);
  throw error;
}
```

Replace the example URL with your application's policy archive. For Node.js
CommonJS, use the following import and run the initialization inside an async
function:

```js
const { init } = require("@janssenproject/cedarling_wasm");
```

The examples below reuse `cedarling`. Run each authorization walkthrough
separately, with a policy store that matches its entities and attributes.
Handle initialization failures in your application's startup error handler.
See the [bootstrap-property reference](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
for configuration values, defaults, and token-validation settings.

### Policy Store Sources

A Cedar archive (`.cjar`) packages the schema, policies, and trusted-issuer
configuration. Prepare policies with the
[Agama Lab Policy Designer](https://cloud.gluu.org/agama-lab/dashboard/policy-designer)
and follow the [Cedar Archive format](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#cedar-archive-cjar-format)
for packaging.

#### Load a Cedar Archive from a URL

Set `CEDARLING_POLICY_STORE_URI` as shown in [Initialization](#initialization).
Cedarling downloads the archive while creating the instance. Use an absolute
HTTP(S) URL. For browser downloads, use the application's origin or configure the
archive server's CORS headers to allow the application origin.

#### Load a Cedar Archive from Bytes

Use `initFromArchiveBytes()` instead of `init()` when your application controls
the download. This browser example reuses `config` from Initialization and
fetches an archive through a same-origin application endpoint:

```ts
import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";

let policyArchiveBytes;
let cedarling;
try {
  const response = await fetch("/api/policy-store");
  if (!response.ok) throw new Error("Policy-store download failed");
  policyArchiveBytes = new Uint8Array(await response.arrayBuffer());
  cedarling = await initFromArchiveBytes(config, policyArchiveBytes);
} catch (error) {
  console.error("Policy-store download or initialization failed", error);
  throw error;
}
```

The archive bytes supply the policy store; `config` supplies the other bootstrap
properties. The package still loads and initializes its own WASM binary. In
Node.js, use an absolute download URL. Keep upstream policy-store credentials on
a trusted server; browser configuration is visible to its users.

### Authorization

Choose token-based authorization when identity comes from JWTs, or unsigned
authorization when the application supplies a trusted principal. Both methods
accept a JSON-string request and return a promise that resolves to an authorization
result containing `decision` and `request_id`.

#### Token-Based Authorization (Multi-Issuer)

Use `authorizeMultiIssuer()` to evaluate tokens from one or more issuers. Your
policy store must define the token mappings, trusted issuers, resource schema,
action, and context used below. Configure token validation for those issuers;
the initialization example retains Cedarling's validation defaults.

1. Prepare the tokens.

   Obtain `acmeAccessToken` and `dolphinAccessToken` from your application's
   authentication flows. Each `mapping` must match its policy-store token
   metadata; each `payload` is the actual JWT string.

   ```ts
   const tokens = [
     { mapping: "Acme::Access_Token", payload: acmeAccessToken },
     { mapping: "Dolphin::Access_Token", payload: dolphinAccessToken },
   ];
   ```

2. Define the resource.

   This example targets a document. Its type and attributes must match your schema.

   ```ts
   const resource = {
     cedar_entity_mapping: { entity_type: "Task::Document", id: "document-1" },
     owner: "alice",
   };
   ```

3. Define the action.

   ```ts
   const action = 'Task::Action::"Read"';
   ```

4. Define the context.

   Supply request-specific data required by your policies and schema.

   ```ts
   const context = { tenant: "example" };
   ```

5. Build the request.

   ```ts
   const request = { tokens, action, resource, context };
   ```

6. Perform authorization and handle the decision.

   ```ts
   try {
     const result = await cedarling.authorizeMultiIssuer(JSON.stringify(request));
     console.log(result.decision ? "Allowed" : "Denied", result.request_id);
   } catch (error) {
     console.error("Authorization failed", error);
   }
   ```

#### Unsigned Authorization

Use `authorizeUnsigned()` when your application supplies the principal and its
attributes. Obtain those values from trusted application state. The policy store
for this example must define `Task::User` with a `role`, `Task::Document` with an
`owner`, and the `Read` action with the `tenant` context field.

1. Prepare the principal.

   ```ts
   const principal = {
     cedar_entity_mapping: { entity_type: "Task::User", id: "alice" },
     role: "member",
   };
   ```

2. Define the resource.

   ```ts
   const resource = {
     cedar_entity_mapping: { entity_type: "Task::Document", id: "document-1" },
     owner: "alice",
   };
   ```

3. Define the action.

   ```ts
   const action = 'Task::Action::"Read"';
   ```

4. Define the context.

   ```ts
   const context = { tenant: "example" };
   ```

5. Build the request.

   ```ts
   const request = { principal, action, resource, context };
   ```

6. Perform authorization and handle the decision.

   ```ts
   try {
     const result = await cedarling.authorizeUnsigned(JSON.stringify(request));
     console.log(result.decision ? "Allowed" : "Denied", result.request_id);
   } catch (error) {
     console.error("Authorization failed", error);
   }
   ```

For both methods, `decision: false` is a valid denial. Malformed requests reject
the call. Multi-issuer authorization can continue with tokens that pass validation,
so policies must require the tokens needed for access. Perform the protected
operation only after receiving `decision: true`; enforce server-side access at the
server boundary.

#### Batch Authorization

Evaluate several resources with one principal or token set. Each item contains
an action, resource, and optional context. Reuse the values from the relevant
walkthrough:

```ts
const items = [
  { action, resource, context },
  {
    action,
    resource: {
      ...resource,
      cedar_entity_mapping: { entity_type: "Task::Document", id: "document-2" },
    },
    context,
  },
];
```

For the unsigned walkthrough:

```ts
let batch;
try {
  batch = await cedarling.authorizeUnsignedBatch(
    JSON.stringify({ principal, items }),
  );
} catch (error) {
  console.error("Batch authorization failed", error);
  throw error;
}
```

For the token-based walkthrough, use this call instead:

```ts
let batch;
try {
  batch = await cedarling.authorizeMultiIssuerBatch(
    JSON.stringify({ tokens, items }),
  );
} catch (error) {
  console.error("Batch authorization failed", error);
  throw error;
}
```

After a successful batch call, inspect each item in the same order as the
submitted `items`:

```ts
for (const item of batch.results) {
  if (item.is_ok) {
    console.log(item.unwrap().decision ? "Allowed" : "Denied");
  } else {
    console.error(item.error?.category, item.error?.message);
  }
}
```

A valid denial has `is_ok === true` and a false decision. A per-item build error
has `is_ok === false`. The returned `batch_id` correlates the batch's decision logs.

### Logging

The initialization example enables memory logging and retains entries for up to 120
seconds. Inside either authorization walkthrough's `try` block, use the returned
request ID to inspect its logs:

```ts
const logs = cedarling.getLogsByRequestId(result.request_id);
console.log(logs);
```

Log records are plain JavaScript objects. To retrieve and clear all retained logs
when your application is ready to consume them:

```ts
try {
  const retainedLogs = cedarling.popLogs();
  console.log(retainedLogs);
} catch (error) {
  console.error("Log retrieval failed", error);
}
```

### Context Data

Store application-scoped data for policy evaluation, then retrieve or remove it:

```ts
try {
  cedarling.pushDataCtx("user:alice", { plan: "pro" }, 3600n);
  const value = cedarling.getDataCtx("user:alice");
  console.log(value);
  cedarling.removeDataCtx("user:alice");
} catch (error) {
  console.error("Context-data operation failed", error);
}
```

The optional TTL is a `bigint` number of seconds; omitting it uses the configured
default. Retrieval returns `null` when a value is absent or expired.

### Trusted Issuer Readiness

Before token-based authorization, inspect which configured issuers loaded and
which failed:

```ts
console.log("Loaded issuers", cedarling.loadedTrustedIssuerIds());
console.log("Failed issuers", cedarling.failedTrustedIssuerIds());
```

To check one issuer, pass its configured identifier to
`cedarling.isTrustedIssuerLoadedByName(issuerId)`.

### Policy Annotations

Annotations attach metadata such as descriptions to policies using
`@key("value")`. They describe policies independently of their authorization rules.
For example, a policy in your archive can include:

```cedar
@description("Allows Alice to read document-1")
permit (
    principal == Task::User::"alice",
    action == Task::Action::"Read",
    resource == Task::Document::"document-1"
);
```

Inside the unsigned walkthrough's `try` block, retrieve descriptions for the
determining policy IDs returned in the result:

```ts
const descriptions = cedarling.annotationValues(
  result.response.diagnostics.reason,
  "description",
);
console.log(descriptions);
```

See the [Cedar annotation reference](https://docs.cedarpolicy.com/policies/syntax-policy.html#annotations)
for annotation syntax and behavior.

### Shutdown

When the application finishes using a Cedarling instance, shut it down:

```ts
try {
  await cedarling.shutDown();
} catch (error) {
  console.error("Cedarling shutdown failed", error);
}
```

### Other Integration Paths

#### Edge Deployments

Use the ESM-only `./edge` entry in hosts whose deployment tool bundles the package's
WASM binary as a precompiled WebAssembly module. In Cloudflare Workers, initialize inside
the [request context](https://developers.cloudflare.com/workers/runtime-apis/request/#the-request-context)
so policy downloads can run:

```ts
import { init } from "@janssenproject/cedarling_wasm/edge";

export default {
  async fetch() {
    let cedarling;
    try {
      cedarling = await init({
        CEDARLING_APPLICATION_NAME: "edge-api",
        CEDARLING_POLICY_STORE_URI: "https://example.com/policy-store.cjar",
      });
      return new Response("Cedarling initialized");
    } catch (error) {
      console.error("Cedarling initialization failed", error);
      return new Response("Internal server error", { status: 500 });
    } finally {
      try {
        await cedarling?.shutDown();
      } catch (error) {
        console.error("Cedarling shutdown failed", error);
      }
    }
  },
};
```

#### Manual Bundler Integration

For application-controlled WASM loading, configure your ESM bundler to emit the
package's `./wasm` export as a URL-addressable binary asset. Configure the server
to serve it with the `application/wasm` content type. The example below reuses
`config` and `policyArchiveBytes` from
[Load a Cedar Archive from Bytes](#load-a-cedar-archive-from-bytes), replacing
that section's initialization call:

```ts
import initWasm, {
  initFromArchiveBytes,
} from "@janssenproject/cedarling_wasm/manual";
import wasmUrl from "@janssenproject/cedarling_wasm/wasm";

let cedarling;
try {
  await initWasm({ module_or_path: wasmUrl });
  cedarling = await initFromArchiveBytes(config, policyArchiveBytes);
} catch (error) {
  console.error("WASM loading or Cedarling initialization failed", error);
  throw error;
}
```

Configure the `.wasm` import to return an asset URL; the exact rule or plugin
depends on your bundler.

## Upgrading Existing Applications

Update calls such as `authorize_unsigned()` to `authorizeUnsigned()`,
`shut_down()` to `shutDown()`, and `json_string()` to `jsonString()`.
Bootstrap keys (`CEDARLING_*`), request JSON, result/data fields such as
`request_id`, and serialized JSON retain their names.

## See Also

- [JavaScript tutorial](https://docs.jans.io/stable/cedarling/tutorials/javascript/)
- [Full API reference](https://docs.jans.io/stable/cedarling/tutorials/javascript/#defined-api)
- [Bootstrap-property reference](https://docs.jans.io/stable/cedarling/reference/cedarling-properties/)
- [Cedar Archive format](https://docs.jans.io/nightly/cedarling/reference/cedarling-policy-store/#cedar-archive-cjar-format)
- [Maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/js/docs/maintainer.md)
