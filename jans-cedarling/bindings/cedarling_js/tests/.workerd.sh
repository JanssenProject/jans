#!/usr/bin/env bash
set -e

COMPATIBILITY_DATE=$(node -p "new Date().toISOString().slice(0,10)")

mkdir -p .test-dist/.build

node tests/build-sandbox.mjs workerd

cat > "$(pwd)/.test-dist/.workerd.capnp" <<EOT
using Workerd = import "/workerd/workerd.capnp";
const config :Workerd.Config = (
  services = [(name = "main", worker = .tapWorker)],
);
const tapWorker :Workerd.Worker = (
  modules = [
    (name = "worker", esModule = embed ".build/run-workerd.js"),
    (name = "@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm", wasm = embed "../node_modules/@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm"),
  ],
  compatibilityDate = "$COMPATIBILITY_DATE",
  compatibilityFlags = []
);
EOT

npx -y workerd test --verbose "$(pwd)/.test-dist/.workerd.capnp"
