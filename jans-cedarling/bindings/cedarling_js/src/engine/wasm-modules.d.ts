/**
 * Ambient declarations for build-time WebAssembly artifact imports.
 *
 * These module specifiers are resolved by the deployment bundler of the
 * target runtime, never by Node or TypeScript module resolution:
 *
 * - `*.wasm` is the Cloudflare Workers / Wrangler convention. Wrangler's
 *   default module rules map `**\/*.wasm` to a precompiled
 *   `WebAssembly.Module` (CompiledWasm), which the runtime instantiates
 *   synchronously without byte compilation.
 * - `*.wasm?module` is the Vercel Edge / Next.js convention. The build
 *   pipeline precompiles the asset and yields the imported source that the
 *   generated asynchronous initializer accepts unchanged.
 *
 * Authoritative runtime references:
 * https://developers.cloudflare.com/workers/runtime-apis/webassembly/javascript/#bundling
 * https://vercel.com/docs/functions/runtimes/edge#unsupported-apis
 *
 * Nothing here appears in the published declarations: the runtime entries
 * re-export only `createCedarling` and the shared public types.
 */
declare module "*.wasm" {
  const module: WebAssembly.Module;
  export default module;
}

declare module "*.wasm?module" {
  const source: WebAssembly.Module;
  export default source;
}
