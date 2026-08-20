import type { SuiteLoader } from "../run.js";

export const unitSuites: readonly SuiteLoader[] = [
  () => import("./authorization-kernel.test.js"),
  () => import("./authorize-multi-issuer.test.js"),
  () => import("./authorize-unsigned.test.js"),
  () => import("./common-values.test.js"),
  () => import("./capabilities.test.js"),
  () => import("./archive-policy.test.js"),
  () => import("./context.test.js"),
  () => import("./issuers.test.js"),
  () => import("./logs.test.js"),
  () => import("./options.test.js"),
  () => import("./engine-initialization.test.js"),
];
