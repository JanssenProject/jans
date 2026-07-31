import path from "path";
import webpack from "webpack";
import { merge } from "webpack-merge";

import baseConfig from "./webpack.config.base";
import webpackPaths from "./webpack.paths";

const configuration: webpack.Configuration = {
  devtool: "inline-source-map",
  mode: "development",
  target: "electron-main",
  entry: {
    main: path.join(webpackPaths.srcMainPath, "main.ts"),
    preload: path.join(webpackPaths.srcMainPath, "preload.ts"),
  },
  output: {
    path: webpackPaths.dllPath,
    filename: "[name].bundle.dev.js",
    library: { type: "umd" },
  },
  plugins: [new webpack.DefinePlugin({ "process.type": '"browser"' })],
  node: { __dirname: false, __filename: false },
  // Keep the SDK external so Electron main loads its Node entry and packaged
  // WASM beside the installed dependency at runtime.
  externals: {
    "@janssenproject/cedarling": "commonjs @janssenproject/cedarling",
  },
};

export default merge(baseConfig, configuration);
