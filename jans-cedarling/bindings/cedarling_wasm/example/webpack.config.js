const CopyWebpackPlugin = require("copy-webpack-plugin");
const path = require("path");

module.exports = {
  entry: "./bootstrap.js",
  output: {
    path: path.resolve(__dirname, "dist"),
    filename: "bootstrap.js",
  },
  mode: "development",
  plugins: [
    new CopyWebpackPlugin(["index.html"]),
  ],
  experiments: {
    asyncWebAssembly: true, // or use syncWebAssembly: true if preferred
  },
  module: {
    rules: [
      {
        test: /\.wasm$/, // regex to match .wasm files
        type: "webassembly/async", // or 'webassembly/sync' based on your preference
      },
    ],
  },
  resolve: {
    extensions: [".js", ".wasm"], // resolve these extensions
  },
};
