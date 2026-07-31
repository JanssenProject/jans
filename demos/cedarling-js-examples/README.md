# Cedarling JavaScript examples

These examples apply one TaskApp policy across browser, server, edge, worker,
and desktop JavaScript runtimes. They share a local OIDC provider, strict RS256
validation, one policy store, and the Bob, Alice, and Charlie task model.

| Example | Runtime | Guide |
| --- | --- | --- |
| Shared development IdP | Node.js | [common](common/README.md) |
| React + Express | Browser + Node.js | [overview](react-nodejs/README.md) |
| Hono | Cloudflare Workers, Bun, Deno | [hono](hono/README.md) |
| Next.js | Node.js + Vercel Edge | [next](vercel-nextjs/README.md) |
| Electron | sandboxed renderer + main | [electron](electron/README.md) |

The browser-visible applications use shared design tokens from
`common/ui/theme.css`. Runtime-specific UI code stays inside each example so
the packages remain independently understandable and runnable. The local IdP
uses the bundled development interactions from `oidc-provider`.

All applications demonstrate both explicit unsigned users and signed OIDC
UserInfo JWTs. Signed paths validate RS256 signatures. Create, update, and
delete policies bind the task owner to the authenticated identity.

## How to read an example

The source comments follow the same authorization path in every runtime:

1. initialize one reusable Cedarling client;
2. construct a principal or map a signed UserInfo token;
3. construct the resource from application-owned data;
4. distinguish SDK failure from a policy deny;
5. enforce the decision before mutation; and
6. shut down long-lived clients when the runtime provides a lifecycle hook.

## Requirements

- Node.js 20.19 or newer and npm 10 or newer
- Bun, Deno, or Wrangler only for the selected Hono runtime
- Chromium only for the Next.js Playwright suite

## Install from this repository

Each application pins `@janssenproject/cedarling` to an exact version. Until
the coordinated SDK and WASM packages are available from npm, stage and install
the current repository source without modifying manifests or lockfiles:

```bash
node scripts/install-example.mjs --all
```

From one application directory, use its `npm run install:sdk:local` command.
After publication, a normal `npm ci` installs the same exact versions.

## Quick start

Start the shared IdP:

```bash
cd common
npm ci
npm start
```

Then start one application. For the reference web stack:

```bash
cd react-nodejs/backend
npm run install:sdk:local
npm start
```

```bash
cd react-nodejs/frontend
npm run install:sdk:local
npm run dev
```

Open `http://localhost:3000`. Each application README lists its environment,
test, type-check, and build commands.

## Security boundary

The examples never disable signature, status, or schema validation. Tokens are
not placed in URLs or persistent browser storage. The IdP, registrations,
sessions, Cedarling clients, and task stores are still in-memory development
fixtures and are not production deployment templates.

Cloudflare and Vercel deployment size limits can be lower than the current WASM
bundle. Treat their local runtime builds as integration proof and check current
platform limits before deployment.
