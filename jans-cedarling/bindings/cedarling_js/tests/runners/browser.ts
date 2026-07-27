import QUnit from "qunit";
import { runTestSuites } from "../run.js";

import { unitSuites } from "../unit/index.js";
import { portableContractSuites } from "./portable-suites.js";
import type { IsolatedSdkEntry, RuntimeFixtures } from "../run.js";
import { createCedarling } from "@janssenproject/cedarling";

const sdk: IsolatedSdkEntry = { createCedarling };

/**
 * Browser-page fixtures. The tracer archive and the generated WASM asset are
 * served same-origin by the Playwright fixture server; only the fixtures
 * used by the portable contract subset are implemented for real.
 */
const browserRuntimeFixtures: RuntimeFixtures = {
  runtime: "browser",

  async loadTracerArchive(): Promise<Uint8Array> {
    const response = await fetch("/tracer-policy-store.cjar");
    if (!response.ok) {
      throw new Error(`tracer archive fixture failed: ${response.status}`);
    }
    return new Uint8Array(await response.arrayBuffer());
  },

  async withMissingWasmAsset(): Promise<void> {},

  async withMissingWebAssembly(run): Promise<void> {
    const descriptor = Object.getOwnPropertyDescriptor(
      globalThis,
      "WebAssembly",
    );

    try {
      Object.defineProperty(globalThis, "WebAssembly", {
        configurable: true,
        enumerable: false,
        value: undefined,
        writable: true,
      });
      await run(sdk);
    } finally {
      if (descriptor === undefined) {
        Reflect.deleteProperty(globalThis, "WebAssembly");
      } else {
        Object.defineProperty(globalThis, "WebAssembly", descriptor);
      }
    }
  },

  async withPolicyServer(): Promise<void> {},
};

// Run the unit and real-WASM contract suites inside the browser.
runTestSuites(
  QUnit,
  [...unitSuites, ...portableContractSuites],
  browserRuntimeFixtures,
).then((stats) => {
  (globalThis as unknown as { stats: unknown }).stats = stats;
}).catch((error) => {
  console.error("Test execution failed:", error);
  (globalThis as unknown as { stats: unknown }).stats = { failed: 1 };
});
