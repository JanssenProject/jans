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

/** Registers focused archive initialization routing tests. */
export default function registerArchivePolicyTests(QUnit: QUnitApi): void {
  QUnit.module("archive-policy");

  QUnit.test("archive sources use only init_from_archive_bytes", async (assert) => {
    const bytes = new Uint8Array([80, 75, 3, 4]);
    let ordinaryCalls = 0;
    let archiveInput: Uint8Array | undefined;
    const dependencies: WebEngineDependencies = {
      hasRequiredWebAssembly: () => true,
      initializeGeneratedModule: async () => ({ memory: {} }),
      async initializeGeneratedClient() {
        ordinaryCalls += 1;
        return generatedClient();
      },
      async initializeGeneratedArchiveClient(_config, input) {
        archiveInput = input;
        return generatedClient();
      },
    };

    const engine = await createWebEngineFactory(dependencies)({
      bootstrapConfig: {
        CEDARLING_APPLICATION_NAME: "archive-unit",
      },
      policyStore: { type: "archive", bytes },
    });

    assert.strictEqual(ordinaryCalls, 0);
    assert.strictEqual(archiveInput, bytes);
    await engine.close();
  });
}
