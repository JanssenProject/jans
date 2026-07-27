import type { SuiteLoader } from "../run.js";

/**
 * Real-WASM public contract suites that run in sandboxed runtimes.
 *
 * Excluded from this list:
 * - `policy-url`: requires a loopback HTTP fixture server, which sandboxed
 *   runners cannot host. URL sources are covered by the Node-family contract
 *   runs and the packed browser end-to-end test.
 * - `web-initialization-errors`: intentionally Node-specific (isolated
 *   consumer staging and asset manipulation).
 * - `web-entry`: exercises the browser/web engine through a host fetch shim,
 *   which only the Node-family runners install.
 */
export const portableContractSuites: readonly SuiteLoader[] = [
  () => import("../contract/authorization.test.js"),
  () => import("../contract/authorize-multi-issuer.test.js"),
  () => import("../contract/authorize-unsigned.test.js"),
  () => import("../contract/configuration.test.js"),
  () => import("../contract/context.test.js"),
  () => import("../contract/error-contract.test.js"),
  () => import("../contract/exports.test.js"),
  () => import("../contract/issuers.test.js"),
  () => import("../contract/lifecycle.test.js"),
  () => import("../contract/logs.test.js"),
  () => import("../contract/policy-archive.test.js"),
  () => import("../contract/policy-loader.test.js"),
  () => import("../contract/value-validation.test.js"),
  () => import("../contract/web-native-policy-sources.test.js"),
  () => import("../contract/web-tracer.test.js"),
];
