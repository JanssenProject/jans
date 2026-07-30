/// <reference types="node" />

/**
 * Node-family construction boundary for the generated `cedarling_wasm`
 * package.
 *
 * This adapter resolves the generated dependency from the installed SDK,
 * reads its adjacent WASM asset, and passes those bytes to the generated
 * synchronous initializer. It does not depend on a consumer working directory,
 * a global `fetch` patch, or repository-relative paths.
 *
 * `createRequire(import.meta.url)` is Node's documented ESM mechanism for
 * module-relative CommonJS resolution:
 * https://nodejs.org/api/module.html#modulecreaterequirefilename
 */
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import path from "node:path";
import { pathToFileURL } from "node:url";

import type { EngineFactory } from "./engine.js";
import {
  createEngineFactory,
  hasWebAssemblyConstructors,
} from "./factory.js";

/**
 * Module reference for dependency resolution.
 *
 * The ESM build supplies `import.meta.url`; the bundled CommonJS build supplies
 * `__filename`. In both formats resolution begins at the installed SDK rather
 * than the application working directory.
 */
const moduleReference =
  typeof __filename !== "undefined" ? __filename : import.meta.url;

const localRequire = createRequire(moduleReference);

/** Absolute generated-package locations resolved by the host module loader. */
interface ResolvedGeneratedPackage {
  /** Generated ESM glue entry. */
  readonly gluePath: string;

  /** WASM asset distributed beside the generated glue. */
  readonly wasmPath: string;
}

/** Resolves the generated package without exposing host resolver details. */
function resolveRequiredGeneratedPackage(): ResolvedGeneratedPackage {
  let gluePath: string;
  try {
    gluePath = localRequire.resolve("@janssenproject/cedarling_wasm");
  } catch {
    throw new Error("The generated Cedarling package is not resolvable.");
  }
  return {
    gluePath,
    wasmPath: path.join(
      path.dirname(gluePath),
      "cedarling_wasm_bg.wasm",
    ),
  };
}

/** Generated module methods consumed by this runtime adapter. */
interface CedarlingWasmModule {
  readonly __wasm: unknown;
  initSync(module: { module: Uint8Array }): unknown;
  init(config: Readonly<Record<string, unknown>>): Promise<unknown>;
  init_from_archive_bytes(
    config: Readonly<Record<string, unknown>>,
    archiveBytes: Uint8Array,
  ): Promise<unknown>;
}

/**
 * Imports generated ESM glue through a file URL.
 *
 * Node's ESM loader accepts `file:` URLs, and conversion avoids platform-
 * specific path syntax in dynamic `import()`:
 * https://nodejs.org/api/esm.html#urls
 *
 * Passing an already resolved package lets module initialization verify the
 * adjacent WASM asset before importing generated glue.
 */
async function importGeneratedPackage(
  resolved = resolveRequiredGeneratedPackage(),
): Promise<CedarlingWasmModule> {
  return (await import(
    pathToFileURL(resolved.gluePath).href
  )) as CedarlingWasmModule;
}

/**
 * Reads the installed WASM asset into detached bytes.
 *
 * `undefined` deliberately hides filesystem and package-layout details from
 * public initialization errors.
 */
function readGeneratedWasmBytes(
  resolved: ResolvedGeneratedPackage,
): Uint8Array | undefined {
  try {
    return new Uint8Array(readFileSync(resolved.wasmPath));
  } catch {
    return undefined;
  }
}

/** Once-per-realm Node-family engine factory used by the Node entry. */
export const createNodeEngine: EngineFactory = createEngineFactory({
  hasRequiredWebAssembly: hasWebAssemblyConstructors,
  initializeGeneratedModule: async () => {
    const resolved = resolveRequiredGeneratedPackage();
    const wasmBytes = readGeneratedWasmBytes(resolved);
    if (wasmBytes === undefined || wasmBytes.byteLength === 0) {
      throw new Error("The generated Cedarling WASM asset is not readable.");
    }
    const module = await importGeneratedPackage(resolved);
    return module.__wasm ?? module.initSync({ module: wasmBytes });
  },
  initializeGeneratedClient: async (config) => {
    const module = await importGeneratedPackage();
    return module.init(config);
  },
  initializeGeneratedArchiveClient: async (config, bytes) => {
    const module = await importGeneratedPackage();
    return module.init_from_archive_bytes(config, bytes);
  },
});
