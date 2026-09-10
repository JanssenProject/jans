import type { SuiteLoader } from "../run.js";

/** Public real-WASM contracts that require no Node-specific fixture. */
export const portableContractSuites: readonly SuiteLoader[] = [
  () => import("./authorize-unsigned.test.js"),
  () => import("./authorize-multi-issuer.test.js"),
  () => import("./lifecycle.test.js"),
  () => import("./web-native-policy-sources.test.js"),
];
