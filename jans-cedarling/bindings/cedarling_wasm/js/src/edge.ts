import wasmModule from "./cedarling_wasm_bg.wasm?module";
import type { InitOutput } from "@generated/cedarling_wasm.js";

import { createRuntime } from "./runtime.js";

const cedarling = createRuntime(async () => wasmModule);

export const init: typeof import("@generated/cedarling_wasm.js").init =
  cedarling.init;
export const initSync: typeof import("@generated/cedarling_wasm.js").initSync =
  cedarling.initSync;
export const initWasm: () => Promise<InitOutput> = cedarling.initWasm;
export const initFromArchiveBytes: typeof import("@generated/cedarling_wasm.js").initFromArchiveBytes =
  cedarling.initFromArchiveBytes;
export { initWasm as default };
export type * from "@generated/cedarling_wasm.js";
