import path from "path";
import webpack from "webpack";
import TerserPlugin from "terser-webpack-plugin";
import { merge } from "webpack-merge";

import checkNodeEnv from "../scripts/check-node-env";
import baseConfig from "./webpack.config.base";
import webpackPaths from "./webpack.paths";

checkNodeEnv("production");

const configuration: webpack.Configuration = {
  devtool: "source-map",
  mode: "production",
  target: "electron-main",
  entry: {
    main: path.join(webpackPaths.srcMainPath, "main.ts"),
    preload: path.join(webpackPaths.srcMainPath, "preload.ts"),
  },
  output: {
    clean: true,
    path: webpackPaths.distMainPath,
    filename: "[name].js",
    library: { type: "umd" },
  },
  optimization: { minimizer: [new TerserPlugin({ parallel: true })] },
  plugins: [
    new webpack.EnvironmentPlugin({
      NODE_ENV: "production",
      START_MINIMIZED: false,
    }),
    new webpack.DefinePlugin({ "process.type": '"browser"' }),
  ],
  node: { __dirname: false, __filename: false },
  // Keep the SDK external so Electron main loads its Node entry and packaged
  // WASM beside the installed dependency at runtime.
  externals: {
    "@janssenproject/cedarling": "commonjs @janssenproject/cedarling",
  },
};

export default merge(baseConfig, configuration);
