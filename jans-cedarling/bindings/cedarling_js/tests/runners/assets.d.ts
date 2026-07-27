/**
 * Ambient declaration for bundle-embedded binary test assets.
 *
 * Sandbox runners (workerd, edge) cannot read fixture files from a
 * filesystem, so the tracer archive is embedded at bundle time through the
 * esbuild binary loader (`--loader:.cjar=binary`).
 */
declare module "*.cjar" {
  const bytes: Uint8Array;
  export default bytes;
}
