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

## Run with Docker Compose

Docker builds the unpublished Cedarling WASM and JavaScript packages from the
local `jans-cedarling` sources, installs the resulting tarballs in the selected
example, and starts that example with an isolated development IdP. The Docker
path requires only Docker with Compose on the host; a cold build can take about
15 minutes because it compiles Cedarling from Rust source. Cached builds are
substantially faster.

Run one profile at a time from this directory. The profiles intentionally share
the canonical loopback origins and ports, so stop the active profile before
starting another one.

| Profile | Command | Result |
| --- | --- | --- |
| React + Express | `docker compose --profile react up --build` | Open `http://localhost:3000`; API at `http://localhost:8080` |
| Next.js | `docker compose --profile nextjs up --build` | Open `http://localhost:3000` |
| Hono on Bun | `docker compose --profile hono-bun up --build` | Open `http://localhost:3000`; API at `http://localhost:3001` |
| Hono on Deno | `docker compose --profile hono-deno up --build` | Open `http://localhost:3000`; API at `http://localhost:3001` |
| Hono on Wrangler | `docker compose --profile hono-cloudflare up --build` | Open `http://localhost:3000`; Worker simulation at `http://localhost:8787` |
| Cloudflare bundle check | `docker compose --profile hono-cloudflare-build up --build --abort-on-container-exit --exit-code-from hono-cloudflare-build` | Finite job; success is exit code 0 |
| Electron validation | `docker compose --profile electron-build up --build --abort-on-container-exit --exit-code-from electron-build` | Finite lint, type-check, test, renderer-bundle, and build job; success is exit code 0 |

### How Docker obtains Cedarling before npm publication

The repository root is a deliberately filtered build context so Docker can use
both these examples and the required `jans-cedarling` Rust sources without
copying host dependencies or generated outputs. A shared artifact stage:

1. installs the required Rust, WASM, Protobuf, Clang, Node.js, and npm toolchain;
2. builds `cedarling_wasm` from clean source with the locked Web target;
3. builds `cedarling_js` and stages exact-version SDK and WASM tarballs; and
4. installs those two tarballs into the selected example without modifying its
   manifest or lockfile.

The runtime-specific stages then build and validate the selected example before
copying only its required runtime files into the final image. BuildKit reuses
the expensive Cedarling artifact stage across profiles when its inputs have not
changed.

After both `@janssenproject/cedarling` and
`@janssenproject/cedarling_wasm` are published at coordinated exact versions,
the migration is:

1. update every example manifest and lockfile from the npm registry, ensuring
   the published SDK names the matching registry WASM dependency;
2. replace Docker's local-tarball installation with normal `npm ci`;
3. remove the Rust/WASM artifact builder and reduce the Docker context to the
   examples directory; and
4. remove the temporary local installer only after native workflows and all
   profile builds pass using the published packages.

Do not remove the source-build path merely because one package is published;
both coordinated packages and the consumer lockfiles must be available first.

### Hono browser UI

The runnable Bun, Deno, and Wrangler profiles reuse the React TaskApp as a
separate Nginx container. React is not copied into a Hono backend image. Each
frontend is compiled with the selected API URL, waits for that API to become
healthy, and prints the clickable `http://localhost:3000` URL when Nginx starts.

For a browser profile, sign in as `bob`, `alice`, or `charlie` with any
non-empty password, then create and delete a task to exercise the signed OIDC
and Cedarling-protected path. The IdP is always available at
`http://localhost:9090`. Its users, keys, registrations, sessions, and the task
stores are in memory and are reset when the containers stop.

Stop a runnable profile and remove its containers with its matching profile
name, for example:

```bash
docker compose --profile react down --remove-orphans
```

The Wrangler profile is a local simulation, not proof of a Cloudflare
production deployment. The Electron profile does not package or display the
desktop GUI; use the native Electron commands for the real desktop OIDC flow.
The Docker topology and development IdP are local demonstration infrastructure,
not production deployment templates.

## Run without Docker

The existing native workflow remains supported and is recommended for active
development and for opening the Electron desktop UI.

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
