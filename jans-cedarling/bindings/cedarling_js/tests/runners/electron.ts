import { app } from "electron";
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

const processGlobal = process;
const groupIndex = processGlobal.argv.findIndex((arg) => arg === "unit" || arg === "contract" || arg === "e2e");
const requestedGroup = groupIndex !== -1 ? processGlobal.argv[groupIndex] as TestGroup : undefined;
const filter = groupIndex !== -1 ? processGlobal.argv[groupIndex + 1] : undefined;

function isTestGroup(value: string | undefined): value is TestGroup {
  return value === "unit" || value === "contract" || value === "e2e";
}

if (!isTestGroup(requestedGroup)) {
  throw new Error(`Expected a test group (unit, contract, or e2e) in process.argv: ${JSON.stringify(processGlobal.argv)}`);
}

app.on("ready", async () => {
  installFileUrlFetchShim();

  const summary = await runTestSuites(
    QUnit,
    suiteGroups[requestedGroup],
    nodeRuntimeFixtures,
    filter,
  );
  app.exit(summary.failed === 0 ? 0 : 1);
});
