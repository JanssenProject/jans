/**
 * Workerd public entry point for the Cedarling JavaScript SDK.
 *
 * Selected by the `workerd` export condition (chosen automatically by
 * wrangler/workerd builds). Exposes the identical public contract as every
 * other runtime entry; only the private engine differs.
 *
 * @packageDocumentation
 */

import { createCedarlingForEngine } from "../client/client.js";
import { createWorkerdEngine } from "../engine/workerd.js";

/**
 * Creates Cedarling from a deployment-bundled, precompiled WebAssembly
 * module through the generated binding's synchronous initializer.
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
  createCedarlingForEngine(createWorkerdEngine);

export type * from "../index.js";
