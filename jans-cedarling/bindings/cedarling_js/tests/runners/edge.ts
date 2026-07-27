import QUnit from "qunit";
import { runTestSuites } from "../run.js";

import { portableContractSuites } from "./portable-suites.js";
import { createSandboxedRuntimeFixtures } from "./sandbox-fixtures.js";

/**
 * Completion handle read by the Node harness (`run-edge.mjs`) after
 * the edge-runtime VM finishes evaluating this module.
 */
(globalThis as unknown as { __cedarlingTestDone: Promise<unknown> }).__cedarlingTestDone =
  runTestSuites(
    QUnit,
    portableContractSuites,
    createSandboxedRuntimeFixtures("vercel-edge"),
  );
