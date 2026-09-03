# Cedarling JavaScript SDK

This directory contains the caller-facing implementation of
`@janssenproject/cedarling`. The package is private and is not yet intended for
publication.

## Included API

- `createCedarling(options)`
- `authorizeUnsigned(request)`
- `authorizeMultiIssuer(request)`
- `context` data operations
- retained `logs` operations
- `issuers` readiness queries
- `shutDown()`
- One `CedarlingError` and `Result<T>` contract

Configuration accepts inline, URL, archive-byte, and application-loader policy
sources, plus the typed and raw-bootstrap option forms.

`authorizeMultiIssuer` is the token-validating authorization method. It accepts
one or more mapped tokens; its name does not require multiple token issuers.

## Development

Node.js 20.19 or newer and the sibling Cedarling WASM package are required.

```bash
npm ci
npm run check
```

The root export selects the Node or browser loader. The current test command
validates the complete client under Node. Browser, Bun, Deno, and CommonJS
hosts are outside this local verification command.

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

## Current boundaries

CommonJS output, cross-runtime test runners, packaging, publication, and CI
automation are not part of this package configuration.
