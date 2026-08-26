import loadWasmBytes from "cedarling:wasm-file";

import { createRuntime } from "./runtime.js";

const cedarling = createRuntime(async () =>
  WebAssembly.compile(await loadWasmBytes()),
);

/** Initializes Cedarling from raw bootstrap properties. */
export const init = cedarling.init;
/** Initializes Cedarling from raw bootstrap properties and Cedar Archive bytes. */
export const initFromArchiveBytes = cedarling.initFromArchiveBytes;
export type * from "./types.js";
