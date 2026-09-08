import type { SuiteLoader } from "../run.js";

/** Contracts suitable for Node, Bun, Deno, and browser runners. */
export const portableContractSuites: readonly SuiteLoader[] = [
  () => import("./raw-wrapper.test.js"),
];
