declare module "cedarling:generated-glue" {
  const initWasm: typeof import("../../pkg/cedarling_wasm.js").default;
  export default initWasm;
  export const init: typeof import("../../pkg/cedarling_wasm.js").init;
  export const initFromArchiveBytes:
    typeof import("../../pkg/cedarling_wasm.js").initFromArchiveBytes;
  export const initSync: typeof import("../../pkg/cedarling_wasm.js").initSync;
}

declare module "cedarling:wasm-bytes" {
  const bytes: Uint8Array | string;
  export default bytes;
}

declare module "cedarling:wasm-file" {
  const load: () => Promise<Uint8Array>;
  export default load;
}

declare module "*.wasm?module" {
  const module: WebAssembly.Module;
  export default module;
}
