# Shared development identity provider

This directory contains the local OpenID Connect provider and Cedarling policy
fixtures used by every JavaScript example. It is development infrastructure,
not a production identity provider.

## What it demonstrates

- OpenID Connect discovery and Authorization Code/implicit test flows
- Dynamic Client Registration (DCR)
- signed ID, access, and UserInfo JWTs
- RP-initiated logout
- shared Cedar policy-store and Cedarling test-configuration endpoints

## Requirements

- Node.js 20.19 or newer
- npm 10 or newer

## Run

```bash
npm install
npm start
```

The default issuer is `http://localhost:9090`. To use another loopback port and
issuer:

```bash
PORT=9191 OIDC_ISSUER=http://localhost:9191 npm start
```

`OIDC_ISSUER` must match the public URL clients use to reach this process.

## Endpoints

| Endpoint | Purpose |
| --- | --- |
| `/.well-known/openid-configuration` | OIDC discovery metadata |
| `/reg` | Dynamic Client Registration endpoint discovered from metadata |
| `/auth` | Authorization endpoint discovered from metadata |
| `/token` | Token endpoint discovered from metadata |
| `/me` | Signed UserInfo response |
| `/session/end` | RP-initiated logout endpoint discovered from metadata |
| `/config/policy-store` | TaskApp Cedar policy store |
| `/config/test-config` | Shared Cedarling development scenario |

Use `bob`, `alice`, or `charlie` as the login name. The development interaction
accepts any password.

## Connect an example

Start this service first, then start one of the example applications:

- [React + Express](../react-nodejs/README.md)
- [Hono on Cloudflare Workers, Bun, or Deno](../hono/README.md)
- [Next.js](../vercel-nextjs/README.md)
- [Electron](../electron/README.md)

Most examples default to `http://localhost:9090`. See the selected example's
README before changing the issuer because some runtime adapters currently use
that development URL directly.

## Security and limitations

> [!WARNING]
> This server uses in-memory clients, grants, sessions, and generated signing
> keys. Restarting it invalidates existing registrations and sessions.

The shared Cedarling scenario intentionally relaxes selected JWT checks so the
runtime examples can focus on SDK integration. Do not expose this server to a
network, reuse its configuration in production, or treat it as an identity
provider deployment template.
