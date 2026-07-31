import type { PolicyStoreDocument } from "@janssenproject/cedarling";

import type { RendererCedarlingOptions } from "../../shared/contracts";

export function oidcIssuer(): string {
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

export function oidcAllowsInsecureRequests(): boolean {
  return new URL(oidcIssuer()).protocol === "http:";
}

export async function loadCedarlingOptions(): Promise<RendererCedarlingOptions> {
  const issuer = oidcIssuer();
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
  // Main validates the provider documents once, then gives the renderer only
  // configuration and policies—never the signed session token.
  return {
    applicationName: config.applicationName,
    policyStoreDocument: (await policyResponse.json()) as PolicyStoreDocument,
  };
}
