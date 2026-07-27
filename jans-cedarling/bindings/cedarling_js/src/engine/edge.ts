/**
 * Vercel Edge (`edge-light`) construction boundary for the generated
 * `cedarling_wasm` package.
 *
 * The Vercel Edge Runtime prohibits compiling WebAssembly from ordinary byte
 * buffers. Its build pipeline precompiles a statically imported
 * `.wasm?module` artifact; the imported source is passed unchanged to the
 * generated asynchronous initializer, which performs the permitted
 * imported-source instantiation. This module is only reachable through the
 * `edge-light` export condition, so the static artifact import never enters
 * another runtime's module graph.
 *
 * Vercel documents both the byte-compilation restriction and the required
 * static import:
 * https://vercel.com/docs/functions/runtimes/edge#unsupported-apis
 */
import initializeGeneratedModule, {
  init as initializeGeneratedClient,
  init_from_archive_bytes as initializeGeneratedArchiveClient,
} from "@janssenproject/cedarling_wasm";
import wasmSource from "@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm?module";

import type { EngineFactory } from "./engine.js";
import { createEngineFactory } from "./factory.js";

/** Once-per-realm Vercel Edge engine factory used by the edge entry. */
export const createEdgeEngine: EngineFactory = createEngineFactory({
  hasRequiredWebAssembly: () =>
    typeof WebAssembly === "object" &&
    typeof WebAssembly.Module === "function" &&
    typeof WebAssembly.Instance === "function",
  initializeGeneratedModule: async () =>
    initializeGeneratedModule({ module_or_path: wasmSource }),
  initializeGeneratedClient: async (config) =>
    initializeGeneratedClient(config),
  initializeGeneratedArchiveClient: async (config, bytes) =>
    initializeGeneratedArchiveClient(config, bytes),
});
