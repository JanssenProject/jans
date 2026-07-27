import QUnit from "qunit";

import { runTestSuites, type TestGroup, type SuiteLoader } from "../run.js";
import { nodeRuntimeFixtures } from "../fixtures/fixtures.js";
import { unitSuites, nodeUnitSuites } from "../unit/index.js";
import { contractSuites } from "../contract/index.js";
import { endToEndSuites } from "../e2e/index.js";
import { installFileUrlFetchShim } from "./file-url-fetch-shim.js";

const suiteGroups: Readonly<Record<TestGroup, readonly SuiteLoader[]>> = {
  unit: [...unitSuites, ...nodeUnitSuites],
  contract: contractSuites,
  e2e: endToEndSuites,
};

/** Minimal process surface required by the Node.js TAP runner. */
interface NodeProcess {
  readonly argv: readonly string[];
  exitCode?: number;
}

const nodeProcess = (globalThis as typeof globalThis & { process?: NodeProcess }).process;

if (nodeProcess === undefined) {
  throw new Error("The Node.js test runner requires the process global.");
}

const [, , requestedGroup, filter] = nodeProcess.argv;

function isTestGroup(value: string | undefined): value is TestGroup {
  return value === "unit" || value === "contract" || value === "e2e";
}

if (!isTestGroup(requestedGroup)) {
  throw new Error("Expected a test group: unit, contract, or e2e.");
}

installFileUrlFetchShim();

const summary = await runTestSuites(
  QUnit,
  suiteGroups[requestedGroup],
  nodeRuntimeFixtures,
  filter,
);
nodeProcess.exitCode = summary.failed === 0 ? 0 : 1;
