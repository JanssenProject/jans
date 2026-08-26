import wasmBytes from "cedarling:wasm-bytes";

import { createRuntime } from "./runtime.js";

const cedarling = createRuntime(async () => {
  if (typeof wasmBytes === "string") {
    const response = await fetch(wasmBytes);
    if (!response.ok) throw new Error("Cedarling WASM asset could not be loaded");
    return WebAssembly.compile(await response.arrayBuffer());
  }
  return WebAssembly.compile(wasmBytes);
});

/** Initializes Cedarling from raw bootstrap properties. */
export const init = cedarling.init;
/** Initializes Cedarling from raw bootstrap properties and Cedar Archive bytes. */
export const initFromArchiveBytes = cedarling.initFromArchiveBytes;

export type {
  AuthorizationResult,
  BatchAuthorizationResult,
  BatchItemAuthorizationResult,
  BatchItemError,
  BootstrapProperties,
  Cedarling,
  CedarlingApi,
  PolicyEvaluationDiagnostic,
} from "./types.js";
