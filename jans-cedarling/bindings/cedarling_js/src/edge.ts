import wasmModule from "./cedarling_wasm_bg.wasm?module";

import { createRuntime } from "./runtime.js";

const cedarling = createRuntime(async () => wasmModule);

/** Initializes Cedarling from raw bootstrap properties. */
export const init = cedarling.init;
/** Initializes Cedarling from raw bootstrap properties and Cedar Archive bytes. */
export const initFromArchiveBytes = cedarling.initFromArchiveBytes;
export type * from "./types.js";
