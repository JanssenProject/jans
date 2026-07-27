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
  type EngineDependencies,
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

/**
 * Resolves the generated package through the host module resolver.
 *
 * `undefined` means the dependency is unavailable. Callers convert this into a
 * stable SDK error so package paths and resolver details never cross the public
 * boundary.
 */
function resolveGeneratedPackage(): ResolvedGeneratedPackage | undefined {
  let gluePath: string;
  try {
    gluePath = localRequire.resolve("@janssenproject/cedarling_wasm");
  } catch {
    return undefined;
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
 */
async function importGeneratedModule(
  gluePath: string,
): Promise<CedarlingWasmModule> {
  return (await import(
    pathToFileURL(gluePath).href
  )) as CedarlingWasmModule;
}

/**
 * Node engine dependency boundary retained as a named type for focused tests.
 *
 * @internal
 */
export type NodeEngineDependencies = EngineDependencies;

/**
 * Creates the shared engine factory with Node-family host operations.
 *
 * @internal
 */
export const createNodeEngineFactory = createEngineFactory;

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
export const createNodeEngine: EngineFactory = createNodeEngineFactory({
  hasRequiredWebAssembly: () =>
    typeof WebAssembly === "object" &&
    typeof WebAssembly.Module === "function" &&
    typeof WebAssembly.Instance === "function",
  initializeGeneratedModule: async () => {
    const resolved = resolveGeneratedPackage();
    if (resolved === undefined) {
      throw new Error("The generated Cedarling package is not resolvable.");
    }
    const wasmBytes = readGeneratedWasmBytes(resolved);
    if (wasmBytes === undefined || wasmBytes.byteLength === 0) {
      throw new Error("The generated Cedarling WASM asset is not readable.");
    }
    const module = await importGeneratedModule(resolved.gluePath);
    return module.__wasm ?? module.initSync({ module: wasmBytes });
  },
  initializeGeneratedClient: async (config) => {
    const resolved = resolveGeneratedPackage();
    if (resolved === undefined) {
      throw new Error("The generated Cedarling package is not resolvable.");
    }
    const module = await importGeneratedModule(resolved.gluePath);
    return module.init(config);
  },
  initializeGeneratedArchiveClient: async (config, bytes) => {
    const resolved = resolveGeneratedPackage();
    if (resolved === undefined) {
      throw new Error("The generated Cedarling package is not resolvable.");
    }
    const module = await importGeneratedModule(resolved.gluePath);
    return module.init_from_archive_bytes(config, bytes);
  },
});
