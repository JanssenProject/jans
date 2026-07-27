import { mkdtemp, readFile, rm } from "node:fs/promises";
import { createRequire } from "node:module";
import { tmpdir } from "node:os";
import { join } from "node:path";

import type QUnitApi from "qunit";

interface WebpackStats {
  hasErrors(): boolean;
  toJson(options: unknown): {
    readonly assets?: readonly { readonly name?: string }[];
  };
  toString(options: unknown): string;
}

interface WebpackCompiler {
  close(callback: (error?: Error) => void): void;
  run(
    callback: (
      error: Error | null,
      stats?: WebpackStats,
    ) => void,
  ): void;
}

type Webpack = (configuration: unknown) => WebpackCompiler;

const localRequire = createRequire(import.meta.url);
const webpack = localRequire("webpack") as Webpack;

function runWebpack(outputDirectory: string): Promise<WebpackStats> {
  return new Promise((resolve, reject) => {
    const compiler = webpack({
      context: process.cwd(),
      entry: "./tests/fixtures/webpack.mjs",
      mode: "development",
      target: "web",
      output: {
        assetModuleFilename: "assets/[name][ext]",
        filename: "consumer.js",
        path: outputDirectory,
      },
    });
    compiler.run((error, stats) => {
      if (error !== null) {
        compiler.close(() => reject(error));
        return;
      }
      if (stats === undefined) {
        compiler.close(() => reject(new Error("Webpack returned no stats.")));
        return;
      }
      compiler.close((closeError) => {
        if (closeError !== undefined && closeError !== null) {
          reject(closeError);
        } else {
          resolve(stats);
        }
      });
    });
  });
}

/** Registers the browser webpack asset-delivery build contract. */
export default function registerWebpackBuildTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("webpack-build");

  QUnit.test(
    "the browser entry emits and rewrites its dependency-owned WASM asset",
    async (assert) => {
      assert.timeout(60_000);
      const outputDirectory = await mkdtemp(
        join(tmpdir(), "cedarling-webpack-"),
      );

      try {
        const stats = await runWebpack(outputDirectory);
        assert.false(
          stats.hasErrors(),
          stats.toString({ all: false, errors: true }),
        );
        const details = stats.toJson({
          all: false,
          assets: true,
        });
        const wasmAssets = (details.assets ?? []).filter(
          (asset) => asset.name?.endsWith(".wasm") === true,
        );
        assert.strictEqual(
          wasmAssets.length,
          1,
          "webpack emits exactly one generated WASM asset",
        );
        const wasmAsset = wasmAssets[0];
        if (wasmAsset?.name === undefined) {
          return;
        }

        const bundle = await readFile(
          join(outputDirectory, "consumer.js"),
          "utf8",
        );
        assert.true(
          bundle.includes(wasmAsset.name),
          "the browser bundle references the emitted WASM URL",
        );
        assert.false(
          bundle.includes("node:fs"),
          "the browser graph contains no Node filesystem import",
        );
      } finally {
        await rm(outputDirectory, { force: true, recursive: true });
      }
    },
  );
}
