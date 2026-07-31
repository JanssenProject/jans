import { createCedarling, type CedarlingClient } from "@janssenproject/cedarling";

let clientPromise: Promise<CedarlingClient> | undefined;

async function initialize(): Promise<CedarlingClient> {
  const options = await window.electron.cedarling.options();
  // Renderer initializes a browser-compatible engine from policy data supplied
  // by main. It never receives main's signed UserInfo token.
  const result = await createCedarling({
    applicationName: options.applicationName,
    jwt: { allowedAlgorithms: ["RS256"] },
    policyStore: { type: "inline", document: options.policyStoreDocument },
  });
  if (!result.ok) throw result.error;
  return result.value;
}

export function getRendererCedarling(): Promise<CedarlingClient> {
  // All renderer components share one engine and can retry after a failed
  // initialization.
  clientPromise ??= initialize().catch((error) => {
    clientPromise = undefined;
    throw error;
  });
  return clientPromise;
}

export async function shutDownRendererCedarling(): Promise<void> {
  if (!clientPromise) return;
  const client = await clientPromise;
  clientPromise = undefined;
  // Drain accepted UI previews before releasing renderer WASM resources.
  const result = await client.shutDown();
  if (!result.ok) throw result.error;
}
