import type { IsolatedSdkEntry, RuntimeFixtures } from "../run.js";
import { createCedarling } from "@janssenproject/cedarling";
import { tracerArchiveBytes } from "./tracer-archive.js";

/**
 * Runtime fixtures for sandboxed hosts (workerd, edge).
 *
 * These hosts cannot stage isolated installations, manipulate packaged
 * files, or host loopback servers, so only the fixtures used by the portable
 * contract subset are implemented for real:
 *
 * - `loadTracerArchive` returns bundle-embedded `.cjar` bytes;
 * - `withMissingWebAssembly` removes the host WebAssembly capability for the
 *   duration of the callback, exercising the SDK capability probe.
 *
 * The remaining fixtures are unreachable in the portable subset and stay
 * empty by construction.
 */
export function createSandboxedRuntimeFixtures(
  runtime: string,
): RuntimeFixtures {
  const sdk: IsolatedSdkEntry = { createCedarling };

  return {
    runtime,

    async loadTracerArchive(): Promise<Uint8Array> {
      return tracerArchiveBytes();
    },

    async withMissingWasmAsset(): Promise<void> {},

    async withMissingWebAssembly(run): Promise<void> {
      const descriptor = Object.getOwnPropertyDescriptor(
        globalThis,
        "WebAssembly",
      );

      try {
        Object.defineProperty(globalThis, "WebAssembly", {
          configurable: true,
          enumerable: false,
          value: undefined,
          writable: true,
        });
        await run(sdk);
      } finally {
        if (descriptor === undefined) {
          Reflect.deleteProperty(globalThis, "WebAssembly");
        } else {
          Object.defineProperty(globalThis, "WebAssembly", descriptor);
        }
      }
    },

    async withPolicyServer(): Promise<void> {},
  };
}
