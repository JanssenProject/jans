/**
 * Node-family public entry point for the Cedarling JavaScript SDK.
 *
 * Node.js selects this entry through the standard `node` package condition;
 * Bun, Deno, and Electron main or utility processes also use this adapter when
 * they resolve that condition. The interface is identical to the Web entry;
 * only private WebAssembly loading differs.
 *
 * Package-condition semantics:
 * https://nodejs.org/api/packages.html#conditional-exports
 *
 * @packageDocumentation
 */

import { createCedarlingForEngine } from "../client/client.js";
import { createNodeEngine } from "../engine/node.js";

/**
 * Creates Cedarling through the Node-family adapter, which resolves the
 * generated package with the host module resolver and initializes its adjacent
 * WebAssembly asset from bytes.
 */
export const createCedarling =
  createCedarlingForEngine(createNodeEngine);

export type * from "../index.js";
