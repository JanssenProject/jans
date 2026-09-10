import QUnit from "qunit";
import {
  runTestSuites,
  type SuiteLoader,
  type TestGroup,
} from "../run.js";
import { unitSuites } from "../unit/index.js";
import { contractSuites } from "../contract/index.js";
import { portableContractSuites } from "../contract/portable.js";

const groups: Readonly<Record<TestGroup, readonly SuiteLoader[]>> = {
  unit: unitSuites,
  contract: contractSuites,
  portable: portableContractSuites,
};
const [, , requestedGroup, filter] = process.argv;
function isTestGroup(value: string | undefined): value is TestGroup {
  return value === "unit" || value === "contract" || value === "portable";
}
if (!isTestGroup(requestedGroup)) {
  throw new Error("Expected a test group: unit, contract, or portable.");
}
const summary = await runTestSuites(
  QUnit,
  groups[requestedGroup],
  filter,
);
process.exitCode = summary.failed === 0 ? 0 : 1;
