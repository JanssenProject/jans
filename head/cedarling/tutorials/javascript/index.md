# Using Cedarling WebAssembly (WASM) in JavaScript

Janssen's Cedarling npm package includes the WebAssembly binary for evaluating
Cedar policies in JavaScript or TypeScript. It runs in browsers, Node.js, and
edge workers. In the Node.js examples below, Alice first requests a document
using an identity supplied by the application. The second example uses signed
tokens from two issuers.

## Installation

With Node.js 22, 24, or 26 installed, run these commands in an empty directory.

```sh
npm init -y
npm install @janssenproject/cedarling_wasm
```

<span id="usage"></span>
<span id="authorization"></span>

## Unsigned authorization

Save this as `unsigned.mjs`. Its inline YAML defines a policy store with a single
policy that lets Alice read `document-1`. The loop asks Cedarling whether she can
read each of two documents.

```js
import { init } from "@janssenproject/cedarling_wasm";

const policyStore = `
cedar_version: 4.5.0
policy_stores:
  abc123def456:
    name: Document access
    schema:
      encoding: none
      content_type: cedar
      body: |
        namespace Example {
          entity User;
          entity Document;
          action "Read" appliesTo { principal: [User], resource: [Document], context: {} };
        }
    policies:
      allow-alice:
        description: Alice may read document-1.
        policy_content:
          encoding: none
          content_type: cedar
          body: |
            @description("Alice may read document-1")
            permit(principal == Example::User::"alice",
                   action == Example::Action::"Read",
                   resource == Example::Document::"document-1");
`;

const cedarling = await init({
  CEDARLING_APPLICATION_NAME: "document-reader",
  CEDARLING_POLICY_STORE_LOCAL: policyStore,
  CEDARLING_LOG_TYPE: "memory",
  CEDARLING_LOG_LEVEL: "INFO",
  CEDARLING_LOG_TTL: 120,
});

const principal = {
  cedar_entity_mapping: { entity_type: "Example::User", id: "alice" },
};

function readDocument(id) {
  return {
    action: 'Example::Action::"Read"',
    resource: { cedar_entity_mapping: { entity_type: "Example::Document", id } },
    context: {},
  };
}

try {
  for (const id of ["document-1", "document-2"]) {
    const request = { principal, ...readDocument(id) };
    const result = await cedarling.authorizeUnsigned(JSON.stringify(request));
    try {
      console.log(id, result.decision ? "allow" : "deny");
      const logs = cedarling.getLogsByRequestId(result.request_id);
      console.log("Request has logs:", logs.length > 0);
    } finally {
      result.free();
    }
  }
} finally {
  await cedarling.shutDown();
  cedarling.free();
}
```

Run `node unsigned.mjs`. You'll see `document-1 allow` and `document-2 deny`,
each followed by `Request has logs: true`. Both calls succeeded. Cedarling
denied the second request because no policy lets Alice read that document.
The binding takes JSON text. An invalid request rejects the promise, while a
denial returns a result with `decision === false`.

Take the principal from trusted application state. If you omit it or pass
`null`, Cedarling uses partial evaluation and denies the request if it still
needs the principal to reach a decision. If the resource lives on a server,
enforce the decision on the server that handles the request.

## Token-based authorization (multi-issuer)

Acme and Dolphin each issue an access token with an `org_id` claim. The caller
can read the resource only if both tokens are valid and both claims match the
resource's organization. In the request, `mapping` names the token's Cedar
entity type and `payload` holds the JWT.

Save [demo-issuers.mjs](../../assets/cedarling/wasm/demo-issuers.mjs){: download }
and [signed-policy.yml](../../assets/cedarling/wasm/signed-policy.yml){: download }
alongside `unsigned.mjs`. The demo runs both issuers on a local HTTP server.
Each gets its own signing key and issues a token that lasts five minutes.
Cedarling reads each issuer's discovery document, fetches its public key, and
checks the token's signature. In your application, use your identity providers'
HTTPS discovery URLs and the tokens from your login flows.

Save the example as `signed.mjs` in the same directory.

```js
import { readFile } from "node:fs/promises";
import { init } from "@janssenproject/cedarling_wasm";
import { startDemoIssuers } from "./demo-issuers.mjs";

const template = await readFile(new URL("./signed-policy.yml", import.meta.url), "utf8");
const demo = await startDemoIssuers();
try {
  const cedarling = await init({
    CEDARLING_APPLICATION_NAME: "organization-reader",
    CEDARLING_POLICY_STORE_LOCAL: template.replaceAll("DEMO_ORIGIN", demo.origin),
    CEDARLING_JWT_SIG_VALIDATION: "enabled",
    CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED: ["ES256"],
  });
  try {
    const item = {
      action: 'MultiIssuer::Action::"Read"',
      resource: {
        cedar_entity_mapping: { entity_type: "MultiIssuer::Resource", id: "document" },
        org_id: "example",
      },
      context: {},
    };
    for (const tokens of [demo.tokens, demo.tokens.slice(0, 1)]) {
      const request = { tokens, ...item };
      const result = await cedarling.authorizeMultiIssuer(JSON.stringify(request));
      try {
        console.log(tokens.length, "token(s):", result.decision ? "allow" : "deny");
      } finally {
        result.free();
      }
    }
  } finally {
    await cedarling.shutDown();
    cedarling.free();
  }
} finally {
  await demo.close();
}
```

Run `node signed.mjs`. The first call prints `2 token(s): allow`. The second
prints `1 token(s): deny` because it sends only Acme's token. Cedarling can
still evaluate a request when some tokens fail validation, using those that
passed. To see why Cedarling dropped a token you sent, check the [decision diagnostics](#decision-diagnostics).

## Initialization

`init(config)` loads the bundled WASM module and creates a Cedarling instance.
Pass the bootstrap properties as a plain object or a JavaScript `Map`, then
reuse that instance across requests. To try the snippets below, add them to
the corresponding example before it shuts down.

### Policy store sources (WASM)

#### Load an archive from a URL

Set `CEDARLING_POLICY_STORE_URI` to the absolute HTTPS URL of your Cedar archive.
For a signed archive, supply the certificate identity and OIDC issuer used to
sign it. In `enforce` mode, Cedarling starts only if the signature verifies.

```js
const config = {
  CEDARLING_APPLICATION_NAME: "document-reader",
  CEDARLING_POLICY_STORE_SIGSTORE_MODE: "enforce",
  CEDARLING_POLICY_STORE_SIGSTORE_CERT_IDENTITY: signerIdentity,
  CEDARLING_POLICY_STORE_SIGSTORE_CERT_OIDC_ISSUER: signerOidcIssuer,
};
const cedarling = await init({ ...config, CEDARLING_POLICY_STORE_URI: archiveUrl });
console.log(cedarling.policyStoreId());
```

`policyStoreId()` returns the loaded store's ID, or `undefined` if it has none.
Cedarling finds the signature bundle by appending `.sigstore.json` to the
archive URL.

#### Load archive bytes

If your application downloads the archive, pass its bytes as a `Uint8Array`.
Supply the bundle URL too when you enforce signature verification. In a
browser, the archive and bundle must each be on the application's origin or
served with CORS headers that allow that origin.

```js
import { initFromArchiveBytes } from "@janssenproject/cedarling_wasm";

const response = await fetch(archiveUrl);
if (!response.ok) throw new Error(`Policy download failed: ${response.status}`);
const bytes = new Uint8Array(await response.arrayBuffer());
const cedarling = await initFromArchiveBytes({
  ...config,
  CEDARLING_POLICY_STORE_SIGSTORE_BUNDLE_URI: bundleUrl,
}, bytes);
```

See [policy store formats](../reference/cedarling-policy-store.md) and
[bootstrap properties](../reference/cedarling-properties.md).

## Decision diagnostics

To keep a decision after you release the WASM result, call `result.jsonString()`
first and parse the JSON. In either walkthrough, `reason` lists the IDs of the
policies that determined the decision, and `errors` lists any evaluation
errors. For Alice's request to read the second document, no policy matches
and `reason` is empty.

```js
const decision = JSON.parse(result.jsonString());
const { reason, errors } = decision.response.diagnostics;
console.log(decision.request_id, decision.decision, reason);
for (const { id, error } of errors) console.error(id, error);
console.log(cedarling.annotationValues(reason, "description"));
console.log(cedarling.annotationsByPolicy(reason));
console.log(cedarling.annotationsMap(reason));
```

Use `annotationsByPolicy()` when you need to know which policy supplied an
annotation. If two policies use the same key, `annotationsMap()` keeps just
one value. On a multi-issuer result, each dropped token records its original
index, mapping, and the reason Cedarling excluded it. Call `toJSON()` to get
those details as a JavaScript object.

```js
for (const token of result.droppedTokens()) {
  try {
    console.log(token.toJSON());
  } finally {
    token.free();
  }
}
```

You can also read `token.index`, `token.mapping`, and `token.reason` directly.
The reason has a `kind`, a `message`, optional `detail`, and its own `toJSON()`
method. The decision JSON includes `dropped_tokens` only when Cedarling
excluded at least one token.

## Batch authorization

A batch lets you check several actions or resources for the same principal or
set of tokens. To try it with Alice's two documents, add this code inside the
outer `try` in the unsigned example.

```js
const items = [readDocument("document-1"), readDocument("document-2")];
const batch = await cedarling.authorizeUnsignedBatch(JSON.stringify({ principal, items }));
try {
  console.log("Batch ID:", batch.batch_id);
  for (const item of batch.results) {
    try {
      if (item.is_ok) {
        const result = item.unwrap();
        try {
          console.log(result.decision);
        } finally {
          result.free();
        }
      } else {
        const error = item.error;
        try {
          console.error(error.item_index, error.category, error.message);
        } finally {
          error.free();
        }
      }
    } finally {
      item.free();
    }
  }
} finally {
  batch.free();
}
```

The batch returns decisions in request order. Here it prints `true` for the
first document and `false` for the second. If Cedarling can't build an item,
that item has `is_ok === false` and an `error`. An empty `items` array rejects
the whole call. In the signed example, pass the tokens once for the batch.

```js
// In signed.mjs, after defining item:
const batch = await cedarling.authorizeMultiIssuerBatch(JSON.stringify({
  tokens: demo.tokens,
  items: [item, { ...item, resource: { ...item.resource, org_id: "other" } }],
}));
// Inspect batch.results as above before freeing the batch.
batch.free();
```

## Trusted issuer readiness

Run these checks after initialization in `signed.mjs`. If Acme or Dolphin
appears in the list of failed issuers, check its discovery document and
public keys when investigating the denial.

```js
console.log(cedarling.loadedTrustedIssuersCount(), "of", cedarling.totalIssuers());
console.log(cedarling.loadedTrustedIssuerIds(), cedarling.failedTrustedIssuerIds());
console.log(cedarling.isTrustedIssuerLoadedByName("Acme"));
console.log(cedarling.isTrustedIssuerLoadedByIss(demo.origin + "/Acme"));
```

## Logging

The unsigned example keeps logs in memory. Use `request_id` to find the
records for one decision, or a log ID to look up one record. Call `popLogs()`
when you're ready to collect and clear everything the logger still holds.

```js
const records = cedarling.getLogsByRequestId(result.request_id);
const decisions = cedarling.getLogsByRequestIdAndTag(result.request_id, "Decision");
const allDecisions = cedarling.getLogsByTag("Decision");
const [firstId] = cedarling.getLogIds();
if (firstId !== undefined) console.log(cedarling.getLogById(firstId));
const drained = cedarling.popLogs(); // returns and clears retained records
```

## Context data

Store a value under a key to make it available to policies through
`context.data`. You can give the value a lifetime in seconds as a JavaScript
`bigint`. Each request can also supply its own `context`.

```js
cedarling.pushDataCtx("user:alice", { plan: "pro" }, 120n);
console.log(cedarling.getDataCtx("user:alice")); // { plan: "pro" }
const entry = cedarling.getDataEntryCtx("user:alice");
if (entry) {
  console.log(entry.key, entry.value(), entry.expires_at);
  entry.free();
}
for (const stored of cedarling.listDataCtx()) {
  console.log(stored.jsonString());
  stored.free();
}
const stats = cedarling.getStatsCtx();
console.log(stats.entry_count, stats.capacity_usage_percent);
stats.free();
cedarling.removeDataCtx("user:alice"); // true if removed
```

If a key is missing or has expired, `getDataCtx()` returns `null` and
`getDataEntryCtx()` returns `undefined`. To clear every entry from the
instance, call `clearDataCtx()`. Both entry metadata and store statistics
have a `jsonString()` method.

## Metrics and shutdown

Set `CEDARLING_METRICS_COLLECTION: "enabled"` when you initialize Cedarling.
For local collection, call `drainMetrics()` to read the metrics for the current
interval and reset the counters. If you set `CEDARLING_LOCK_TELEMETRY_INTERVAL`,
Lock handles collection and the local call fails.

```js
const snapshot = cedarling.drainMetrics();
try {
  console.log(snapshot.interval_secs, snapshot.operational_stats.get("authz.requests_total"));
  console.log(snapshot.jsonString());
} finally {
  snapshot.free();
}
```

`operational_stats`, `policy_stats`, and `error_counters` are JavaScript maps.
The number in `interval_secs` tells you how long the collection interval lasted.
When your application is done, await `cedarling.shutDown()`, then call
`cedarling.free()`. Generated result, entry, and diagnostic wrappers have their
own `free()` method. Read the fields you need before releasing each wrapper.
For wrappers with `jsonString()`, you can keep a JSON copy. Generated wrappers
also implement `[Symbol.dispose]()` for runtimes with explicit resource
management.

## Choose a runtime

| Runtime | Package entry | How to initialize |
| --- | --- | --- |
| Node.js ESM or CommonJS | `@janssenproject/cedarling_wasm` | Use `import { init }` or `const { init } = require("@janssenproject/cedarling_wasm")`. Await `init(config)`. |
| Browser with Vite or webpack | `@janssenproject/cedarling_wasm` | Import `init` in your app. The bundler handles the WASM binary. |
| Edge worker with precompiled WASM support | `@janssenproject/cedarling_wasm/edge` | Import `init` from the edge entry. In Cloudflare Workers, call it inside a request handler so Cedarling can fetch the policy archive. |
| ESM bundler with custom asset loading | `@janssenproject/cedarling_wasm/manual` and `@janssenproject/cedarling_wasm/wasm` | Configure the bundler to emit the binary as an asset URL. Initialize the module once, then create a client. |

In the manual example, `wasmUrl` points to the binary emitted by the bundler.
Set `archiveUrl` to the HTTPS URL of your policy archive.

```js
import initWasm, { init } from "@janssenproject/cedarling_wasm/manual";
import wasmUrl from "@janssenproject/cedarling_wasm/wasm";

await initWasm({ module_or_path: wasmUrl });
const cedarling = await init({
  CEDARLING_APPLICATION_NAME: "document-reader",
  CEDARLING_POLICY_STORE_URI: archiveUrl,
});
```

If you already have a compiled WASM module or its bytes, initialize it
synchronously with `initSync({ module })`, then await `init(config)`. The root
entry exports `initWasm` as its default if you want to load WASM explicitly.
On the edge entry, `initWasm()` loads the bundled module. After initializing
the manual entry, you can also create clients with `Cedarling.new(config)` or
`Cedarling.newFromMap(configMap)`.

See the [runnable Node.js, React/Vite, webpack, and Cloudflare examples](https://github.com/JanssenProject/jans/tree/main/jans-cedarling/bindings/cedarling_wasm/js/examples).

## API reference

The package includes TypeScript declarations for its public methods and result
types. For signatures and return values, see the [JavaScript/TypeScript API reference](https://janssenproject.github.io/developer-docs/cedarling-js/index.html).

<span id="build-from-source"></span>

To build the binding from source, follow the [package maintainer guide](https://github.com/JanssenProject/jans/blob/main/jans-cedarling/bindings/cedarling_wasm/js/docs/maintainer.md).
