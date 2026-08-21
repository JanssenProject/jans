# Cedarling JavaScript SDK

Add fast, local, policy-based authorization to JavaScript applications with
`@janssenproject/cedarling`. The SDK provides one client API for server-side
JavaScript and browsers, with typed configuration, structured decisions, and
consistent error handling.

Use Cedarling when your application needs to answer questions such as:

- Can this user read or update this resource?
- Does this signed access token grant the requested action?
- Which policy allowed or denied the request?
- Is a trusted identity provider ready for token validation?

## Module formats

The package supports ES modules:

```ts
import { createCedarling } from "@janssenproject/cedarling";
```

And CommonJS on Node.js:

```js
const { createCedarling } = require("@janssenproject/cedarling");
```

Supported environments include Node.js 22, 24, and 26, Bun, Deno, and modern
browsers. Import from the package root in those environments.

Cloudflare Workers and Vercel Edge use the edge entry, without additional
asset setup:

```ts
import { createCedarling } from "@janssenproject/cedarling/edge";
```

The package uses modern `exports`; TypeScript projects should use `node16`,
`nodenext`, or `bundler` module resolution.

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

Authorization results contain a request ID and policy diagnostics:

```ts
interface AuthorizationDecision {
  decision: boolean;
  requestId: string;
  diagnostics: {
    reasons: readonly string[];
    errors: readonly CedarlingError[];
  };
}
```

Use `error.code`, `error.operation`, and optional `error.path` for application
logic. The SDK exports no error constructor, so do not depend on `instanceof`.

```ts
if (!result.ok) {
  switch (result.error.code) {
    case "INPUT_OUT_OF_RANGE":
      console.error("Invalid value at", result.error.path);
      break;
    case "CLIENT_CLOSED":
      console.error("The Cedarling client has shut down");
      break;
    default:
      console.error(result.error.code, result.error.operation);
  }
}
```

Policy-evaluation diagnostics use the same error shape. Read `message`
explicitly because standard `Error` properties are not all enumerable:

```ts
if (result.ok) {
  for (const error of result.value.diagnostics.errors) {
    console.error({
      code: error.code,
      operation: error.operation,
      message: error.message,
      details: error.details,
    });
  }
}
```

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

## Advanced configuration

Typed options cover logging, authorization, context storage, token validation,
token caching, issuer loading, HTTP behavior, and Lock integration. They are
validated and copied when the client is created.

For direct access to Cedarling bootstrap properties, use the mutually
exclusive advanced form:

```ts
const initialized = await createCedarling({
  bootstrapProperties: {
    CEDARLING_APPLICATION_NAME: "task-api",
    CEDARLING_POLICY_STORE_URI:
      "https://configuration.example/policy-store.cjar",
    CEDARLING_LOG_TYPE: "off",
  },
});
```

Do not combine `bootstrapProperties` with typed configuration fields.

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
