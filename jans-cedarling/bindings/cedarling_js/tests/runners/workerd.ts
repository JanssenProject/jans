import QUnit from "qunit";
import { runTestSuites } from "../run.js";

import { unitSuites } from "../unit/index.js";
import { portableContractSuites } from "./portable-suites.js";
import { createSandboxedRuntimeFixtures } from "./sandbox-fixtures.js";

export default {
  async test() {
    await new Promise((resolve, reject) => {
      runTestSuites(
        QUnit,
        [...unitSuites, ...portableContractSuites],
        createSandboxedRuntimeFixtures("workerd"),
      )
        .then((summary) => {
          if (summary.failed > 0) {
            reject(new Error(`${summary.failed} assertions failed`));
          } else {
            resolve(undefined);
          }
        })
        .catch(reject);
    });
  },
};
