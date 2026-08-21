declare module "cedarling:wasm-bytes" {
  const bytes: BufferSource;
  export default bytes;
}

declare module "*.wasm?module" {
  const module: WebAssembly.Module;
  export default module;
}
