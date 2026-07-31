import { createCedarling, type CedarlingClient } from "@janssenproject/cedarling";

const globalForCedarling = globalThis as typeof globalThis & {
  taskAppCedarlingClient?: Promise<CedarlingClient>;
};

function issuerUrl(): string {
  const value = process.env.OIDC_ISSUER ?? "http://localhost:9090";
  const url = new URL(value);
  const loopback = ["localhost", "127.0.0.1", "[::1]"].includes(url.hostname);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.origin !== value ||
    (url.protocol !== "https:" && !loopback)
  ) {
    throw new Error("OIDC_ISSUER must be an HTTPS origin (loopback HTTP is allowed)");
  }
  return url.origin;
}

async function initializeCedarling(): Promise<CedarlingClient> {
  const issuer = issuerUrl();
  const response = await fetch(`${issuer}/config/cedarling`);
  if (!response.ok) throw new Error(`Failed to load Cedarling config: HTTP ${response.status}`);
  const config = (await response.json()) as Record<string, unknown>;
  const jwt = config.jwt as Record<string, unknown> | undefined;
  if (
    typeof config.applicationName !== "string" ||
    !jwt ||
    !Array.isArray(jwt.allowedAlgorithms) ||
    jwt.allowedAlgorithms.some((algorithm) => algorithm !== "RS256")
  ) {
    throw new Error("Cedarling config must require RS256");
  }
  // The SDK resolves this URL policy source in whichever Next.js runtime owns
  // the importing route.
  const result = await createCedarling({
    applicationName: config.applicationName,
    jwt: { allowedAlgorithms: ["RS256"] },
    policyStore: { type: "url", url: `${issuer}/config/policy-store` },
  });
  if (!result.ok) throw result.error;
  return result.value;
}

export function getCedarling(): Promise<CedarlingClient> {
  // Next.js can reload modules while retaining globalThis, so cache one engine
  // per worker and clear rejected initialization for a later retry.
  if (!globalForCedarling.taskAppCedarlingClient) {
    const pending = initializeCedarling().catch((error) => {
      delete globalForCedarling.taskAppCedarlingClient;
      throw error;
    });
    globalForCedarling.taskAppCedarlingClient = pending;
  }
  return globalForCedarling.taskAppCedarlingClient;
}
