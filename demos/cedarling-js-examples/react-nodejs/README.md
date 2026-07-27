# Cedarling React + Express example

This is the reference TaskApp implementation for the JavaScript examples. The
Express API enforces every task operation with Cedarling, while the Vite React
frontend runs Cedarling in the browser to present permission-aware controls.

## What it demonstrates

- one policy model shared between browser and Node.js
- `authorizeUnsigned` for application-asserted identities
- `authorizeMultiIssuer` for signed OIDC UserInfo tokens
- Dynamic Client Registration against the shared development IdP
- task CRUD protected by backend middleware
- Cedarling context, trusted-issuer, decision-log, and lifecycle APIs
- a frontend that can be reused with Express or any Hono runtime

## Components

| Component | Default origin | README |
| --- | --- | --- |
| Shared development IdP and policy fixtures | `http://localhost:9090` | [Open](../common/README.md) |
| Express backend | `http://localhost:8080` | [Open](backend/README.md) |
| Vite React frontend | `http://localhost:3000` | [Open](frontend/README.md) |

## Requirements

- Node.js 20.19 or newer
- npm 10 or newer
- published `@janssenproject/cedarling@1.0.0` and
  `@janssenproject/cedarling_wasm@1.0.0` packages, or the local staged-package workflow
  described in the [examples overview](../README.md#sdk-package-availability)

## Run the reference stack

Use three terminals:

```bash
# Terminal 1
cd ../common
npm install
npm start
```

```bash
# Terminal 2
cd backend
npm run install:sdk:local
npm start
```

```bash
# Terminal 3
cd frontend
npm run install:sdk:local
npm run dev
```

Open `http://localhost:3000`.

## Use another backend

Start one Hono target instead of Express, then set the frontend API origin:

| Backend | Start from `../hono` | Start from `frontend` |
| --- | --- | --- |
| Cloudflare Workers | `npm run dev:cf` | `VITE_BACKEND_URL=http://localhost:8787 npm run dev` |
| Bun | `npm run dev:bun` | `VITE_BACKEND_URL=http://localhost:3001 npm run dev` |
| Deno | `npm run dev:deno` | `VITE_BACKEND_URL=http://localhost:3001 npm run dev` |

The [frontend README](frontend/README.md) explains persistent local
configuration. The [Hono README](../hono/README.md) covers runtime permissions
and deployment limitations.

## Exercise the policy

- Bob owns “Buy groceries.”
- Alice owns “Schedule meeting with CEO” and has the Admin role in signed
  tokens.
- Charlie is a guest identity.

Switch users in unsigned mode and compare update/delete controls. Then switch
to signed mode, log in with the selected username and any password, and repeat
the same operations with a signed UserInfo token.

## Security and limitations

The IdP, task store, and Cedarling scenario are local development fixtures. The
React frontend currently demonstrates a browser-managed test token flow; do
not copy that storage model into production. For server-held tokens,
Authorization Code + PKCE, verified state/nonce, and HttpOnly cookies, use the
[Next.js example](../vercel-nextjs/README.md) as the stronger web-session
reference.
