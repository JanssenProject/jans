import QUnit from "qunit";
import {
  runTestSuites,
  type SuiteLoader,
  type TestGroup,
} from "../run.js";
import { unitSuites } from "../unit/index.js";
import { contractSuites } from "../contract/index.js";

const groups: Readonly<Record<TestGroup, readonly SuiteLoader[]>> = {
  unit: unitSuites,
  contract: contractSuites,
};
const [, , requestedGroup, filter] = process.argv;
if (requestedGroup !== "unit" && requestedGroup !== "contract") {
  throw new Error("Expected a test group: unit or contract.");
}
const summary = await runTestSuites(QUnit, groups[requestedGroup], filter);
process.exitCode = summary.failed === 0 ? 0 : 1;
