import type { SuiteLoader } from "../run.js";

/** Host-specific packed-consumer end-to-end suites. */
export const endToEndSuites: readonly SuiteLoader[] = [
  () => import("./browser-tracer.test.js"),
  () => import("./consumer.test.js"),
  () => import("./signature-validation.test.js"),
  () => import("./stage-release.test.js"),
  () => import("./webpack-build.test.js"),
];
