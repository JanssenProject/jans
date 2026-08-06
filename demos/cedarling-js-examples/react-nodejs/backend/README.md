# Cedarling Express backend

This Node.js API initializes Cedarling from the shared strict configuration and
policy URLs, then enforces every task operation.

## Cedarling code tour

- `cedarling/init.js` owns the retryable process-wide client and shutdown.
- `cedarling/authz-middleware.js` constructs trusted task resources, selects
  `authorizeMultiIssuer` or `authorizeUnsigned`, and interprets the result.
- `server.js` orders validation, authorization, and mutation.

## Run and verify

Start the [shared IdP](../../common/README.md), then:

```bash
npm run install:sdk:local
npm test
npm start
```

The API listens on `http://localhost:8080`. Configure `OIDC_ISSUER`,
`FRONTEND_ORIGIN`, or `PORT` when required.

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/tasks` | list tasks |
| `POST` | `/tasks` | create an identity-owned task |
| `PUT` | `/tasks/:id` | update an existing owned task |
| `DELETE` | `/tasks/:id` | delete an existing owned task |

Every request requires `x-user-id: bob`, `alice`, or `charlie`. Add
`Authorization: Bearer <signed-userinfo-jwt>` for signed authorization. Bearer
scheme matching is case-insensitive. Missing identities never default to Bob.

The API validates bodies, rejects unknown fields, returns missing resources
before authorization, and distinguishes policy denial from Cedarling operation
failure. Tasks are process-local and reset on restart.
