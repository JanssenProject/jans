# Cedarling Next.js example

This TaskApp runs Cedarling in Next.js App Router handlers on both Node.js and
the Edge runtime. It also provides the examples' production-oriented web OIDC
pattern: server-side Dynamic Client Registration and Authorization Code + PKCE
with an HttpOnly session.

## What it demonstrates

- Cedarling's Node.js export in task and OIDC route handlers
- Cedarling's `edge-light` export in an Edge route
- unsigned and signed TaskApp authorization
- OIDC discovery and Dynamic Client Registration (DCR)
- random state, nonce, and S256 PKCE values
- server-side code exchange, ID-token verification, and signed UserInfo
  verification
- tokens in HttpOnly, `SameSite=Lax` cookies instead of browser storage
- RP-initiated provider logout
- the same context, issuer, decision-log, authorization, and lifecycle features
  exercised by the React + Express reference

## Requirements

- Node.js 20.19 or newer
- npm 10 or newer
- the [shared development IdP](../common/README.md) running on port `9090`
- published `@janssenproject/cedarling@1.0.0` and
  `@janssenproject/cedarling_wasm@1.0.0` packages, or the local staged-package workflow
  described in the [examples overview](../README.md#sdk-package-availability)

## Run

```bash
npm run install:sdk:local
npm run dev
```

Open `http://localhost:3000`.

Optional environment variables:

| Variable | Purpose |
| --- | --- |
| `OIDC_ISSUER` | IdP issuer; defaults to `http://localhost:9090` |
| `APP_ORIGIN` | Public application origin used for registered callbacks; required in production |

Local development derives a loopback origin from the request when
`APP_ORIGIN` is omitted.

## Authorization and OIDC flow

1. Unsigned requests carry an `x-user-id` and call `authorizeUnsigned`.
2. `/api/oidc/start` discovers the IdP and dynamically registers a public web
   client for the current application origin.
3. The server creates state, nonce, and an S256 PKCE verifier before redirecting
   to the provider.
4. The callback validates the response, exchanges the code server-side,
   verifies the ID token and signed UserInfo response, and creates the
   HttpOnly-cookie session.
5. Signed task handlers call `authorizeMultiIssuer` with the verified UserInfo
   JWT.
6. `/api/oidc/logout` clears the local session and starts RP-initiated logout.

DCR data and tokens are never written to `localStorage`, and tokens do not
appear in application URLs.

## Runtime routes

| Route | Runtime | Purpose |
| --- | --- | --- |
| `/api/tasks`, `/api/tasks/[id]` | Node.js | protected task CRUD |
| `/api/check` | Node.js | explicit authorization check |
| `/api/check-edge` | Edge | `edge-light` integration check |
| `/api/cedarling/exercises` | Node.js | disposable full-feature SDK exercise |
| `/api/oidc/*` | Node.js | DCR, login callback, session, and logout |

Smoke-test both Cedarling runtime conditions:

```bash
curl 'http://localhost:3000/api/check?action=ViewTask&userId=bob'
curl 'http://localhost:3000/api/check-edge?action=ViewTask&userId=bob'
```

Both responses should report `"allowed": true`; the second also reports
`"runtime": "edge"`.

The feature-exercise endpoint creates a disposable client so lifecycle testing
cannot close the cached client used by task routes. It is a development
diagnostic endpoint, not a production API.

## Verify

With the shared IdP running:

```bash
npm run test:e2e -- --workers=1
npm run build
```

The browser suite covers hydration persistence, DCR metadata, Code + PKCE,
signed login, SDK feature exercises, signed CRUD, and provider logout.

## Known limitations

Task/session state is process-local development state. Configure durable,
encrypted session storage before production use.

The current generated WASM is approximately 25 MiB raw and 6.1 MiB compressed,
and the build emits two Edge WASM assets. It exceeds Vercel's current Edge
function compressed-size limits, so the Edge route is a local integration proof
rather than a deployable artifact. Review the current
[Vercel Edge runtime limits](https://vercel.com/docs/functions/runtimes/edge/edge-functions)
before deployment.

For a deployed Node route, use HTTPS origins and an authenticated policy service
instead of the loopback development IdP.
