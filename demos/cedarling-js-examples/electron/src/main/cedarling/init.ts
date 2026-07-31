import { createCedarling, type CedarlingClient } from "@janssenproject/cedarling";

import { loadCedarlingOptions } from "./config";

let clientPromise: Promise<CedarlingClient> | undefined;

async function initialize(): Promise<CedarlingClient> {
  const options = await loadCedarlingOptions();
  // The main process owns an independent engine used for authoritative task
  // enforcement and signed-token authorization.
  const result = await createCedarling({
    applicationName: options.applicationName,
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
  if (!clientPromise) return;
  const client = await clientPromise;
  clientPromise = undefined;
  // Drain accepted IPC authorization work before Electron exits.
  const result = await client.shutDown();
  if (!result.ok) throw result.error;
}
