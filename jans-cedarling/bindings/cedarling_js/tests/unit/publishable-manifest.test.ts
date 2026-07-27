import { execFile } from "node:child_process";
import { mkdtemp, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { promisify } from "node:util";

import type QUnitApi from "qunit";

const execute = promisify(execFile);

interface GuardResult {
  readonly exitCode: number;
  readonly stderr: string;
}

async function runGuard(
  dependency: string,
  dependencyName = "@janssenproject/cedarling_wasm",
): Promise<GuardResult> {
  const directory = await mkdtemp(
    join(tmpdir(), "cedarling-publishable-manifest-"),
  );
  const manifestPath = join(directory, "package.json");
  const guardPath = resolve(
    process.cwd(),
    "scripts/assert-publishable.mjs",
  );

  try {
    await writeFile(
      manifestPath,
      JSON.stringify({
        name: "publishable-contract-fixture",
        version: "1.0.0",
        dependencies: { [dependencyName]: dependency },
      }),
    );

    try {
      await execute(
        process.execPath,
        [guardPath, manifestPath],
        {
          env: { ...process.env, ELECTRON_RUN_AS_NODE: "1" },
        },
      );
      return { exitCode: 0, stderr: "" };
    } catch (error: unknown) {
      if (
        typeof error === "object" &&
        error !== null &&
        "code" in error &&
        "stderr" in error
      ) {
        return {
          exitCode:
            typeof error.code === "number" ? error.code : 1,
          stderr:
            typeof error.stderr === "string" ? error.stderr : "",
        };
      }
      throw error;
    }
  } finally {
    await rm(directory, { force: true, recursive: true });
  }
}

/** Registers the release-manifest publication guard contract. */
export default function registerPublishableManifestTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("publishable-manifest");

  QUnit.test("accepts the scoped WASM package and rejects unsafe dependencies", async (assert) => {
    const exact = await runGuard("1.2.3");
    assert.strictEqual(
      exact.exitCode,
      0,
      "the scoped WASM package at an exact version is publishable",
    );

    const legacyName = await runGuard("1.2.3", "cedarling_wasm");
    assert.notStrictEqual(
      legacyName.exitCode,
      0,
      "the unpublished unscoped WASM package name is rejected",
    );

    for (const dependency of [
      "npm:cedarling_wasm@1.2.3",
      "npm:@janssenproject/cedarling_wasm@^1.2.3",
      "file:../pkg",
      "^1.2.3",
      "~1.2.3",
    ]) {
      const rejected = await runGuard(dependency);
      assert.notStrictEqual(
        rejected.exitCode,
        0,
        `${dependency} is rejected`,
      );
      assert.true(
        rejected.stderr.includes("@janssenproject/cedarling_wasm"),
        "the failure identifies the unsafe dependency",
      );
    }
  });
}
