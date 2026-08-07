# Cedarling Hono backend

One Hono application runs on Cloudflare Workers, Bun, and Deno. Each adapter
passes `OIDC_ISSUER` and `FRONTEND_ORIGIN` bindings into the application; the
core does not hardcode a deployment URL.

## Cedarling code tour

- `src/cedarling/init.ts` caches one client per effective issuer.
- `src/cedarling/authorize.ts` selects signed or unsigned authorization and
  constructs server-owned resources.
- `src/app.ts` contains the shared authorization enforcement seam.
- `src/entry.cloudflare.ts`, `src/entry.bun.ts`, and `src/entry.deno.ts` are
  deliberately thin runtime adapters.

## Install and verify

```bash
npm run install:sdk:local
npm test
npm run typecheck:node
npm run typecheck:deno
npm run build:cf
```

`npm run typecheck` runs both runtime type checks.

| Runtime | Command | Default API origin |
| --- | --- | --- |
| Cloudflare local | `npm run dev:cf` | `http://localhost:8787` |
| Bun | `npm run dev:bun` | `http://localhost:3001` |
| Deno | `npm run dev:deno` | `http://localhost:3001` |

The API exposes only `/tasks` and `/tasks/:id` CRUD routes. Every request needs
a known `x-user-id`; signed requests additionally carry a signed UserInfo
Bearer token. CORS allows only `FRONTEND_ORIGIN`. Bodies, actions, identities,
and resources are validated before authorization.

```bash
curl http://localhost:8787/tasks -H "x-user-id: bob"
```

Cedarling clients are cached per effective issuer and failed initialization is
retryable. Task storage is isolate-local. Check current Cloudflare bundle-size
limits before deployment.
