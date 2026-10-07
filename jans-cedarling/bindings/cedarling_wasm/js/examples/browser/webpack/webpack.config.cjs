const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const { Compilation, sources } = require("webpack");

const html = readFileSync(resolve(__dirname, "index.html"));

module.exports = {
  mode: "production",
  entry: resolve(__dirname, "main.mjs"),
  output: {
    clean: true,
    filename: "bundle.js",
    path: resolve(__dirname, "../../.build/webpack"),
  },
  plugins: [
    {
      apply(compiler) {
        compiler.hooks.thisCompilation.tap("ExampleHtml", (compilation) => {
          compilation.hooks.processAssets.tap(
            {
              name: "ExampleHtml",
              stage: Compilation.PROCESS_ASSETS_STAGE_ADDITIONAL,
            },
            () =>
              compilation.emitAsset("index.html", new sources.RawSource(html)),
          );
        });
      },
    },
  ],
};
