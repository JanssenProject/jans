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

// @ts-expect-error Deno global type
const DenoArgv = Deno.args;
const requestedGroup = DenoArgv[0];
const filter = DenoArgv[1];

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

if (summary.failed !== 0) {
  // @ts-expect-error Deno global exit call
  Deno.exit(1);
} else {
  // @ts-expect-error Deno global exit call
  Deno.exit(0);
}
