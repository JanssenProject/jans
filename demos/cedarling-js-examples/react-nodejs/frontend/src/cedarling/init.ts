import {
  createCedarling,
  type CedarlingClient,
  type PolicyStoreDocument,
  type PolicyStoreSource,
} from "@janssenproject/cedarling";

import { oidcIssuer } from "../oidc";

let clientPromise: Promise<CedarlingClient> | undefined;

function strictConfig(value: unknown) {
  if (!value || typeof value !== "object") throw new Error("Invalid Cedarling config");
  const config = value as Record<string, unknown>;
  const jwt = config.jwt as Record<string, unknown> | undefined;
  if (
    typeof config.applicationName !== "string" ||
    !jwt ||
    !Array.isArray(jwt.allowedAlgorithms) ||
    jwt.allowedAlgorithms.some((item) => item !== "RS256")
  ) {
    throw new Error("Cedarling config must require RS256");
  }
  return { applicationName: config.applicationName, jwt: { allowedAlgorithms: ["RS256"] as const } };
}

async function createClient(): Promise<CedarlingClient> {
  const issuer = oidcIssuer();
  const [configResponse, policyResponse] = await Promise.all([
    fetch(`${issuer}/config/cedarling`),
    fetch(`${issuer}/config/policy-store`),
  ]);
  if (!configResponse.ok || !policyResponse.ok) {
    throw new Error("Failed to load Cedarling configuration");
  }
  const config = strictConfig(await configResponse.json());
  // The browser snapshots the fetched policy document into an inline source;
  // subsequent authorization calls do not depend on another network fetch.
  const document = (await policyResponse.json()) as PolicyStoreDocument;
  const policyStore = {
    type: "inline",
    document,
  } satisfies PolicyStoreSource;
  // createCedarling selects the browser runtime adapter and initializes WASM.
  const result = await createCedarling({
    ...config,
    policyStore,
  });
  if (!result.ok) throw result.error;
  return result.value;
}

export function initCedarling(): Promise<CedarlingClient> {
  // Every component shares one browser engine. A rejected initialization is
  // cleared so a later retry can recover.
  clientPromise ??= createClient().catch((error) => {
    clientPromise = undefined;
    throw error;
  });
  return clientPromise;
}

export async function shutDownCedarling(): Promise<void> {
  if (!clientPromise) return;
  const client = await clientPromise;
  clientPromise = undefined;
  // shutDown waits for accepted checks before releasing the WASM resources.
  const result = await client.shutDown();
  if (!result.ok) throw result.error;
}

if (import.meta.hot) {
  import.meta.hot.dispose(() => void shutDownCedarling());
}
