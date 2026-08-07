# Cedarling Next.js example

This App Router TaskApp runs Cedarling in Node.js task handlers and in an Edge
integration route. OIDC uses server-side DCR, Authorization Code + S256 PKCE,
verified state/nonce, verified RS256 ID and UserInfo JWTs, HttpOnly cookies, and
provider logout.

## Cedarling code tour

- `libs/cedarling/init.ts` initializes the SDK in the importing route runtime.
- `libs/cedarling/authorize.ts` maps signed or unsigned identities and returns a
  canonical allowed, denied, or error outcome.
- `libs/permission-check.ts` implements the shared UI-preview request.
- `app/api/check/route.ts` and `app/api/check-edge/route.ts` keep the Node and
  Edge runtime adapters explicit.
- `app/api/tasks/*` repeats authorization with server-owned task resources
  before every operation.
- `libs/oidc/auth.ts` verifies the HttpOnly signed session before authorization.

## Run and verify

Start the [shared IdP](../common/README.md), then:

```bash
npm run install:sdk:local
npm run build
npm run test:e2e -- --workers=1
npm run dev
```

`OIDC_ISSUER` defaults to `http://localhost:9090`. `APP_ORIGIN` is required in
production and defines registered callback URLs.

A complete verified OIDC session always takes precedence. Partial or tampered
session cookies fail authentication and cannot fall back to `x-user-id`.
Without session cookies, unsigned requests require an explicit known
`x-user-id`. The obsolete `authMode` cookie has no effect.

| Route | Runtime | Purpose |
| --- | --- | --- |
| `/api/tasks`, `/api/tasks/[id]` | Node.js | protected task CRUD |
| `/api/check` | Node.js | update/delete permission check using server task state |
| `/api/check-edge` | Edge | the same safe integration check |
| `/api/oidc/*` | Node.js | DCR, login, session, and logout |

Check routes accept only `action=UpdateTask|DeleteTask` and `taskId`; they never
accept tokens or caller-supplied owner attributes in URLs. Task and session
state is process-local. The current Edge WASM size may exceed hosted platform
limits even when local Edge simulation succeeds.
