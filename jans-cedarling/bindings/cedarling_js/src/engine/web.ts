/**
 * Browser construction boundary for the generated `cedarling_wasm` package.
 *
 * The wasm-bindgen Web target resolves its adjacent `.wasm` asset relative to
 * the generated JavaScript module and fetches it through the standard Web
 * loader contract. The SDK therefore delegates browser asset delivery to the
 * generated package rather than patching `fetch` or deriving filesystem paths.
 *
 * Authoritative loader reference:
 * https://rustwasm.github.io/docs/wasm-bindgen/reference/deployment.html
 */
import initializeGeneratedModule, {
  init as initializeGeneratedClient,
  init_from_archive_bytes as initializeGeneratedArchiveClient,
} from "@janssenproject/cedarling_wasm";

import { createEngineFactory } from "./factory.js";
import type { EngineFactory } from "./engine.js";

/** Once-per-realm browser engine factory used by the package root. */
export const createWebEngine: EngineFactory = createEngineFactory({
  hasRequiredWebAssembly: () =>
    typeof WebAssembly === "object" &&
    typeof WebAssembly.instantiate === "function" &&
    typeof WebAssembly.Instance === "function",
  initializeGeneratedModule,
  initializeGeneratedClient,
  initializeGeneratedArchiveClient,
});
