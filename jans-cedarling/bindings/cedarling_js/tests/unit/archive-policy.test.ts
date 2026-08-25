import type QUnitApi from "qunit";

import { prepareCedarlingOptions } from "../../.build/configuration/prepare.js";
import {
  createEngineFactory,
  type EngineDependencies,
} from "../../.build/engine/factory.js";
import { createGeneratedClientFixture } from "./engine-fixture.js";

function registerArchivePolicyTests(QUnit: QUnitApi): void {
  QUnit.module("archive-policy");

  QUnit.test("archive sources use only init_from_archive_bytes", async (assert) => {
    const bytes = new Uint8Array([80, 75, 3, 4]);
    let ordinaryCalls = 0;
    let archiveInput: Uint8Array | undefined;
    const dependencies: EngineDependencies = {
      hasRequiredWebAssembly: () => true,
      initializeGeneratedModule: async () => ({ memory: {} }),
      async initializeGeneratedClient() {
        ordinaryCalls += 1;
        return createGeneratedClientFixture();
      },
      async initializeGeneratedArchiveClient(_config, input) {
        archiveInput = input;
        return createGeneratedClientFixture();
      },
    };

    const prepared = prepareCedarlingOptions({
      applicationName: "archive-unit",
      policyStore: { type: "archive", bytes },
    });
    const engine = await createEngineFactory(dependencies)(prepared);

    assert.strictEqual(ordinaryCalls, 0);
    assert.notStrictEqual(archiveInput, bytes);
    assert.deepEqual(archiveInput, bytes);
    await engine.shutDown();
  });
}

function registerLoaderPolicyTests(QUnit: QUnitApi): void {
  QUnit.module("loader-policy");

  QUnit.test("module readiness precedes one loader call and copied archive init", async (assert) => {
    const events: string[] = [];
    const returned = new Uint8Array([80, 75, 3, 4]);
    let archiveInput: Uint8Array | undefined;
    const dependencies: EngineDependencies = {
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
        return createGeneratedClientFixture();
      },
    };

    const engine = await createEngineFactory(dependencies)({
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

function registerUrlPolicyTests(QUnit: QUnitApi): void {
  QUnit.module("url-policy");

  QUnit.test("URL ownership stays with the generated Cedarling bootstrap", (assert) => {
    const prepared = prepareCedarlingOptions({
      applicationName: "url-policy-unit",
      policyStore: {
        type: "url",
        url: new URL("https://policy.example/store?version=1#current"),
        refresh: { intervalSeconds: 30 },
      },
    });

    assert.deepEqual(prepared.policyStore, {
      type: "url",
      url: "https://policy.example/store?version=1#current",
    });
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_POLICY_STORE_URI,
      "https://policy.example/store?version=1#current",
    );
    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_POLICY_STORE_REFRESH_INTERVAL,
      30,
    );
    assert.false(
      Object.hasOwn(prepared.bootstrapConfig, "CEDARLING_POLICY_STORE_LOCAL"),
      "the SDK does not fetch or rewrite URL policy material",
    );
  });

  QUnit.test("accepts HTTPS and exact loopback HTTP endpoints", (assert) => {
    for (const [input, expected] of [
      ["https://policy.example/store", "https://policy.example/store"],
      ["http://localhost/store", "http://localhost/store"],
      ["http://127.0.0.1/store", "http://127.0.0.1/store"],
      ["http://127.255.255.254/store", "http://127.255.255.254/store"],
      ["http://[::1]/store", "http://[::1]/store"],
    ]) {
      const prepared = prepareCedarlingOptions({
        applicationName: "safe-policy-url",
        policyStore: { type: "url", url: input },
      });
      assert.strictEqual(
        prepared.bootstrapConfig.CEDARLING_POLICY_STORE_URI,
        expected,
        input,
      );
    }
  });

  QUnit.test("rejects remote HTTP and credential-bearing endpoints", (assert) => {
    for (const url of [
      "http://policy.example/store",
      "http://localhost.example/store",
      "https://user:password@policy.example/store",
    ]) {
      assert.throws(
        () =>
          prepareCedarlingOptions({
            applicationName: "unsafe-policy-url",
            policyStore: { type: "url", url },
          }),
        (error: unknown) =>
          (error as { readonly code?: unknown }).code ===
            "INPUT_INVALID_FORMAT",
        url,
      );
    }
  });

  QUnit.test("applies the same transport policy to Lock configuration", (assert) => {
    const prepared = prepareCedarlingOptions({
      applicationName: "loopback-lock-url",
      policyStore: { type: "inline", document: {} },
      lock: { configurationUrl: "http://[::1]/lock" },
    });

    assert.strictEqual(
      prepared.bootstrapConfig.CEDARLING_LOCK_SERVER_CONFIGURATION_URI,
      "http://[::1]/lock",
    );
    assert.throws(
      () =>
        prepareCedarlingOptions({
          applicationName: "remote-lock-url",
          policyStore: { type: "inline", document: {} },
          lock: { configurationUrl: "http://lock.example/config" },
        }),
      (error: unknown) =>
        (error as { readonly code?: unknown }).code ===
          "INPUT_INVALID_FORMAT",
    );
  });
}

export default function registerPolicySourceTests(QUnit: QUnitApi): void {
  registerArchivePolicyTests(QUnit);
  registerLoaderPolicyTests(QUnit);
  registerUrlPolicyTests(QUnit);
}
