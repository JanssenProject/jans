import QUnit from "qunit";

import { portableContractSuites } from "../contract/portable.js";
import { runTestSuites } from "../run.js";

declare global {
  var cedarlingTestResult:
    | { readonly failed: number; readonly error?: string }
    | undefined;
}

try {
  globalThis.cedarlingTestResult = await runTestSuites(QUnit, portableContractSuites);
} catch (error: unknown) {
  globalThis.cedarlingTestResult = {
    failed: 1,
    error: error instanceof Error ? error.stack ?? error.message : String(error),
  };
}
