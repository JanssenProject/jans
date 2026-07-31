import "webpack-dev-server";
import path from "path";
import { spawn } from "child_process";
import HtmlWebpackPlugin from "html-webpack-plugin";
import webpack from "webpack";
import { merge } from "webpack-merge";

import baseConfig from "./webpack.config.base";
import webpackPaths from "./webpack.paths";

const port = process.env.PORT ?? 1212;
const configuration: webpack.Configuration = {
  devtool: "inline-source-map",
  mode: "development",
  target: ["web", "electron-renderer"],
  entry: [path.join(webpackPaths.srcRendererPath, "index.tsx")],
  output: {
    path: webpackPaths.distRendererPath,
    publicPath: "/",
    filename: "renderer.dev.js",
    library: { type: "umd" },
  },
  module: {
    rules: [
      { test: /\.css$/, use: ["style-loader", "css-loader", "postcss-loader"] },
      // The browser SDK resolves its WASM URL at runtime, so emit the binary as
      // an addressable renderer asset instead of inlining it as JavaScript.
      {
        test: /\.wasm$/,
        type: "asset/resource",
        generator: { filename: "static/wasm/[name].[hash][ext]" },
      },
    ],
  },
  plugins: [
    new webpack.NoEmitOnErrorsPlugin(),
    new webpack.EnvironmentPlugin({ NODE_ENV: "development" }),
    new HtmlWebpackPlugin({
      filename: "index.html",
      template: path.join(webpackPaths.srcRendererPath, "index.ejs"),
    }),
  ],
  node: { __dirname: false, __filename: false },
  devServer: {
    port,
    compress: true,
    hot: true,
    historyApiFallback: true,
    setupMiddlewares(middlewares) {
      const args = ["run", "start:main"];
      if (process.env.MAIN_ARGS) args.push("--", process.env.MAIN_ARGS);
      spawn("npm", args, { shell: true, stdio: "inherit" })
        .on("close", (code) => process.exit(code ?? 0))
        .on("error", (error) => console.error(error));
      return middlewares;
    },
  },
};

export default merge(baseConfig, configuration);
