declare module "cedarling:wasm-bytes" {
  const bytes: BufferSource | string;
  export default bytes;
}

declare module "cedarling:wasm-file" {
  const load: () => Promise<BufferSource>;
  export default load;
}

declare module "*.wasm?module" {
  const module: WebAssembly.Module;
  export default module;
}
