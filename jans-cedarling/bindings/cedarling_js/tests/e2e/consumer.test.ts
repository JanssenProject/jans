import { execFile } from "node:child_process";
import { resolve } from "node:path";
import { promisify } from "node:util";

import type QUnitApi from "qunit";

const execute = promisify(execFile);

/** Registers the clean hoisted Node consumer simulation. */
export default function registerConsumerSimulationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("consumer");

  QUnit.test(
    "packed ESM and CommonJS consumers initialize in a hoisted layout",
    async (assert) => {
      assert.timeout(60_000);
      const { stdout } = await execute(
        process.execPath,
        [resolve(process.cwd(), "scripts/verify-consumer.mjs")],
        { cwd: process.cwd() },
      );
      assert.true(stdout.includes("ESM consumer initialized and authorized"));
      assert.true(stdout.includes("CommonJS consumer initialized and authorized"));
    },
  );
}
