import type { SuiteLoader } from "../run.js";

export const unitSuites: readonly SuiteLoader[] = [
  () => import("./authorization-kernel.test.js"),
  () => import("./authorize-unsigned.test.js"),
  () => import("./common-values.test.js"),
];
