import { createCedarling } from "@janssenproject/cedarling";

const DEFAULT_ISSUER = "http://localhost:9090";
let clientPromise;

function issuerUrl() {
  const value = process.env.OIDC_ISSUER ?? DEFAULT_ISSUER;
  const url = new URL(value);
  if (!['http:', 'https:'].includes(url.protocol) || url.origin !== value) {
    throw new TypeError("OIDC_ISSUER must be an absolute HTTP(S) origin");
  }
  if (
    url.protocol !== "https:" &&
    !["localhost", "127.0.0.1", "[::1]"].includes(url.hostname)
  ) {
    throw new TypeError("OIDC_ISSUER must use HTTPS except on loopback");
  }
  return url.origin;
}

async function createClient() {
  const issuer = issuerUrl();
  const response = await fetch(`${issuer}/config/cedarling`);
  if (!response.ok) {
    throw new Error(`Failed to fetch Cedarling config: HTTP ${response.status}`);
  }
  // The server lets the SDK load the policy store by URL. Cedarling resolves
  // and validates that source as part of client initialization.
  const config = await response.json();
  const result = await createCedarling({
    ...config,
    policyStore: {
      type: "url",
      url: `${issuer}/config/policy-store`,
    },
  });
  if (!result.ok) throw result.error;
  return result.value;
}

export function initCedarling() {
  // Share one engine across requests, but clear a failed attempt so a temporary
  // provider outage does not permanently poison the process.
  clientPromise ??= createClient().catch((error) => {
    clientPromise = undefined;
    throw error;
  });
  return clientPromise;
}

export async function shutDownCedarling() {
  if (!clientPromise) return;
  const client = await clientPromise;
  clientPromise = undefined;
  // shutDown drains accepted work before releasing the underlying WASM engine.
  const result = await client.shutDown();
  if (!result.ok) throw result.error;
}
