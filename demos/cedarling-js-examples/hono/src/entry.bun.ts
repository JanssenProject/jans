import app, { type Bindings } from "./app";

const port = Number(process.env.PORT ?? 3001);
const bindings: Bindings = {
  FRONTEND_ORIGIN: process.env.FRONTEND_ORIGIN,
  OIDC_ISSUER: process.env.OIDC_ISSUER,
};

// This adapter supplies Bun's server API; the Hono application and Cedarling
// authorization flow remain runtime-neutral.
Bun.serve({
  port,
  fetch(request) {
    return app.fetch(request, bindings);
  },
});
