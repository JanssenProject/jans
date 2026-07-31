# Cedarling React frontend

This Vite UI runs the browser export of `@janssenproject/cedarling` and can use
the Express or Hono TaskApp API.

## Cedarling code tour

- `src/cedarling/init.ts` loads strict configuration and initializes WASM.
- `src/cedarling/permissions.ts` previews update and delete decisions.
- `src/oidc.ts` verifies the signed UserInfo JWT passed to Cedarling.
- `src/App.tsx` keeps permission previews separate from backend enforcement.
- `src/main.tsx` shuts down the browser client at the page lifecycle boundary.

## Run and verify

Start the shared IdP and one backend, then:

```bash
npm run install:sdk:local
npm run typecheck
npm test
npm run build
npm run dev
```

| Backend | Frontend command |
| --- | --- |
| Express on `8080` | `npm run dev` |
| Cloudflare on `8787` | `VITE_BACKEND_URL=http://localhost:8787 npm run dev` |
| Bun or Deno on `3001` | `VITE_BACKEND_URL=http://localhost:3001 npm run dev` |

`VITE_OIDC_ISSUER` defaults to `http://localhost:9090`. Remote API and issuer
URLs should use HTTPS.

Unsigned mode explicitly sends a known `x-user-id`. Signed mode performs
Authorization Code + PKCE and stores only the signed session in
`sessionStorage`, so closing the tab clears it. The UI requires signed UserInfo
and never falls back to an ID token.
The backend remains the final authorization boundary.
