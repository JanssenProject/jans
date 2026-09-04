import type { SuiteLoader } from "../run.js";

export const unitSuites: readonly SuiteLoader[] = [
  () => import("./raw-wrapper.test.js"),
];
