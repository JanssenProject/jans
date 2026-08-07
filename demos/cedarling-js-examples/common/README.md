# Shared development identity provider

This directory provides the local OIDC issuer and canonical Cedar policy files
used by every JavaScript example. It is development infrastructure, not a
production identity provider.

It supports Dynamic Client Registration, Authorization Code + S256 PKCE,
RS256-signed ID/access/UserInfo JWTs, logout, exact-origin CORS, and strict
Cedarling configuration. Policies and schema remain readable source files;
`policy-store.js` assembles the served document with the active issuer URL.

## Cedarling code tour

- `cedarling-config.json` restricts JWT verification to RS256.
- `policy-store.js` combines the Cedar schema, policies, and effective issuer.
- `policies/*-token.cedar` authorizes verified signed UserInfo identities.
- `policies/*-user.cedar` authorizes explicit unsigned application identities.
- `idp.js` serves the resulting Cedarling documents and signed UserInfo JWTs.

## Run and test

```bash
npm ci
npm test
npm start
```

Defaults:

- issuer: `http://localhost:9090`
- allowed browser origin: `http://localhost:3000`

Override them together when needed:

```bash
PORT=9191 OIDC_ISSUER=http://localhost:9191 FRONTEND_ORIGIN=http://localhost:3000 npm start
```

Remote issuers and frontend origins must use HTTPS. Loopback HTTP is accepted
for local development.

| Endpoint | Purpose |
| --- | --- |
| `/.well-known/openid-configuration` | OIDC discovery |
| `/reg`, `/auth`, `/token`, `/me` | DCR, authorization, token, signed UserInfo |
| `/session/end` | provider logout |
| `/config/cedarling` | minimal RS256 Cedarling options |
| `/config/policy-store` | dynamically assembled TaskApp policy store |

The server intentionally uses the bundled development interaction pages from
`oidc-provider`. Use `bob`, `alice`, or `charlie` as the login and any
non-empty password. Restarting the process invalidates generated keys,
registrations, grants, and sessions.
