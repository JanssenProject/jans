import {
  createCedarling,
  type CedarlingClient,
  type PolicyStoreDocument,
} from "@janssenproject/cedarling";

const clients = new Map<string, Promise<CedarlingClient>>();

export function validatedIssuer(value: string): string {
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

async function initialize(issuer: string): Promise<CedarlingClient> {
  const [configResponse, policyResponse] = await Promise.all([
    fetch(`${issuer}/config/cedarling`),
    fetch(`${issuer}/config/policy-store`),
  ]);
  if (!configResponse.ok || !policyResponse.ok) {
    throw new Error("Failed to load Cedarling configuration");
  }
  const config = (await configResponse.json()) as Record<string, unknown>;
  const jwt = config.jwt as Record<string, unknown> | undefined;
  if (
    typeof config.applicationName !== "string" ||
    !jwt ||
    !Array.isArray(jwt.allowedAlgorithms) ||
    jwt.allowedAlgorithms.some((algorithm) => algorithm !== "RS256")
  ) {
    throw new Error("Cedarling config must require RS256");
  }
  const document = (await policyResponse.json()) as PolicyStoreDocument;
  // An inline policy snapshot avoids a second runtime-specific fetch from
  // inside WASM after Hono has loaded and validated the provider documents.
  const result = await createCedarling({
    applicationName: config.applicationName,
    jwt: { allowedAlgorithms: ["RS256"] },
    policyStore: { type: "inline", document },
  });
  if (!result.ok) throw result.error;
  return result.value;
}

export function getCedarling(issuerValue: string): Promise<CedarlingClient> {
  const issuer = validatedIssuer(issuerValue);
  // Each isolate reuses one engine per issuer. Failed initialization is evicted
  // so a warm worker can recover from a temporary provider outage.
  let pending = clients.get(issuer);
  if (!pending) {
    pending = initialize(issuer).catch((error) => {
      clients.delete(issuer);
      throw error;
    });
    clients.set(issuer, pending);
  }
  return pending;
}
