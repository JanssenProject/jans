import {
  init as initializeGeneratedCedarling,
  init_from_archive_bytes as initializeGeneratedArchiveCedarling,
  initSync as initializeGeneratedModule,
} from "@janssenproject/cedarling_wasm";

import { createCedarlingApi } from "./client.js";
import type { CedarlingApi } from "./types.js";

/** Binds the generated binding to one runtime-specific compiled WASM module. */
export function createRuntime(loadModule: () => Promise<WebAssembly.Module>): CedarlingApi {
  return createCedarlingApi(
    async () => {
      initializeGeneratedModule({ module: await loadModule() });
    },
    {
      init: initializeGeneratedCedarling,
      initFromArchiveBytes: initializeGeneratedArchiveCedarling,
    },
  );
}
