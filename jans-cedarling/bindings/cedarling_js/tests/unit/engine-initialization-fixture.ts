import type QUnitApi from "qunit";

import {
  createEngineFactory,
  type EngineDependencies,
} from "../../dist/engine/factory.js";

/**
 * Canonical prepared options shared by every engine-init suite.
 *
 * Engine-init tests observe only the construction boundary; they do not run
 * authorization, so the policy-store document and logging settings are inert.
 */
const options = {
  bootstrapConfig: {
    CEDARLING_APPLICATION_NAME: "engine-initialization",
    CEDARLING_POLICY_STORE_LOCAL: "{}",
    CEDARLING_LOG_TYPE: "off",
  },
  policyStore: {
    type: "inline" as const,
    document: {},
  },
};

/** Returns a compatible generated client for initialization-only tests. */
function generatedClient(): object {
  return {
    async authorize_unsigned() {
      throw new Error("authorization is outside this test");
    },
    async authorize_multi_issuer() {
      throw new Error("authorization is outside this test");
    },
    async shut_down() {},
    free() {},
  };
}

/** Returns one compatible set of host-specific engine dependencies. */
function compatibleDependencies(): EngineDependencies {
  return {
    hasRequiredWebAssembly: () => true,
    initializeGeneratedModule: async () => ({ memory: {} }),
    initializeGeneratedClient: async () => generatedClient(),
    initializeGeneratedArchiveClient: async () => generatedClient(),
  };
}

/** Captures the normalized failure from one engine factory call. */
async function captureFailure(
  createEngine: ReturnType<typeof createEngineFactory>,
): Promise<{
  readonly code?: unknown;
  readonly operation?: unknown;
  readonly serialized: string;
}> {
  try {
    await createEngine(options);
  } catch (error: unknown) {
    return {
      code: (error as { code?: unknown }).code,
      operation: (error as { operation?: unknown }).operation,
      serialized: JSON.stringify(error),
    };
  }

  return {
    code: undefined,
    operation: undefined,
    serialized: "",
  };
}

/**
 * Registers the runtime-neutral engine-initialization tests once per host
 * engine. Each host engine (Web, Node, Edge, Workerd) is built on the same
 * `createEngineFactory`, so its externally visible construction contract is
 * identical. Registering the suite once per engine proves each host wires
 * its dependencies through the shared factory unchanged.
 *
 * @param QUnit - Host-loaded QUnit instance.
 * @param label - Engine label used as the QUnit module name (e.g. `"web"`).
 */
export function registerEngineInitializationTests(
  QUnit: QUnitApi,
  label: "web" | "node" | "edge" | "workerd",
): void {
  QUnit.module(`${label}-initialization`);

  QUnit.test("the generated module initializer runs before Cedarling construction", async (assert) => {
    const events: string[] = [];
    const dependencies: EngineDependencies = {
      hasRequiredWebAssembly: () => true,
      async initializeGeneratedModule() {
        events.push("module");
        return { memory: {} };
      },
      async initializeGeneratedClient() {
        events.push("client");
        return generatedClient();
      },
      async initializeGeneratedArchiveClient() {
        throw new Error("the inline source must not use archive initialization");
      },
    };

    const engine = await createEngineFactory(dependencies)(options);

    assert.deepEqual(events, ["module", "client"]);
    await engine.close();
  });

  QUnit.test("a missing generated module initializer is a protocol error", async (assert) => {
    const dependencies = {
      hasRequiredWebAssembly: () => true,
      initializeGeneratedModule: undefined,
      initializeGeneratedClient: async () => generatedClient(),
      initializeGeneratedArchiveClient: async () => generatedClient(),
    } as unknown as EngineDependencies;

    const failure = await captureFailure(createEngineFactory(dependencies));

    assert.strictEqual(failure.code, "GENERATED_PROTOCOL_ERROR");
    assert.strictEqual(failure.operation, "initialize");
  });

  QUnit.test("missing WebAssembly capability stops before module initialization", async (assert) => {
    let moduleInitializations = 0;
    const failure = await captureFailure(
      createEngineFactory({
        ...compatibleDependencies(),
        hasRequiredWebAssembly: () => false,
        async initializeGeneratedModule() {
          moduleInitializations += 1;
          return { memory: {} };
        },
      }),
    );

    assert.strictEqual(failure.code, "UNSUPPORTED_RUNTIME_CAPABILITY");
    assert.strictEqual(failure.operation, "initialize");
    assert.strictEqual(moduleInitializations, 0, "no module work starts");
  });

  QUnit.test("module initialization uses a redacted WASM load failure", async (assert) => {
    const secret = "private-generated-asset-path"; // # gitleaks:allow
    const loadFailure = await captureFailure(
      createEngineFactory({
        ...compatibleDependencies(),
        async initializeGeneratedModule() {
          throw new Error(secret);
        },
      }),
    );

    assert.strictEqual(loadFailure.code, "WASM_LOAD_FAILED");
    assert.false(loadFailure.serialized.includes(secret));
  });

  QUnit.test("an incompatible generated module output is a protocol error", async (assert) => {
    const failure = await captureFailure(
      createEngineFactory({
        ...compatibleDependencies(),
        initializeGeneratedModule: async () => ({}),
      }),
    );

    assert.strictEqual(failure.code, "GENERATED_PROTOCOL_ERROR");
    assert.strictEqual(failure.operation, "initialize");
  });

  QUnit.test(
    "successful module readiness is memoized but clients remain isolated",
    async (assert) => {
      let moduleInitializations = 0;
      let clientInitializations = 0;
      const createEngine = createEngineFactory({
        ...compatibleDependencies(),
        async initializeGeneratedModule() {
          moduleInitializations += 1;
          return { memory: {} };
        },
        async initializeGeneratedClient() {
          clientInitializations += 1;
          return generatedClient();
        },
      });

      const first = await createEngine(options);
      const second = await createEngine(options);

      assert.strictEqual(moduleInitializations, 1);
      assert.strictEqual(clientInitializations, 2);
      await first.close();
      await second.close();
    },
  );

  QUnit.test("a failed module attempt is not cached", async (assert) => {
    let moduleInitializations = 0;
    const createEngine = createEngineFactory({
      ...compatibleDependencies(),
      async initializeGeneratedModule() {
        moduleInitializations += 1;
        if (moduleInitializations === 1) {
          throw new Error("repairable module failure");
        }
        return { memory: {} };
      },
    });

    const failure = await captureFailure(createEngine);
    const recovered = await createEngine(options);

    assert.strictEqual(failure.code, "WASM_LOAD_FAILED");
    assert.strictEqual(moduleInitializations, 2);
    await recovered.close();
  });

  QUnit.test("opaque generated client rejection is an initialization failure", async (assert) => {
    const secret = "private-policy-bootstrap"; // # gitleaks:allow
    const failure = await captureFailure(
      createEngineFactory({
        ...compatibleDependencies(),
        async initializeGeneratedClient() {
          throw new Error(secret);
        },
      }),
    );

    assert.strictEqual(failure.code, "INITIALIZATION_FAILED");
    assert.strictEqual(failure.operation, "initialize");
    assert.false(failure.serialized.includes(secret));
  });
}
