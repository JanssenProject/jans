# Cedarling Express backend

This backend is the reference TaskApp API. It runs Cedarling in Node.js,
protects each task operation, and serves the shared policy/configuration files
to the browser frontend.

## What it demonstrates

- `@janssenproject/cedarling` on the Node.js runtime adapter
- application-asserted authorization through `authorizeUnsigned`
- token-based authorization through `authorizeMultiIssuer`
- Express authorization middleware
- Cedarling context, issuer, memory-log, and lifecycle exercises at startup

## Requirements

- Node.js 20.19 or newer
- npm 10 or newer
- the [shared development IdP](../../common/README.md) running on port `9090`
- published `@janssenproject/cedarling@1.0.0` and
  `@janssenproject/cedarling_wasm@1.0.0` packages, or the local staged-package workflow
  described in the [examples overview](../../README.md#sdk-package-availability)

## Run

```bash
npm run install:sdk:local
npm start
```

The API listens on `http://localhost:8080`.

## Connect the React frontend

The frontend uses this backend by default:

```bash
cd ../frontend
npm run install:sdk:local
npm run dev
```

Open `http://localhost:3000`.

## API

| Method | Path | Authorization | Purpose |
| --- | --- | --- | --- |
| `GET` | `/config/policy-store` | none | shared TaskApp policy store |
| `GET` | `/config/test-config` | none | shared development scenario |
| `GET` | `/tasks` | `x-user-id` or bearer token | list visible tasks |
| `POST` | `/tasks` | `x-user-id` or bearer token | create a task |
| `PUT` | `/tasks/:id` | `x-user-id` or bearer token | update an allowed task |
| `DELETE` | `/tasks/:id` | `x-user-id` or bearer token | delete an allowed task |

Unsigned requests use `x-user-id: bob`, `alice`, or `charlie`. Signed requests
use `Authorization: Bearer <signed-userinfo-jwt>`.

## Security and limitations

The task store is in memory and resets when the process restarts. The policy
and test configuration are local fixtures. Do not disable Cedar schema
validation or use the shared scenario's relaxed JWT validation in production.
