import { createCedarling, type CedarlingClient } from "@janssenproject/cedarling";

import { loadCedarlingOptions } from "./config";

let clientPromise: Promise<CedarlingClient> | undefined;

async function initialize(): Promise<CedarlingClient> {
  const options = await loadCedarlingOptions();
  // The main process owns an independent engine used for authoritative task
  // enforcement and signed-token authorization.
  const result = await createCedarling({
    applicationName: options.applicationName,
    // Electron main still hosts the wasm32 Cedarling engine. Its retry timer
    // cannot provide std::time::Instant, so fail closed on network errors
    // instead of entering the unsupported timer path.
    http: { maxRetries: 0 },
    jwt: { allowedAlgorithms: ["RS256"] },
    policyStore: { type: "inline", document: options.policyStoreDocument },
  });
  if (!result.ok) throw result.error;
  return result.value;
}

export function getCedarling(): Promise<CedarlingClient> {
  // IPC handlers share one engine, while a failed initialization remains
  // retryable.
  clientPromise ??= initialize().catch((error) => {
    clientPromise = undefined;
    throw error;
  });
  return clientPromise;
}

export async function shutDownCedarling(): Promise<void> {
  const pending = clientPromise;
  if (!pending) return;
  clientPromise = undefined;
  const client = await pending.catch(() => undefined);
  if (!client) return;
  // Drain accepted IPC authorization work before Electron exits.
  const result = await client.shutDown();
  if (!result.ok) throw result.error;
}
