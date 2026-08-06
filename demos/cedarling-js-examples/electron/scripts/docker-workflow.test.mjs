import assert from "node:assert/strict";
import test from "node:test";

import {
  ELECTRON_IDP_ISSUER,
  composeArguments,
  nativeElectronEnvironment,
  signalExitCode,
  validatePackageArtifacts,
  waitForDiscovery,
} from "./docker-workflow.mjs";

test("Compose commands are pinned to the examples project and Electron profile", () => {
  assert.deepEqual(
    composeArguments("/examples", "/examples/compose.yaml", "build", "idp-electron"),
    [
      "compose",
      "--project-directory",
      "/examples",
      "--file",
      "/examples/compose.yaml",
      "--profile",
      "electron",
      "build",
      "idp-electron",
    ],
  );
});

test("package export requires coordinated SDK and WASM tarballs", async () => {
  const artifacts = await validatePackageArtifacts("/unused", async () => [
    "janssenproject-cedarling-1.0.0.tgz",
    "janssenproject-cedarling_wasm-1.0.0.tgz",
  ]);
  assert.deepEqual(artifacts, {
    sdk: "janssenproject-cedarling-1.0.0.tgz",
    version: "1.0.0",
    wasm: "janssenproject-cedarling_wasm-1.0.0.tgz",
  });

  await assert.rejects(
    validatePackageArtifacts("/unused", async () => [
      "janssenproject-cedarling-1.0.0.tgz",
      "janssenproject-cedarling_wasm-2.0.0.tgz",
    ]),
    /mismatched/,
  );
});

test("native Electron uses the canonical issuer and cannot inherit Node mode", () => {
  assert.deepEqual(
    nativeElectronEnvironment({ ELECTRON_RUN_AS_NODE: "1", KEEP: "yes" }),
    { KEEP: "yes", OIDC_ISSUER: ELECTRON_IDP_ISSUER },
  );
  assert.equal(signalExitCode("SIGINT"), 130);
  assert.equal(signalExitCode("SIGTERM"), 143);
});

test("readiness rejects inconsistent discovery before accepting the IdP", async () => {
  let attempts = 0;
  const document = await waitForDiscovery({
    fetchImplementation: async () => {
      attempts += 1;
      return {
        ok: true,
        async json() {
          return {
            issuer: attempts === 1 ? "http://127.0.0.1:9090" : ELECTRON_IDP_ISSUER,
          };
        },
      };
    },
    sleep: async () => {},
    timeoutMs: 1_000,
  });

  assert.equal(attempts, 2);
  assert.equal(document.issuer, ELECTRON_IDP_ISSUER);
});
