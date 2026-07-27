# Cedarling React frontend

This Vite React application is the shared browser UI for the Express and Hono
TaskApp backends. Cedarling also runs directly in the browser to calculate the
update and delete controls shown for each task.

## What it demonstrates

- the browser export of `@janssenproject/cedarling`
- unsigned and signed permission checks in a React application
- Dynamic Client Registration against the shared development IdP
- one frontend switched between Express, Cloudflare Workers, Bun, and Deno
  backends through one environment variable

## Requirements

- Node.js 20.19 or newer
- npm 10 or newer
- the [shared development IdP](../../common/README.md) on port `9090`
- one compatible backend from the table below
- published `@janssenproject/cedarling@1.0.0` and
  `@janssenproject/cedarling_wasm@1.0.0` packages, or the local staged-package workflow
  described in the [examples overview](../../README.md#sdk-package-availability)

## Select a backend

| Backend | Start command | Frontend command |
| --- | --- | --- |
| Express | `cd ../backend && npm start` | `npm run dev` |
| Hono + Cloudflare | `cd ../../hono && npm run dev:cf` | `VITE_BACKEND_URL=http://localhost:8787 npm run dev` |
| Hono + Bun | `cd ../../hono && npm run dev:bun` | `VITE_BACKEND_URL=http://localhost:3001 npm run dev` |
| Hono + Deno | `cd ../../hono && npm run dev:deno` | `VITE_BACKEND_URL=http://localhost:3001 npm run dev` |

Run `npm run install:sdk:local` in both the selected backend and this directory
before starting them. Open `http://localhost:3000`.

You can persist the backend selection in an uncommitted `.env.local` file:

```dotenv
VITE_BACKEND_URL=http://localhost:8787
```

Only use a trusted backend URL. Vite exposes `VITE_*` variables to browser code.

## Exercise authorization

1. In unsigned mode, switch between Bob, Alice, and Charlie.
2. Create, update, and delete tasks and compare the available controls.
3. Switch to signed mode and log in through the local IdP.
4. Inspect browser console output for Cedarling initialization and decisions.

## Security and limitations

This is a development UI. Its current OIDC flow keeps test tokens and the
dynamically registered public client identifier in browser storage. Use the
server-side Authorization Code + PKCE pattern in the
[Next.js example](../../vercel-nextjs/README.md) for a production-oriented web
session design. The Vite configuration intentionally contains no repository-
local filesystem allowances or package-resolution polyfills.
