import type QUnitApi from "qunit";

import { createClientForEngine } from "../../dist/client/client.js";
import { createSdkError } from "../../dist/errors/errors.js";
import {
  createWebEngineFactory,
  type WebEngineDependencies,
} from "../../dist/engine/web.js";
import { createTestEngine } from "./engine-fixture.js";

const request = {
  principal: { type: "Tracer::User", id: "alice" },
  action: 'Tracer::Action::"Read"',
  resource: { type: "Tracer::Resource", id: "document" },
};

const decision = {
  decision: true,
  requestId: "lifecycle-unit",
  diagnostics: { reasons: ["allow"], errors: [] },
};

/** Registers deterministic lifecycle and concurrency coordination tests. */
export default function registerLifecycleTests(QUnit: QUnitApi): void {
  QUnit.module("lifecycle");

  QUnit.test("shutDown rejects new work and waits for accepted work", async (assert) => {
    const releases: Array<() => void> = [];
    let starts = 0;
    let engineShutdowns = 0;
    const engine = createTestEngine({
      async authorizeUnsigned() {
        starts += 1;
        await new Promise<void>((resolve) => {
          releases.push(resolve);
        });
        return decision;
      },
      async shutDown() {
        engineShutdowns += 1;
      },
    });
    const client = createClientForEngine(engine);

    const first = client.authorizeUnsigned(request);
    const second = client.authorizeUnsigned(request);
    assert.strictEqual(starts, 2, "authorization calls are not serialized");

    const firstShutdown = client.shutDown();
    const secondShutdown = client.shutDown();
    assert.strictEqual(
      firstShutdown,
      secondShutdown,
      "concurrent shutdown shares one promise",
    );
    const rejected = await client.authorizeUnsigned(request);
    assert.false(rejected.ok);
    if (!rejected.ok) {
      assert.strictEqual(rejected.error.code, "CLIENT_CLOSED");
    }
    assert.strictEqual(engineShutdowns, 0, "shutdown waits for accepted work");

    for (const release of releases) {
      release();
    }
    assert.true((await first).ok);
    assert.true((await second).ok);
    assert.true((await firstShutdown).ok);
    assert.strictEqual(engineShutdowns, 1);
  });

  QUnit.test("shutdown failure is shared and shutDown remains idempotent", async (assert) => {
    let engineShutdowns = 0;
    const client = createClientForEngine(
      createTestEngine({
        async authorizeUnsigned() {
          return decision;
        },
        async shutDown() {
          engineShutdowns += 1;
          throw createSdkError("LIFECYCLE_FAILED", "shutDown");
        },
      }),
    );

    const firstPromise = client.shutDown();
    const secondPromise = client.shutDown();
    const first = await firstPromise;
    const second = await secondPromise;
    const repeatedPromise = client.shutDown();

    assert.strictEqual(firstPromise, secondPromise);
    assert.strictEqual(repeatedPromise, firstPromise);
    assert.strictEqual(first, second, "all callers observe the same result object");
    assert.false(first.ok);
    if (!first.ok) {
      assert.strictEqual(first.error.code, "LIFECYCLE_FAILED");
    }
    assert.strictEqual(engineShutdowns, 1);
  });

  QUnit.test("closed operations do not inspect caller values", async (assert) => {
    const client = createClientForEngine(
      createTestEngine({
        async authorizeUnsigned() {
          return decision;
        },
      }),
    );
    await client.shutDown();
    let reads = 0;
    const malicious = Object.defineProperty({}, "action", {
      enumerable: true,
      get() {
        reads += 1;
        return request.action;
      },
    });

    const result = await client.authorizeUnsigned(malicious as never);
    assert.false(result.ok);
    assert.strictEqual(reads, 0);
  });

  QUnit.test("generated shutdown failure still disposes the wrapper once", async (assert) => {
    const events: string[] = [];
    const dependencies: WebEngineDependencies = {
      hasRequiredWebAssembly: () => true,
      initializeGeneratedModule: async () => ({ memory: {} }),
      initializeGeneratedArchiveClient: async () => ({}),
      initializeGeneratedClient: async () => ({
        async authorize_unsigned() {
          throw new Error("authorization is outside this test");
        },
        async authorize_multi_issuer() {
          throw new Error("authorization is outside this test");
        },
        async shut_down() {
          events.push("shutdown");
          throw new Error("private shutdown failure");
        },
        free() {
          events.push("dispose");
        },
      }),
    };
    const engine = await createWebEngineFactory(dependencies)({
      bootstrapConfig: {
        CEDARLING_APPLICATION_NAME: "lifecycle-generated-failure",
        CEDARLING_POLICY_STORE_LOCAL: "{}",
      },
      policyStore: { type: "inline", document: {} },
    });
    const client = createClientForEngine(engine);

    const result = await client.shutDown();
    assert.false(result.ok);
    if (!result.ok) {
      assert.strictEqual(result.error.code, "LIFECYCLE_FAILED");
      assert.false(
        JSON.stringify(result.error).includes("private shutdown failure"),
      );
    }
    assert.deepEqual(events, ["shutdown", "dispose"]);
    await client.shutDown();
    assert.deepEqual(events, ["shutdown", "dispose"]);
  });
}
