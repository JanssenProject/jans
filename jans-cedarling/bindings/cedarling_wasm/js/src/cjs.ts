import { initSync as initializeGeneratedModule } from "cedarling:generated-glue";
import type {
  InitInput,
  InitOutput,
} from "../../pkg/cedarling_wasm.js";
import loadWasmBytes from "cedarling:wasm-file";

import { createRuntime } from "./runtime.js";

async function initializeInput(input: InitInput | Promise<InitInput>): Promise<InitOutput> {
  const value = await input;
  if (
    value instanceof WebAssembly.Module ||
    value instanceof ArrayBuffer ||
    ArrayBuffer.isView(value)
  ) {
    return initializeGeneratedModule(value);
  }
  const response = value instanceof Response ? value : await fetch(value);
  if (!response.ok) {
    throw new Error(`Cedarling WASM asset could not be loaded: ${response.status}`);
  }
  return initializeGeneratedModule(await response.arrayBuffer());
}

const cedarling = createRuntime(async () => {
  const bytes = await loadWasmBytes();
  const copy = new Uint8Array(bytes.byteLength);
  copy.set(bytes);
  return WebAssembly.compile(copy);
}, initializeInput);

export const init: typeof import("../../pkg/cedarling_wasm.js").init = cedarling.init;
export const initSync: typeof import("../../pkg/cedarling_wasm.js").initSync = cedarling.initSync;
export const initWasm: typeof import("../../pkg/cedarling_wasm.js").default = cedarling.initWasm;
export const initFromArchiveBytes:
  typeof import("../../pkg/cedarling_wasm.js").initFromArchiveBytes =
    cedarling.initFromArchiveBytes;
export { initWasm as default };
export type * from "../../pkg/cedarling_wasm.js";
