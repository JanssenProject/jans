import type QUnitApi from "qunit";

import {
  createWebEngineFactory,
  type WebEngineDependencies,
} from "../../dist/engine/web.js";
import type { CedarlingEngine } from "../../dist/engine/engine.js";

const supportedEngineOperations: Readonly<Record<keyof CedarlingEngine, true>> = {
  isIssuerLoaded: true,
  setContext: true,
  getContext: true,
  getContextEntry: true,
  deleteContext: true,
  clearContext: true,
  contextEntries: true,
  contextStats: true,
  logIds: true,
  findLogs: true,
  drainLogs: true,
  authorizeUnsigned: true,
  authorizeMultiIssuer: true,
  shutDown: true,
};

const options = {
  bootstrapConfig: {
    CEDARLING_APPLICATION_NAME: "engine-boundary",
    CEDARLING_POLICY_STORE_LOCAL: "{}",
    CEDARLING_LOG_TYPE: "off",
  },
  policyStore: {
    type: "inline" as const,
    document: {},
  },
};

const request = {
  action: 'Example::Action::"Read"',
  resource: {
    type: "Example::Resource",
    id: "document",
  },
};

/** Returns a compatible generated-module loader boundary. */
function moduleDependencies(): Omit<
  WebEngineDependencies,
  "initializeGeneratedClient"
> {
  return {
    hasRequiredWebAssembly: () => true,
    initializeGeneratedModule: async () => ({ memory: {} }),
    initializeGeneratedArchiveClient: async () => ({}),
  };
}

/** Creates a compatible fake client around one generated-result factory. */
function dependenciesForResult(
  createResult: () => unknown | Promise<unknown>,
): WebEngineDependencies {
  return {
    ...moduleDependencies(),
    initializeGeneratedClient: async () => ({
      authorize_unsigned: createResult,
      authorize_multi_issuer: createResult,
      async shut_down() {},
      free() {},
    }),
  };
}

/** Returns one valid generated authorization payload. */
function generatedDecisionJson(): string {
  return JSON.stringify({
    decision: true,
    request_id: "test-request",
    response: {
      diagnostics: {
        reason: ["allow"],
        errors: [],
      },
    },
  });
}

/** Registers generated-WASM containment and disposal unit tests. */
export default function registerEngineBoundaryTests(QUnit: QUnitApi): void {
  QUnit.module("engine-boundary");

  QUnit.test("the engine operation table is exhaustive and accurate", (assert) => {
    // The compile-time `keyof CedarlingEngine` index signature enforces
    // exhaustiveness, but only at the type level. This runtime assertion
    // ensures the table is never accidentally emptied or shadowed.
    assert.deepEqual(
      Object.keys(supportedEngineOperations).sort(),
      [
        "authorizeMultiIssuer",
        "authorizeUnsigned",
        "clearContext",
        "contextEntries",
        "contextStats",
        "deleteContext",
        "drainLogs",
        "findLogs",
        "getContext",
        "getContextEntry",
        "isIssuerLoaded",
        "logIds",
        "setContext",
        "shutDown",
      ],
      "the engine exposes exactly the documented operations",
    );
  });

  QUnit.test(
    "an incompatible generated result is a protocol error and is disposed",
    async (assert) => {
      let resultDisposals = 0;
      const dependencies = dependenciesForResult(async () => ({
        free() {
          resultDisposals += 1;
        },
      }));
      const engine = await createWebEngineFactory(dependencies)(options);

      try {
        await engine.authorizeUnsigned(request);
        assert.pushResult({
          result: false,
          actual: "resolved",
          expected: "GENERATED_PROTOCOL_ERROR",
          message: "the incompatible wrapper must reject",
        });
      } catch (error: unknown) {
        assert.strictEqual(
          (error as { code?: unknown }).code,
          "GENERATED_PROTOCOL_ERROR",
        );
        assert.strictEqual(
          (error as { operation?: unknown }).operation,
          "authorizeUnsigned",
        );
      }

      assert.strictEqual(resultDisposals, 1, "the raw wrapper is released");
    },
  );

  QUnit.test("malformed generated JSON is a conversion error and is disposed", async (assert) => {
    let resultDisposals = 0;
    const engine = await createWebEngineFactory(
      dependenciesForResult(async () => ({
        json_string: () => "{",
        free() {
          resultDisposals += 1;
        },
      })),
    )(options);

    try {
      await engine.authorizeUnsigned(request);
      assert.pushResult({
        result: false,
        actual: "resolved",
        expected: "RESULT_CONVERSION_FAILED",
        message: "malformed generated JSON must reject",
      });
    } catch (error: unknown) {
      assert.strictEqual(
        (error as { code?: unknown }).code,
        "RESULT_CONVERSION_FAILED",
      );
      assert.strictEqual(
        (error as { operation?: unknown }).operation,
        "authorizeUnsigned",
      );
    }

    assert.strictEqual(resultDisposals, 1, "the raw wrapper is released");
  });

  QUnit.test("a generated result disposal failure is a safe protocol error", async (assert) => {
    const secret = "generated-disposal-secret"; // # gitleaks:allow
    const engine = await createWebEngineFactory(
      dependenciesForResult(async () => ({
        json_string: generatedDecisionJson,
        free() {
          throw new Error(secret);
        },
      })),
    )(options);

    try {
      await engine.authorizeUnsigned(request);
      assert.pushResult({
        result: false,
        actual: "resolved",
        expected: "GENERATED_PROTOCOL_ERROR",
        message: "a disposal failure must reject",
      });
    } catch (error: unknown) {
      assert.strictEqual(
        (error as { code?: unknown }).code,
        "GENERATED_PROTOCOL_ERROR",
      );
      assert.false(
        JSON.stringify(error).includes(secret),
        "the raw disposal message is not retained",
      );
    }
  });

  QUnit.test("an incompatible generated client is released and fails safely", async (assert) => {
    let clientDisposals = 0;
    const secret = "unadapted-client-disposal-secret"; // # gitleaks:allow
    const createEngine = createWebEngineFactory({
      ...moduleDependencies(),
      initializeGeneratedClient: async () => ({
        free() {
          clientDisposals += 1;
          throw new Error(secret);
        },
      }),
    });

    try {
      await createEngine(options);
      assert.pushResult({
        result: false,
        actual: "resolved",
        expected: "GENERATED_PROTOCOL_ERROR",
        message: "the incompatible client must reject",
      });
    } catch (error: unknown) {
      assert.strictEqual(
        (error as { code?: unknown }).code,
        "GENERATED_PROTOCOL_ERROR",
      );
      assert.strictEqual(
        (error as { operation?: unknown }).operation,
        "initialize",
      );
      assert.false(
        JSON.stringify(error).includes(secret),
        "the disposal failure is not retained",
      );
    }
    assert.strictEqual(
      clientDisposals,
      1,
      "the unadapted generated wrapper is released",
    );
  });
}
