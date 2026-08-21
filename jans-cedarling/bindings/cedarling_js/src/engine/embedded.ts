import wasmBytes from "cedarling:wasm-bytes";

import type { EngineFactory } from "./engine.js";
import { hasWebAssemblyConstructors } from "./factory.js";
import { createGeneratedEngineFactory } from "./generated-loader.js";

function hasEmbeddedWebAssembly(): boolean {
  return hasWebAssemblyConstructors() &&
    typeof WebAssembly.compile === "function";
}

/** Engine factory for runtimes that compile the package-embedded WASM bytes. */
export const createEmbeddedEngine: EngineFactory =
  createGeneratedEngineFactory(
    () => WebAssembly.compile(wasmBytes),
    hasEmbeddedWebAssembly,
  );
