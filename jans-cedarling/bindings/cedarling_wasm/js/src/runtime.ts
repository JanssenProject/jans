import {
  init as generatedInit,
  initFromArchiveBytes as generatedInitFromArchiveBytes,
  initSync as generatedInitSync,
} from "cedarling:generated-glue";
import type {
  InitInput,
  InitOutput,
  SyncInitInput,
} from "../../pkg/cedarling_wasm.js";

type GeneratedInitWasm = typeof import("../../pkg/cedarling_wasm.js").default;
type InitWasmInput = Parameters<GeneratedInitWasm>[0];
type InitializeInput = (input: InitInput | Promise<InitInput>) => Promise<InitOutput>;

export function createRuntime(
  loadModule: () => Promise<WebAssembly.Module>,
  initializeInput?: InitializeInput,
) {
  let initialized: Promise<InitOutput> | undefined;

  function initialize(operation: () => Promise<InitOutput>): Promise<InitOutput> {
    if (initialized === undefined) {
      initialized = operation().catch((error: unknown) => {
        initialized = undefined;
        throw error;
      });
    }
    return initialized;
  }

  function initWasm(input?: InitWasmInput): Promise<InitOutput> {
    const moduleOrPath = input !== undefined &&
      typeof input === "object" &&
      input !== null &&
      "module_or_path" in input
      ? input.module_or_path
      : input;
    return initialize(() => {
      if (moduleOrPath === undefined) {
        return loadModule().then((module) => generatedInitSync({ module }));
      }
      if (initializeInput === undefined) {
        throw new TypeError("Cedarling edge initialization accepts no input");
      }
      return initializeInput(moduleOrPath);
    });
  }

  function initSync(
    input: { module: SyncInitInput } | SyncInitInput,
  ): InitOutput {
    const output = generatedInitSync(input);
    initialized = Promise.resolve(output);
    return output;
  }

  return Object.freeze({
    initWasm,
    initSync,
    async init(config: Parameters<typeof generatedInit>[0]): Promise<Awaited<ReturnType<typeof generatedInit>>> {
      await initWasm();
      return generatedInit(config);
    },
    async initFromArchiveBytes(
      config: Parameters<typeof generatedInitFromArchiveBytes>[0],
      archiveBytes: Parameters<typeof generatedInitFromArchiveBytes>[1],
    ): Promise<Awaited<ReturnType<typeof generatedInitFromArchiveBytes>>> {
      await initWasm();
      return generatedInitFromArchiveBytes(config, archiveBytes);
    },
  });
}
