import type QUnitApi from "qunit";

import {
  createEngineFactory,
  type EngineDependencies,
} from "../../dist/engine/factory.js";
import { createGeneratedClientFixture } from "./engine-fixture.js";

const options = {
  bootstrapConfig: {
    CEDARLING_APPLICATION_NAME: "engine-initialization",
    CEDARLING_POLICY_STORE_LOCAL: "{}",
    CEDARLING_LOG_TYPE: "off",
  },
  policyStore: { type: "inline" as const, document: {} },
};

function dependencies(
  initializeGeneratedModule: EngineDependencies["initializeGeneratedModule"],
): EngineDependencies {
  return {
    hasRequiredWebAssembly: () => true,
    initializeGeneratedModule,
    initializeGeneratedClient: async () => createGeneratedClientFixture(),
    initializeGeneratedArchiveClient: async () => createGeneratedClientFixture(),
  };
}

async function rejection(
  work: () => Promise<unknown>,
): Promise<{
  code?: unknown;
  operation?: unknown;
  message?: string;
  details?: unknown;
  cause?: unknown;
} | undefined> {
  try {
    await work();
    return undefined;
  } catch (error: unknown) {
    return error as { code?: unknown; operation?: unknown };
  }
}

export default function registerEngineInitializationTests(
  QUnit: QUnitApi,
): void {
  QUnit.module("engine initialization boundary");

  QUnit.test("forgets a failed module load and preserves its safe error", async (assert) => {
    const secret = "module-load-secret"; // # gitleaks:allow
    let attempts = 0;
    const createEngine = createEngineFactory(dependencies(async () => {
      attempts += 1;
      if (attempts === 1) throw new Error(secret);
      return { memory: {} };
    }));

    const first = await rejection(() => createEngine(options));
    assert.strictEqual(first?.code, "WASM_LOAD_FAILED");
    assert.strictEqual(first?.operation, "initialize");
    assert.false(JSON.stringify(first).includes(secret));
    assert.false(first?.message?.includes(secret) ?? false);
    assert.false(String(first).includes(secret));
    assert.strictEqual(first?.details, undefined);
    assert.false(first !== undefined && "cause" in first);

    const engine = await createEngine(options);
    assert.strictEqual(attempts, 2, "a repaired loader is retried");
    await engine.shutDown();
  });

  QUnit.test("rejects incompatible module output before client construction", async (assert) => {
    let clientCalls = 0;
    const createEngine = createEngineFactory({
      ...dependencies(async () => ({})),
      async initializeGeneratedClient() {
        clientCalls += 1;
        return createGeneratedClientFixture();
      },
    });

    const error = await rejection(() => createEngine(options));
    assert.strictEqual(error?.code, "GENERATED_PROTOCOL_ERROR");
    assert.strictEqual(error?.operation, "initialize");
    assert.strictEqual(clientCalls, 0);
  });
}
