# Cedarling React + Express example

Express enforces every task operation in Node.js. The Vite React UI also runs
Cedarling in the browser to preview update and delete permissions.

The browser OIDC path uses Dynamic Client Registration, Authorization Code +
S256 PKCE, random state and nonce values, verified RS256 ID/UserInfo JWTs, and
tab-scoped `sessionStorage`. There is no implicit flow or persistent token
storage.

## Cedarling code tour

- `frontend/src/cedarling/init.ts` initializes the browser SDK.
- `frontend/src/cedarling/permissions.ts` computes UI-only permission previews.
- `frontend/src/oidc.ts` verifies the signed UserInfo JWT used by the preview.
- `backend/cedarling/init.js` initializes the Node.js SDK.
- `backend/cedarling/authz-middleware.js` selects signed or unsigned
  authorization and enforces the result.
- `backend/server.js` places authorization before every task mutation.

## Run

Use three terminals:

```bash
cd ../common && npm ci && npm start
```

```bash
cd backend && npm run install:sdk:local && npm start
```

```bash
cd frontend && npm run install:sdk:local && npm run dev
```

Open `http://localhost:3000`. Bob owns “Buy groceries”, Alice owns “Schedule
meeting with CEO”, and Charlie owns neither.

The frontend can target Hono instead of Express through `VITE_BACKEND_URL`.
See the [frontend guide](frontend/README.md), [backend guide](backend/README.md),
and [Hono guide](../hono/README.md).
