import type { PreparedCedarlingOptions } from "../configuration/prepare.js";
import { createSdkError, isSdkErrorCode } from "../errors/errors.js";
import type { CedarlingEngine, EngineFactory } from "./engine.js";
import {
  createGeneratedEngine,
  hasGeneratedModuleOutput,
} from "./generated.js";

/**
 * Host-specific operations required to load and construct generated Cedarling.
 *
 * Runtime adapters provide only this narrow boundary. The shared factory owns
 * capability checks, once-per-realm module readiness, retry semantics, policy
 * archive loading, generated-protocol validation, and stable public errors.
 *
 * @internal
 */
export interface EngineDependencies {
  /** Reports whether the runtime exposes the required WebAssembly APIs. */
  readonly hasRequiredWebAssembly: () => boolean;

  /** Loads or instantiates the generated module for the selected runtime. */
  readonly initializeGeneratedModule: () => Promise<unknown>;

  /** Constructs Cedarling from a validated bootstrap configuration. */
  readonly initializeGeneratedClient: (
    config: Readonly<Record<string, unknown>>,
  ) => Promise<unknown>;

  /** Constructs Cedarling from configuration and copied policy-archive bytes. */
  readonly initializeGeneratedArchiveClient: (
    config: Readonly<Record<string, unknown>>,
    bytes: Uint8Array,
  ) => Promise<unknown>;
}

/** Checks the runtime dependency protocol before asynchronous work begins. */
function hasDependencyProtocol(
  dependencies: EngineDependencies,
): boolean {
  try {
    return (
      typeof dependencies.hasRequiredWebAssembly === "function" &&
      typeof dependencies.initializeGeneratedModule === "function" &&
      typeof dependencies.initializeGeneratedClient === "function" &&
      typeof dependencies.initializeGeneratedArchiveClient === "function"
    );
  } catch {
    return false;
  }
}

/**
 * Builds an engine factory around one host-specific generated-module boundary.
 *
 * Successful module readiness is shared by all clients created through the
 * returned factory. A failed load is forgotten, allowing a later call to retry
 * after the host repairs asset delivery.
 *
 * @internal
 */
export function createEngineFactory(
  dependencies: EngineDependencies,
): EngineFactory {
  let moduleInitialization: Promise<void> | undefined;

  async function initializeModuleOnce(): Promise<void> {
    if (moduleInitialization === undefined) {
      const attempt = (async () => {
        if (!hasDependencyProtocol(dependencies)) {
          throw createSdkError("GENERATED_PROTOCOL_ERROR", "initialize");
        }

        let output: unknown;
        try {
          output = await dependencies.initializeGeneratedModule();
        } catch {
          throw createSdkError("WASM_LOAD_FAILED", "initialize");
        }
        if (!hasGeneratedModuleOutput(output)) {
          throw createSdkError("GENERATED_PROTOCOL_ERROR", "initialize");
        }
      })();

      moduleInitialization = attempt.catch((error: unknown) => {
        moduleInitialization = undefined;
        if (
          isSdkErrorCode(error, [
            "UNSUPPORTED_RUNTIME_CAPABILITY",
            "WASM_LOAD_FAILED",
            "GENERATED_PROTOCOL_ERROR",
          ])
        ) {
          throw error;
        }
        throw createSdkError("WASM_LOAD_FAILED", "initialize");
      });
    }

    return moduleInitialization;
  }

  return async (
    options: PreparedCedarlingOptions,
  ): Promise<CedarlingEngine> => {
    let hasWebAssembly = false;
    try {
      hasWebAssembly = dependencies.hasRequiredWebAssembly();
    } catch {
      // A failed capability probe is indistinguishable from absence.
    }
    if (!hasWebAssembly) {
      throw createSdkError(
        "UNSUPPORTED_RUNTIME_CAPABILITY",
        "initialize",
        { details: { runtimeCapability: "webAssembly" } },
      );
    }

    await initializeModuleOnce();

    let archiveBytes: Uint8Array | undefined;
    if (options.policyStore.type === "archive") {
      archiveBytes = options.policyStore.bytes;
    } else if (options.policyStore.type === "loader") {
      let loaded: unknown;
      try {
        loaded = await options.policyStore.load();
      } catch {
        throw createSdkError(
          "POLICY_LOADER_FAILED",
          "initialize",
          { details: { sourceType: "loader" } },
        );
      }
      if (!(loaded instanceof Uint8Array) || loaded.byteLength === 0) {
        throw createSdkError(
          "POLICY_LOADER_FAILED",
          "initialize",
          { details: { sourceType: "loader" } },
        );
      }
      archiveBytes = new Uint8Array(loaded);
    }

    let generatedValue: unknown;
    try {
      generatedValue =
        archiveBytes === undefined
          ? await dependencies.initializeGeneratedClient(
              options.bootstrapConfig,
            )
          : await dependencies.initializeGeneratedArchiveClient(
              options.bootstrapConfig,
              archiveBytes,
            );
    } catch {
      throw createSdkError("INITIALIZATION_FAILED", "initialize");
    }

    const engine = createGeneratedEngine(generatedValue);
    if (engine === undefined) {
      throw createSdkError("GENERATED_PROTOCOL_ERROR", "initialize");
    }
    return engine;
  };
}
