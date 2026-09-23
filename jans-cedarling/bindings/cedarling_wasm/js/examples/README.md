# Cedarling JavaScript examples

These examples run the same Cedarling workflow in Node.js, browsers, and edge applications. [`shared/run-cedarling.mjs`](shared/run-cedarling.mjs) contains the shared service calls; each entry handles its runtime-specific loading.

## Choose an example

| Environment             | Source                                     | Command                  |
| ----------------------- | ------------------------------------------ | ------------------------ |
| Node.js ESM             | [`node/esm.mjs`](node/esm.mjs)             | `npm run node:esm`       |
| Node.js CommonJS        | [`node/commonjs.cjs`](node/commonjs.cjs)   | `npm run node:commonjs`  |
| React with Vite         | [`browser/react-vite`](browser/react-vite) | `npm run dev`            |
| Browser webpack         | [`browser/webpack`](browser/webpack)       | `npm run dev:webpack`    |
| Cloudflare `/authorize` | [`edge/cloudflare`](edge/cloudflare)       | `npm run dev:cloudflare` |

The workflow covers authorization, annotations, context data, trusted issuers, memory logs, and shutdown. Its readable policy store is in [`fixtures/policy-store.yml`](fixtures/policy-store.yml).

Node examples print each operation under a service heading. Browser examples render the same canonical Cedarling results on the page and mirror them to grouped browser-console messages.

Authorization responses expose the decision, contributing policy IDs, evaluation errors, and a request ID that correlates retained logs. See the official [authorization](https://docs.jans.io/stable/cedarling/reference/cedarling-authz/) and [log field](https://docs.jans.io/stable/cedarling/reference/cedarling-logs/) references.

## Run every example

Use Node.js 22, 24, or 26 and ensure an Info-ZIP-compatible `zip` executable is
available on `PATH`. Install the parent package dependencies first because the
examples reuse its policy-archive builder, then install and verify the examples:

```sh
cd jans-cedarling/bindings/cedarling_wasm/js
npm ci --ignore-scripts
cd examples
npm ci
npx playwright install chromium
npm run check
```

`npm run check` builds the archive, exercises both Node formats and browser applications, and starts the Worker locally.

To create only the Cedar archive, run `npm run build:policy-archive`.

## Security note

The multi-issuer fixture uses locally generated tokens and disables signature and status validation solely to keep the examples deterministic and offline. Production applications must keep token validation enabled, configure trusted issuers, and obtain tokens from their authentication flows. Perform a protected operation only when Cedarling returns `decision: true`. The examples display complete synthetic results; production logs should exclude unnecessary tokens, claims, and sensitive context.
