import app from "./app";

// Wrangler supplies bindings to this fetch handler, while the package export
// resolver selects the worker-compatible Cedarling implementation.
export default app;
