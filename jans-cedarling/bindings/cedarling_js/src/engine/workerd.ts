/**
 * Workerd (Cloudflare Workers) construction boundary for the generated
 * `cedarling_wasm` package.
 *
 * Workerd prohibits runtime byte compilation (`WebAssembly.compile`,
 * `compileStreaming`, and instantiation from a byte buffer) and cannot fetch
 * the generated package's adjacent asset URL. The deployment bundler
 * (Wrangler) precompiles the statically imported `.wasm` artifact into a
 * `WebAssembly.Module`, and the generated synchronous initializer
 * instantiates it. This module is only reachable through the `workerd`
 * export condition, so the static artifact import never enters another
 * runtime's module graph.
 *
 * Cloudflare documents the prohibited byte-compilation APIs and Wrangler's
 * default `.wasm` module bundling:
 * https://developers.cloudflare.com/workers/runtime-apis/web-standards/
 * https://developers.cloudflare.com/workers/runtime-apis/webassembly/javascript/#bundling
 */
import {
  init as initializeGeneratedClient,
  init_from_archive_bytes as initializeGeneratedArchiveClient,
  initSync,
} from "@janssenproject/cedarling_wasm";
import wasmModule from "@janssenproject/cedarling_wasm/cedarling_wasm_bg.wasm";

import type { EngineFactory } from "./engine.js";
import { createEngineFactory } from "./factory.js";

/** Once-per-realm workerd engine factory used by the workerd entry. */
export const createWorkerdEngine: EngineFactory = createEngineFactory({
  hasRequiredWebAssembly: () =>
    typeof WebAssembly === "object" &&
    typeof WebAssembly.Module === "function" &&
    typeof WebAssembly.Instance === "function",
  initializeGeneratedModule: async () =>
    initSync({ module: wasmModule }),
  initializeGeneratedClient: async (config) =>
    initializeGeneratedClient(config),
  initializeGeneratedArchiveClient: async (config, bytes) =>
    initializeGeneratedArchiveClient(config, bytes),
});
