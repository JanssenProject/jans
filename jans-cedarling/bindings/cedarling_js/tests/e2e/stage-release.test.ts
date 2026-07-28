import { execFile } from "node:child_process";
import { mkdtemp, readFile, readdir, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join, resolve } from "node:path";
import { promisify } from "node:util";

import type QUnitApi from "qunit";

const execute = promisify(execFile);

interface PackageManifest {
  readonly name?: string;
  readonly version?: string;
  readonly dependencies?: Readonly<Record<string, string>>;
}

interface CommandFailure {
  readonly code?: number | string;
  readonly stderr?: string;
}

/** Registers the coordinated release-staging contract. */
export default function registerStageReleaseTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("stage-release");

  QUnit.test("stages coordinated SDK and WASM packages at the SDK release version", async (assert) => {
    assert.timeout(60_000);

    const packageRoot = process.cwd();
    const temporaryRoot = await mkdtemp(
      join(tmpdir(), "cedarling-stage-release-test-"),
    );
    const artifactsDirectory = join(temporaryRoot, "artifacts");

    try {
      await execute(
        process.execPath,
        [
          resolve(packageRoot, "scripts/stage-release.mjs"),
          "--pack-destination",
          artifactsDirectory,
        ],
        { cwd: packageRoot },
      );

      const tarballs = (await readdir(artifactsDirectory)).filter(
        (entry) => entry.endsWith(".tgz"),
      );
      assert.strictEqual(
        tarballs.length,
        2,
        "the stage creates one SDK and one generated WASM tarball",
      );
      const sdkTarball = tarballs.find((entry) =>
        entry.startsWith("janssenproject-cedarling-"),
      );
      const wasmTarball = tarballs.find((entry) =>
        entry.startsWith("janssenproject-cedarling_wasm-"),
      );
      if (sdkTarball === undefined || wasmTarball === undefined) {
        assert.true(false, "both named release artifacts are present");
        return;
      }

      const archivePath = join(artifactsDirectory, sdkTarball);
      const { stdout: archiveListing } = await execute(
        "tar",
        ["-tzf", archivePath],
      );
      const packedFiles = archiveListing.split("\n");
      assert.false(
        packedFiles.includes("package/dist/node-index.js"),
        "the obsolete root Node entry is not published",
      );
      assert.false(
        packedFiles.some((entry) =>
          /package\/(?:CHANGELOG(?:\.md)?|LICENSE)$/.test(entry),
        ),
        "standalone license and changelog files are not published",
      );
      const { stdout } = await execute(
        "tar",
        [
          "-xOf",
          archivePath,
          "package/package.json",
        ],
      );
      const packedManifest = JSON.parse(stdout) as PackageManifest;
      const sourceManifest = JSON.parse(
        await readFile(
          resolve(packageRoot, "package.json"),
          "utf8",
        ),
      ) as { readonly version: string };
      const dependency =
        packedManifest.dependencies?.["@janssenproject/cedarling_wasm"];

      assert.strictEqual(
        dependency,
        sourceManifest.version,
        "the packed SDK pins the scoped generated package version",
      );

      const { stdout: wasmManifestText } = await execute(
        "tar",
        [
          "-xOf",
          join(artifactsDirectory, wasmTarball),
          "package/package.json",
        ],
      );
      const packedWasmManifest = JSON.parse(
        wasmManifestText,
      ) as PackageManifest;
      assert.strictEqual(
        packedWasmManifest.name,
        "@janssenproject/cedarling_wasm",
        "the generated artifact uses its public scoped package name",
      );
      assert.strictEqual(
        packedWasmManifest.version,
        sourceManifest.version,
        "the generated artifact uses the coordinated release version",
      );
    } finally {
      await rm(temporaryRoot, { force: true, recursive: true });
    }
  });

  QUnit.test("stages coordinated packages at an explicit release version", async (assert) => {
    assert.timeout(60_000);

    const packageRoot = process.cwd();
    const temporaryRoot = await mkdtemp(
      join(tmpdir(), "cedarling-stage-release-version-test-"),
    );
    const artifactsDirectory = join(temporaryRoot, "artifacts");
    const releaseVersion = "1.0.0";

    try {
      await execute(
        process.execPath,
        [
          resolve(packageRoot, "scripts/stage-release.mjs"),
          "--pack-destination",
          artifactsDirectory,
          "--version",
          releaseVersion,
        ],
        { cwd: packageRoot },
      );

      const sdkTarball = join(
        artifactsDirectory,
        `janssenproject-cedarling-${releaseVersion}.tgz`,
      );
      const wasmTarball = join(
        artifactsDirectory,
        `janssenproject-cedarling_wasm-${releaseVersion}.tgz`,
      );
      const { stdout: sdkManifestText } = await execute(
        "tar",
        ["-xOf", sdkTarball, "package/package.json"],
      );
      const { stdout: wasmManifestText } = await execute(
        "tar",
        ["-xOf", wasmTarball, "package/package.json"],
      );
      const sdkManifest = JSON.parse(sdkManifestText) as PackageManifest;
      const wasmManifest = JSON.parse(wasmManifestText) as PackageManifest;

      assert.strictEqual(
        sdkManifest.version,
        releaseVersion,
        "the staged SDK uses the requested release version",
      );
      assert.strictEqual(
        wasmManifest.version,
        releaseVersion,
        "the staged WASM package uses the requested release version",
      );
      assert.strictEqual(
        sdkManifest.dependencies?.["@janssenproject/cedarling_wasm"],
        releaseVersion,
        "the staged SDK pins the coordinated WASM version",
      );
    } finally {
      await rm(temporaryRoot, { force: true, recursive: true });
    }
  });

  QUnit.test("rejects missing and non-exact explicit versions", async (assert) => {
    assert.timeout(120_000);

    const packageRoot = process.cwd();
    const stageScript = resolve(
      packageRoot,
      "scripts/stage-release.mjs",
    );

    for (const versionArguments of [
      ["--version"],
      ["--version", "^1.0.0"],
    ]) {
      const temporaryRoot = await mkdtemp(
        join(tmpdir(), "cedarling-stage-release-invalid-version-test-"),
      );
      try {
        await assert.rejects(
          execute(
            process.execPath,
            [
              stageScript,
              "--pack-destination",
              join(temporaryRoot, "artifacts"),
              ...versionArguments,
            ],
            { cwd: packageRoot },
          ),
          (error: unknown) => {
            const failure = error as CommandFailure;
            return (
              failure.code !== 0 &&
              failure.stderr?.includes(
                "--version requires an exact semantic version",
              ) === true
            );
          },
          `${versionArguments.join(" ")} is rejected`,
        );
      } finally {
        await rm(temporaryRoot, { force: true, recursive: true });
      }
    }
  });
}
