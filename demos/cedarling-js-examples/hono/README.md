# Cedarling Hono backend

This API runs the same TaskApp and Cedarling integration on Cloudflare Workers,
Bun, and Deno. It is API-only and is designed to be exercised through the
shared React frontend.

## What it demonstrates

- one Hono application shared by three runtimes
- Cedarling's `workerd` export on Cloudflare Workers
- Cedarling's Node-family adapter on Bun and Deno
- unsigned and signed task authorization
- URL-based policy loading without global `fetch` patches or filesystem
  polyfills
- cached initialization that can retry after a failed first attempt

## Requirements

- the [shared development IdP](../common/README.md) running on port `9090`
- published `@janssenproject/cedarling@1.0.0` and
  `@janssenproject/cedarling_wasm@1.0.0` packages, or the local staged-package workflow
  described in the [examples overview](../README.md#sdk-package-availability)
- one of:
  - Node.js 20.19+ and Wrangler for Cloudflare local development
  - Bun
  - Deno with npm package support

Install once:

```bash
npm run install:sdk:local
npm run typecheck
```

## Run a backend

| Runtime | Command | API origin |
| --- | --- | --- |
| Cloudflare Workers local runtime | `npm run dev:cf` | `http://localhost:8787` |
| Bun | `npm run dev:bun` | `http://localhost:3001` |
| Deno | `npm run dev:deno` | `http://localhost:3001` |

`PORT` can override port `3001` for Bun or Deno. Wrangler's port can be
overridden with its CLI options.

The Deno command grants only the network, read, environment, and FFI
permissions used by the current Node-family WASM adapter.

## Connect the React frontend

In a second terminal, choose the origin from the table above:

```bash
cd ../react-nodejs/frontend
npm install
VITE_BACKEND_URL=http://localhost:8787 npm run dev
```

For Bun or Deno, use:

```bash
VITE_BACKEND_URL=http://localhost:3001 npm run dev
```

Open `http://localhost:3000`. The frontend uses the selected Hono origin for
tasks, policy-store configuration, and Cedarling test configuration.

## API

| Method | Path | Authorization | Purpose |
| --- | --- | --- | --- |
| `GET` | `/config/policy-store` | none | proxy the shared TaskApp policy store |
| `GET` | `/config/test-config` | none | proxy the shared development scenario |
| `GET` | `/tasks` | `x-user-id` or bearer token | list tasks |
| `POST` | `/tasks` | `x-user-id` or bearer token | create a task |
| `PUT` | `/tasks/:id` | `x-user-id` or bearer token | update an allowed task |
| `DELETE` | `/tasks/:id` | `x-user-id` or bearer token | delete an allowed task |

Basic unsigned smoke test:

```bash
curl http://localhost:8787/tasks -H 'x-user-id: bob'
```

## Verify

```bash
npm run typecheck
npm run deploy:cf -- --dry-run
```

Wrangler recognizes the generated WASM module directly; no custom WASM module
rule is configured.

## Known limitations

The example currently points to the loopback IdP at `http://localhost:9090`.
Replace that development constant with an environment binding and a reachable,
authenticated HTTPS policy/configuration service before deployment.

The current Cloudflare upload is approximately 6 MiB compressed. It exceeds the
Workers Free plan's 3 MiB compressed limit but remains below the Paid plan's
10 MiB limit. Review the current
[Cloudflare Workers limits](https://developers.cloudflare.com/workers/platform/limits/)
before deploying. Tasks live only in the runtime's memory and can disappear
between isolate instances.
