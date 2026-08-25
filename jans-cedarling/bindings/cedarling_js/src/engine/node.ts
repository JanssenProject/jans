import loadWasmBytes from "cedarling:wasm-file";

import type { EngineFactory } from "./engine.js";
import { hasWebAssemblyConstructors } from "./factory.js";
import { createGeneratedEngineFactory } from "./generated-loader.js";

/** Engine factory for Node-family runtimes that load the packaged WASM file. */
export const createNodeEngine: EngineFactory = createGeneratedEngineFactory(
  async () => WebAssembly.compile(await loadWasmBytes()),
  hasWebAssemblyConstructors,
);
