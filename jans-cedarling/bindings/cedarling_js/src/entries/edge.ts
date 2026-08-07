/**
 * Vercel Edge public entry point for the Cedarling JavaScript SDK.
 *
 * Selected by the `edge-light` export condition (chosen automatically by
 * Vercel/Next.js Edge Runtime builds). Exposes the identical public contract
 * as every other runtime entry; only the private engine differs.
 *
 * @packageDocumentation
 */

import { createCedarlingForEngine } from "../client/client.js";
import { createEdgeEngine } from "../engine/edge.js";

/**
 * Creates Cedarling from a build-time precompiled WebAssembly source through
 * the generated binding's asynchronous initializer.
 *
 * @example
 * ```ts
 * import { createCedarling } from "@janssenproject/cedarling";
 *
 * const created = await createCedarling({
 *   applicationName: "task-manager",
 *   policyStore: {
 *     type: "inline",
 *     document: policyStoreDocument,
 *   },
 * });
 * ```
 */
export const createCedarling =
  createCedarlingForEngine(createEdgeEngine);

export type * from "../index.js";
