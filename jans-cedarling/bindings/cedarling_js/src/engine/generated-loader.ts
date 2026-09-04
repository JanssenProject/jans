import {
  init as initializeGeneratedClient,
  init_from_archive_bytes as initializeGeneratedArchiveClient,
  initSync as initializeGeneratedModule,
} from "@janssenproject/cedarling_wasm";

import type { EngineFactory } from "./engine.js";
import {
  createEngineFactory,
  hasWebAssemblyConstructors,
} from "./factory.js";

type GeneratedModuleLoader = () => Promise<WebAssembly.Module>;

/** Binds generated Cedarling glue to one private WASM Module loader. */
export function createGeneratedEngineFactory(
  loadModule: GeneratedModuleLoader,
  hasRequiredWebAssembly = hasWebAssemblyConstructors,
): EngineFactory {
  return createEngineFactory({
    hasRequiredWebAssembly,
    initializeGeneratedModule: async () => initializeGeneratedModule({
      module: await loadModule(),
    }),
    initializeGeneratedClient,
    initializeGeneratedArchiveClient,
  });
}
