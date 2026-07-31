import path from "path";
import CssMinimizerPlugin from "css-minimizer-webpack-plugin";
import HtmlWebpackPlugin from "html-webpack-plugin";
import MiniCssExtractPlugin from "mini-css-extract-plugin";
import TerserPlugin from "terser-webpack-plugin";
import webpack from "webpack";
import { merge } from "webpack-merge";

import checkNodeEnv from "../scripts/check-node-env";
import baseConfig from "./webpack.config.base";
import webpackPaths from "./webpack.paths";

checkNodeEnv("production");

const configuration: webpack.Configuration = {
  devtool: "source-map",
  mode: "production",
  target: ["web", "electron-renderer"],
  entry: [path.join(webpackPaths.srcRendererPath, "index.tsx")],
  output: {
    clean: true,
    path: webpackPaths.distRendererPath,
    publicPath: "./",
    filename: "renderer.js",
    library: { type: "umd" },
  },
  module: {
    rules: [
      {
        test: /\.css$/,
        use: [MiniCssExtractPlugin.loader, "css-loader", "postcss-loader"],
      },
      // The browser SDK resolves its WASM URL at runtime, so emit the binary as
      // an addressable renderer asset instead of inlining it as JavaScript.
      {
        test: /\.wasm$/,
        type: "asset/resource",
        generator: { filename: "static/wasm/[name].[hash][ext]" },
      },
    ],
  },
  optimization: {
    minimize: true,
    minimizer: [new TerserPlugin(), new CssMinimizerPlugin()],
  },
  plugins: [
    new webpack.EnvironmentPlugin({ NODE_ENV: "production" }),
    new MiniCssExtractPlugin({ filename: "style.css" }),
    new HtmlWebpackPlugin({
      filename: "index.html",
      template: path.join(webpackPaths.srcRendererPath, "index.ejs"),
      minify: { collapseWhitespace: true, removeComments: true },
    }),
    new webpack.DefinePlugin({ "process.type": '"renderer"' }),
  ],
};

export default merge(baseConfig, configuration);
