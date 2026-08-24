import type { SuiteLoader } from "../run.js";

export const contractSuites: readonly SuiteLoader[] = [
  () => import("./authorize-unsigned.test.js"),
  () => import("./authorize-multi-issuer.test.js"),
  () => import("./capabilities.test.js"),
  () => import("./web-native-policy-sources.test.js"),
];
