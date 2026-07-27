# Cedarling JavaScript examples

These examples demonstrate the same TaskApp authorization policy across browser,
server, edge, worker, and desktop JavaScript runtimes. They share one local
OpenID Connect provider, one Cedar policy store, and the same Bob/Alice/Charlie
task scenarios so runtime integration can be compared without changing the
application model.

## Example index

| Example | Cedarling runtime | Application | README |
| --- | --- | --- | --- |
| Shared IdP | Node.js | OIDC discovery, DCR, signed tokens, logout, and policy fixtures | [Open](common/README.md) |
| React + Express | Browser and Node.js | Reference Vite frontend and Express backend | [Overview](react-nodejs/README.md) · [Backend](react-nodejs/backend/README.md) · [Frontend](react-nodejs/frontend/README.md) |
| Hono | Cloudflare Workers, Bun, and Deno | API compatible with the reference frontend | [Open](hono/README.md) |
| Next.js | Node.js and Vercel Edge | App Router UI/API with server-side DCR and Authorization Code + PKCE | [Open](vercel-nextjs/README.md) |
| Electron | Renderer and main process | Desktop TaskApp with process-specific Cedarling integration | [Open](electron/README.md) |

## How the examples fit together

```text
                         +---------------------------+
                         | Shared development IdP    |
                         | OIDC + DCR + policy URLs  |
                         | http://localhost:9090     |
                         +-------------+-------------+
                                       |
             +-------------------------+-------------------------+
             |                         |                         |
   +---------v---------+     +---------v---------+     +---------v---------+
   | React frontend    |     | Next.js app       |     | Electron app      |
   | Cedarling browser |     | Node + Edge       |     | renderer + main   |
   +---------+---------+     +-------------------+     +-------------------+
             |
       select one API
             |
   +---------+-------------------------------+
   | Express :8080 | Hono CF :8787 | Hono Bun/Deno :3001 |
   +-----------------------------------------------------+
```

The React frontend is deliberately reusable. `VITE_BACKEND_URL` selects
Express, Hono on Cloudflare Workers, Hono on Bun, or Hono on Deno without
changing application code.

## Requirements

- Node.js 20.19 or newer and npm 10 or newer
- Bun, Deno, or Wrangler only for the corresponding Hono target
- Chromium installed by Playwright only when running Next.js browser tests

## SDK package availability

Every manifest pins `@janssenproject/cedarling` to `1.0.0`. The SDK release
artifact in turn pins `@janssenproject/cedarling_wasm` to the same exact
version. This removes
repository-relative `file:` dependencies and lets an example install
independently after both coordinated packages are published to the configured
npm registry.

At the time this example set was prepared, those `1.0.0` packages were not yet
available from the public npm registry. A normal standalone `npm install`
therefore requires the coordinated packages to be published first. Repository
contributors should use the local installer from the consuming example:

```bash
npm run install:sdk:local
```

The command stages both packages, installs stable tarball snapshots without
changing the example manifest or lockfile, and removes the temporary artifacts.
It avoids a symlink to the SDK checkout because SDK builds replace `dist`,
which can interrupt a running Vite or webpack development server.

To install dependencies for every example in one run:

```bash
cd jans-cedarling/bindings/cedarling_js/examples
node scripts/install.mjs
```

Publishing `@janssenproject/cedarling_wasm@1.0.0` before
`@janssenproject/cedarling@1.0.0` is the remaining distribution step. Do not
replace the exact versions with repository-local paths in committed example
manifests.

## Quick start

Start the shared IdP once:

```bash
cd common
npm install
npm start
```

Then choose an application:

```bash
# Reference Express API
cd react-nodejs/backend
npm run install:sdk:local
npm start

# In another terminal: reference React UI
cd react-nodejs/frontend
npm run install:sdk:local
npm run dev
```

See the linked README for Hono, Next.js, or Electron commands and runtime-
specific requirements.

## Download only the examples

Git cannot clone an arbitrary subdirectory as an independent repository, but a
partial clone plus sparse checkout keeps only the examples in the working tree
and downloads file contents on demand:

```bash
git clone --filter=blob:none --no-checkout \
  https://github.com/JanssenProject/jans.git
cd jans
git sparse-checkout init --cone
git sparse-checkout set \
  jans-cedarling/bindings/cedarling_js/examples
git checkout main
```

See the official [`git clone --filter` documentation](https://git-scm.com/docs/git-clone)
and [`git sparse-checkout` documentation](https://git-scm.com/docs/git-sparse-checkout).

## Known limitations

| Area | Limitation |
| --- | --- |
| Shared IdP | In-memory development fixture; relaxed Cedarling JWT checks; not suitable for production |
| Package install | Exact `1.0.0` SDK/WASM packages must be published before public-registry standalone installs work |
| React OIDC | Development implicit flow and browser token storage; use the Next.js server-side PKCE example as the stronger session reference |
| Cloudflare Workers | Current WASM bundle exceeds the Workers Free compressed-size limit |
| Vercel Edge | The current generated WASM assets exceed Vercel Edge compressed-size limits; local simulation is the integration proof |
| Electron DevTools | Native menu and keyboard toggling is currently unreliable and deferred; DevTools do not auto-open |
| Task storage | All task stores are in memory and reset when their process/isolate restarts |

Each example README lists its runtime-specific build or test commands.
