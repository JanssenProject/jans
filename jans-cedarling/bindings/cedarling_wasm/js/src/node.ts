import initializeGeneratedWasm from "cedarling:generated-glue";
import loadWasmBytes from "cedarling:wasm-file";

import { createRuntime } from "./runtime.js";

const cedarling = createRuntime(async () => {
  const bytes = await loadWasmBytes();
  const copy = new Uint8Array(bytes.byteLength);
  copy.set(bytes);
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
