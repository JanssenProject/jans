import wasmBytes from "cedarling:wasm-bytes";

import type { EngineFactory } from "./engine.js";
import { hasWebAssemblyConstructors } from "./factory.js";
import { createGeneratedEngineFactory } from "./generated-loader.js";

function hasEmbeddedWebAssembly(): boolean {
  return hasWebAssemblyConstructors() &&
    typeof WebAssembly.compile === "function";
}

async function compileEmbeddedWasm(): Promise<WebAssembly.Module> {
  if (typeof wasmBytes !== "string") {
    return WebAssembly.compile(wasmBytes);
  }
  const response = await fetch(wasmBytes);
  if (!response.ok) {
    throw new Error("Cedarling WASM asset could not be loaded");
  }
  return WebAssembly.compile(await response.arrayBuffer());
}

/** Engine factory for runtimes that compile the package-embedded WASM bytes. */
export const createEmbeddedEngine: EngineFactory =
  createGeneratedEngineFactory(
    compileEmbeddedWasm,
    hasEmbeddedWebAssembly,
  );
