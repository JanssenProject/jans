import type QUnitApi from "qunit";

import {
  createWebEngineFactory,
  type WebEngineDependencies,
} from "../../dist/engine/web.js";

/** Returns a compatible generated client for initialization tests. */
function generatedClient(): object {
  return {
    async authorize_unsigned() {
      throw new Error("authorization is outside this unit test");
    },
    async authorize_multi_issuer() {
      throw new Error("authorization is outside this unit test");
    },
    async shut_down() {},
    free() {},
  };
}

/** Registers focused application-loader ordering and copy tests. */
export default function registerLoaderPolicyTests(QUnit: QUnitApi): void {
  QUnit.module("loader-policy");

  QUnit.test("module readiness precedes one loader call and copied archive init", async (assert) => {
    const events: string[] = [];
    const returned = new Uint8Array([80, 75, 3, 4]);
    let archiveInput: Uint8Array | undefined;
    const dependencies: WebEngineDependencies = {
      hasRequiredWebAssembly: () => true,
      async initializeGeneratedModule() {
        events.push("module");
        return { memory: {} };
      },
      async initializeGeneratedClient() {
        throw new Error("loader sources must not use ordinary init");
      },
      async initializeGeneratedArchiveClient(_config, bytes) {
        events.push("archive");
        archiveInput = bytes;
        return generatedClient();
      },
    };

    const engine = await createWebEngineFactory(dependencies)({
      bootstrapConfig: { CEDARLING_APPLICATION_NAME: "loader-unit" },
      policyStore: {
        type: "loader",
        async load() {
          events.push("loader");
          return returned;
        },
      },
    });

    assert.deepEqual(events, ["module", "loader", "archive"]);
    assert.notStrictEqual(archiveInput, returned);
    assert.deepEqual(archiveInput, returned);
    await engine.shutDown();
  });
}
