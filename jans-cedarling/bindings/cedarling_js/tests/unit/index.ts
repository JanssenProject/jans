import type { SuiteLoader } from "../run.js";

/** Runtime-neutral unit suites loaded by the shared QUnit orchestrator. */
export const unitSuites: readonly SuiteLoader[] = [
  () => import("./archive-policy.test.js"),
  () => import("./authorize-multi-issuer.test.js"),
  () => import("./authorize-unsigned.test.js"),
  () => import("./client-errors.test.js"),
  () => import("./common-values.test.js"),
  () => import("./context.test.js"),
  () => import("./edge-initialization.test.js"),
  () => import("./engine-boundary.test.js"),
  () => import("./errors.test.js"),
  () => import("./helpers-validation.test.js"),
  () => import("./loader-policy.test.js"),
  () => import("./lifecycle.test.js"),
  () => import("./issuers.test.js"),
  () => import("./logs.test.js"),
  () => import("./options.test.js"),
  () => import("./url-policy.test.js"),
  () => import("./value-inspect.test.js"),
  () => import("./web-initialization.test.js"),
  () => import("./workerd-initialization.test.js"),
];

/**
 * Unit suites that exercise the Node-family engine and therefore import
 * host-only modules (`node:fs`, `node:module`). They run only in runners
 * whose runtime provides the Node module surface (Node, Bun, Deno, Electron
 * main), never in workerd, edge, or browser runners.
 */
export const nodeUnitSuites: readonly SuiteLoader[] = [
  () => import("./node-initialization.test.js"),
  () => import("./publishable-manifest.test.js"),
  () => import("./wasm-producer-contract.test.js"),
];
