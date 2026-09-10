import type { PreparedEngineOptions } from "../configuration/prepare.js";
import { copyUint8Array } from "../configuration/validation.js";
import { createSdkError, isSdkErrorCode } from "../errors/errors.js";
import { errorCode } from "../errors/types.js";
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
  readonly hasRequiredWebAssembly: () => boolean;

  readonly initializeGeneratedModule: () => Promise<unknown>;

  readonly initializeGeneratedClient: (
    config: Readonly<Record<string, unknown>>,
  ) => Promise<unknown>;

  readonly initializeGeneratedArchiveClient: (
    config: Readonly<Record<string, unknown>>,
    bytes: Uint8Array,
  ) => Promise<unknown>;
}

/** Reports the WebAssembly constructors required by the Node-family adapter. */
export function hasWebAssemblyConstructors(): boolean {
  return (
    typeof WebAssembly === "object" &&
    typeof WebAssembly.Module === "function" &&
    typeof WebAssembly.Instance === "function"
  );
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
  const dependencyProtocolValid = hasDependencyProtocol(dependencies);
  let moduleInitialization: Promise<void> | undefined;

  async function initializeModuleOnce(): Promise<void> {
    if (moduleInitialization === undefined) {
      const attempt = (async () => {
        let output: unknown;
        try {
          output = await dependencies.initializeGeneratedModule();
        } catch (error: unknown) {
          if (isSdkErrorCode(error, [errorCode.wasmLoadFailed])) {
            throw error;
          }
          throw createSdkError(errorCode.wasmLoadFailed, "initialize", {
            rawCause: error,
          });
        }
        if (!hasGeneratedModuleOutput(output)) {
          throw createSdkError(errorCode.generatedProtocolError, "initialize");
        }
      })();

      moduleInitialization = attempt.catch((error: unknown) => {
        moduleInitialization = undefined;
        if (
          isSdkErrorCode(error, [
            errorCode.unsupportedRuntimeCapability,
            errorCode.wasmLoadFailed,
            errorCode.generatedProtocolError,
          ])
        ) {
          throw error;
        }
        throw createSdkError(errorCode.wasmLoadFailed, "initialize", {
          rawCause: error,
        });
      });
    }

    return moduleInitialization;
  }

  return async (
    options: PreparedEngineOptions,
  ): Promise<CedarlingEngine> => {
    if (!dependencyProtocolValid) {
      throw createSdkError(errorCode.generatedProtocolError, "initialize");
    }

    let hasWebAssembly = false;
    let capabilityFailure: unknown;
    try {
      hasWebAssembly = dependencies.hasRequiredWebAssembly();
    } catch (error: unknown) {
      // A failed capability probe is indistinguishable from absence.
      capabilityFailure = error;
    }
    if (!hasWebAssembly) {
      throw createSdkError(
        errorCode.unsupportedRuntimeCapability,
        "initialize",
        {
          details: { runtimeCapability: "webAssembly" },
          ...(capabilityFailure === undefined
            ? {}
            : { rawCause: capabilityFailure }),
        },
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
      } catch (error: unknown) {
        throw createSdkError(
          errorCode.policyLoaderFailed,
          "initialize",
          {
            details: { sourceType: "loader" },
            rawCause: error,
          },
        );
      }
      const bytes = copyUint8Array(loaded);
      if (bytes === undefined || bytes.byteLength === 0) {
        throw createSdkError(
          errorCode.policyLoaderFailed,
          "initialize",
          { details: { sourceType: "loader" } },
        );
      }
      archiveBytes = bytes;
    }

    let generatedValue: unknown;
    try {
      generatedValue = archiveBytes === undefined
        ? await dependencies.initializeGeneratedClient(
          options.bootstrapConfig,
        )
        : await dependencies.initializeGeneratedArchiveClient(
          options.bootstrapConfig,
          archiveBytes,
        );
    } catch (error: unknown) {
      if (
        isSdkErrorCode(error, [
          errorCode.initializationFailed,
          errorCode.unsupportedRuntimeCapability,
          errorCode.wasmLoadFailed,
          errorCode.generatedProtocolError,
        ])
      ) {
        throw error;
      }
      throw createSdkError(errorCode.initializationFailed, "initialize", {
        rawCause: error,
      });
    }

    const engine = createGeneratedEngine(generatedValue);
    if (engine === undefined) {
      throw createSdkError(errorCode.generatedProtocolError, "initialize");
    }
    return engine;
  };
}
