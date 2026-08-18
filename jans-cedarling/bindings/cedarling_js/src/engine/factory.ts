import type { PreparedEngineOptions } from "../configuration/prepare.js";
import { createSdkError, isSdkErrorCode } from "../errors/errors.js";
import { errorCode } from "../errors/types.js";
import type { EngineFactory } from "./engine.js";
import {
  createGeneratedEngine,
  hasGeneratedModuleOutput,
} from "./generated.js";

export interface EngineDependencies {
  readonly hasRequiredWebAssembly: () => boolean;
  readonly initializeGeneratedModule: () => Promise<unknown>;
  readonly initializeGeneratedClient: (
    config: Readonly<Record<string, unknown>>,
  ) => Promise<unknown>;
}

export function hasWebAssemblyConstructors(): boolean {
  return (
    typeof WebAssembly === "object" &&
    typeof WebAssembly.Module === "function" &&
    typeof WebAssembly.Instance === "function"
  );
}

function hasDependencyProtocol(
  dependencies: EngineDependencies,
): boolean {
  try {
    return (
      typeof dependencies.hasRequiredWebAssembly === "function" &&
      typeof dependencies.initializeGeneratedModule === "function" &&
      typeof dependencies.initializeGeneratedClient === "function"
    );
  } catch {
    return false;
  }
}

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
          if (isSdkErrorCode(error, [errorCode.wasmLoadFailed])) throw error;
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

  return async (options: PreparedEngineOptions) => {
    if (!dependencyProtocolValid) {
      throw createSdkError(errorCode.generatedProtocolError, "initialize");
    }
    let hasWebAssembly = false;
    let capabilityFailure: unknown;
    try {
      hasWebAssembly = dependencies.hasRequiredWebAssembly();
    } catch (error: unknown) {
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

    let generatedValue: unknown;
    try {
      generatedValue = await dependencies.initializeGeneratedClient(
        options.bootstrapConfig,
      );
    } catch (error: unknown) {
      if (isSdkErrorCode(error, [
          errorCode.initializationFailed,
          errorCode.unsupportedRuntimeCapability,
          errorCode.wasmLoadFailed,
          errorCode.generatedProtocolError,
      ])) throw error;
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
