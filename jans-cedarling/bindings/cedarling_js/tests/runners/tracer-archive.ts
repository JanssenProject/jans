/**
 * Bundle-embedded tracer archive bytes for sandboxed runners.
 *
 * Only runners whose host cannot read fixture files import this module; the
 * esbuild binary loader inlines the `.cjar` asset at bundle time.
 */
import bytes from "../fixtures/tracer-policy-store.cjar";

/** Returns a fresh copy of the embedded tracer `.cjar` bytes. */
export function tracerArchiveBytes(): Uint8Array {
  return new Uint8Array(bytes);
}
