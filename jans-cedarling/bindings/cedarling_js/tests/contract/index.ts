import type { SuiteLoader } from "../run.js";

/** Public real-WASM contract suites loaded by every capable runtime runner. */
export const contractSuites: readonly SuiteLoader[] = [
  () => import("./authorization.test.js"),
  () => import("./authorize-multi-issuer.test.js"),
  () => import("./authorize-unsigned.test.js"),
  () => import("./configuration.test.js"),
  () => import("./context.test.js"),
  () => import("./error-contract.test.js"),
  () => import("./lifecycle.test.js"),
  () => import("./issuers.test.js"),
  () => import("./logs.test.js"),
  () => import("./policy-archive.test.js"),
  () => import("./policy-loader.test.js"),
  () => import("./policy-url.test.js"),
  () => import("./value-validation.test.js"),
  () => import("./web-entry.test.js"),
  () => import("./web-initialization-errors.test.js"),
  () => import("./web-native-policy-sources.test.js"),
  () => import("./web-tracer.test.js"),
];
