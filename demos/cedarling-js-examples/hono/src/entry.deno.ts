import app, { type Bindings } from "./app.ts";

const port = Number(Deno.env.get("PORT") ?? 3001);
const bindings: Bindings = {
  FRONTEND_ORIGIN: Deno.env.get("FRONTEND_ORIGIN"),
  OIDC_ISSUER: Deno.env.get("OIDC_ISSUER"),
};

// This adapter translates Deno's request server into the same Hono fetch
// interface used by the other Cedarling runtime examples.
Deno.serve({ port }, (request) => app.fetch(request, bindings));
