import wasmModule from "./cedarling_wasm_bg.wasm?module";

import type { EngineFactory } from "./engine.js";
import { createGeneratedEngineFactory } from "./generated-loader.js";

/** Engine factory for hosts that supply statically compiled WASM Modules. */
export const createEdgeEngine: EngineFactory =
  createGeneratedEngineFactory(async () => wasmModule);
