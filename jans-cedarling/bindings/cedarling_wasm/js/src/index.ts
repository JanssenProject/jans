import initializeGeneratedWasm from "cedarling:generated-glue";
import wasmBytes from "cedarling:wasm-bytes";

import { createRuntime } from "./runtime.js";

const cedarling = createRuntime(async () => {
  if (typeof wasmBytes === "string") {
    const response = await fetch(wasmBytes);
    if (!response.ok) throw new Error("Cedarling WASM asset could not be loaded");
    return WebAssembly.compile(await response.arrayBuffer());
  }
  const copy = new Uint8Array(wasmBytes.byteLength);
  copy.set(wasmBytes);
  return WebAssembly.compile(copy);
}, initializeGeneratedWasm);

export const init: typeof import("../../pkg/cedarling_wasm.js").init = cedarling.init;
export const initSync: typeof import("../../pkg/cedarling_wasm.js").initSync = cedarling.initSync;
export const initWasm: typeof import("../../pkg/cedarling_wasm.js").default = cedarling.initWasm;
export const initFromArchiveBytes:
  typeof import("../../pkg/cedarling_wasm.js").initFromArchiveBytes =
    cedarling.initFromArchiveBytes;
export { initWasm as default };
export type * from "../../pkg/cedarling_wasm.js";
