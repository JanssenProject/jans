import type { SuiteLoader } from "../run.js";

export const contractSuites: readonly SuiteLoader[] = [
  () => import("./raw-wrapper.test.js"),
];
