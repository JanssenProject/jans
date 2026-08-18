# Cedarling JavaScript SDK

This directory contains the first independently runnable integration slice of
`@janssenproject/cedarling`. The package remains private while the remaining SDK
services, runtime qualification, and release automation are reviewed in later
pull requests.

## Included API

- `createCedarling(options)`
- `authorizeUnsigned(request)`
- `authorizeMultiIssuer(request)`
- `shutDown()`
- One `CedarlingError` and `Result<T>` contract

`authorizeMultiIssuer` is the token-validating authorization method. It accepts
one or more mapped tokens; its name does not require multiple token issuers.

## Development

Node.js 20.19 or newer and the sibling Cedarling WASM package are required.

```bash
npm ci
npm run check
```

The root export selects the Node or browser loader. This slice validates the
Node path; broader runtime qualification is added by later SDK pull requests.

## Unsigned authorization

```ts
import { createCedarling } from "@janssenproject/cedarling";

const initialized = await createCedarling({
  applicationName: "task-api",
  policyStore: { type: "inline", document: policyStoreDocument },
});

if (!initialized.ok) throw initialized.error;
const cedarling = initialized.value;

try {
  const result = await cedarling.authorizeUnsigned({
    principal: { type: "User", id: "alice" },
    action: { namespace: "Task", id: "Read" },
    resource: { type: "Task", id: "task-1" },
  });

  if (!result.ok) throw result.error;
  console.log(result.value.decision);
} finally {
  await cedarling.shutDown();
}
```

Replace `policyStoreDocument` with a parsed Cedarling policy-store document.
Every public operation returns `Result<T>`; a successful decision of `false` is
a policy denial, not an SDK error.

## Deferred from this slice

Context data, retained logs, issuer observation, URL/archive/loader policy
sources, CommonJS output, cross-runtime test runners, packaging, publication,
and CI automation are introduced by the dependent pull requests.
