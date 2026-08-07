import type { NextRequest } from "next/server";

const DEFAULT_ISSUER = "http://localhost:9090";

export interface OidcDiscovery {
  readonly issuer: string;
  readonly authorization_endpoint: string;
  readonly token_endpoint: string;
  readonly userinfo_endpoint: string;
  readonly jwks_uri: string;
  readonly registration_endpoint: string;
  readonly end_session_endpoint?: string;
}

export interface RegisteredClient {
  readonly clientId: string;
  readonly redirectUri: string;
  readonly postLogoutRedirectUri: string;
  readonly discovery: OidcDiscovery;
}

const globalForOidc = globalThis as typeof globalThis & {
  taskAppDiscovery?: Promise<OidcDiscovery>;
  taskAppRegistrations?: Map<string, Promise<RegisteredClient>>;
};

function loopback(hostname: string): boolean {
  return ["localhost", "127.0.0.1", "[::1]"].includes(hostname);
}

function checkedUrl(value: string, label: string): URL {
  const url = new URL(value);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    (url.protocol !== "https:" && !loopback(url.hostname))
  ) {
    throw new Error(`${label} must use HTTPS without credentials (loopback HTTP is allowed)`);
  }
  return url;
}

export function getRequestOrigin(request: NextRequest): string {
  const configuredOrigin = process.env.APP_ORIGIN;
  if (configuredOrigin) {
    const url = checkedUrl(configuredOrigin, "APP_ORIGIN");
    if (url.origin !== configuredOrigin) throw new Error("APP_ORIGIN must be an origin");
    return url.origin;
  }
  if (process.env.NODE_ENV === "production") {
    throw new Error("APP_ORIGIN is required in production");
  }
  return checkedUrl(request.url, "request URL").origin;
}

function requiredEndpoint(metadata: Record<string, unknown>, key: string): string {
  const value = metadata[key];
  if (typeof value !== "string" || value.length === 0) {
    throw new Error(`OIDC discovery is missing ${key}`);
  }
  return checkedUrl(value, `OIDC discovery ${key}`).href;
}

async function discoverProvider(): Promise<OidcDiscovery> {
  const issuer = checkedUrl(process.env.OIDC_ISSUER ?? DEFAULT_ISSUER, "OIDC_ISSUER");
  if (issuer.href !== `${issuer.origin}/`) {
    throw new Error("OIDC_ISSUER must be an origin without a path, query, or fragment");
  }
  const response = await fetch(new URL("/.well-known/openid-configuration", issuer), {
    headers: { accept: "application/json" },
    cache: "no-store",
  });
  if (!response.ok) throw new Error(`OIDC discovery failed with HTTP ${response.status}`);
  const metadata = (await response.json()) as Record<string, unknown>;
  if (metadata.issuer !== issuer.origin) throw new Error("OIDC discovery issuer mismatch");
  return {
    issuer: issuer.origin,
    authorization_endpoint: requiredEndpoint(metadata, "authorization_endpoint"),
    token_endpoint: requiredEndpoint(metadata, "token_endpoint"),
    userinfo_endpoint: requiredEndpoint(metadata, "userinfo_endpoint"),
    jwks_uri: requiredEndpoint(metadata, "jwks_uri"),
    registration_endpoint: requiredEndpoint(metadata, "registration_endpoint"),
    end_session_endpoint:
      typeof metadata.end_session_endpoint === "string"
        ? requiredEndpoint(metadata, "end_session_endpoint")
        : undefined,
  };
}

export function getDiscovery(): Promise<OidcDiscovery> {
  if (!globalForOidc.taskAppDiscovery) {
    const pending = discoverProvider().catch((error) => {
      delete globalForOidc.taskAppDiscovery;
      throw error;
    });
    globalForOidc.taskAppDiscovery = pending;
  }
  return globalForOidc.taskAppDiscovery;
}

async function registerClient(origin: string): Promise<RegisteredClient> {
  const discovery = await getDiscovery();
  const redirectUri = `${origin}/api/oidc/callback`;
  const postLogoutRedirectUri = `${origin}/api/oidc/logout/callback`;
  const response = await fetch(discovery.registration_endpoint, {
    method: "POST",
    headers: { accept: "application/json", "content-type": "application/json" },
    body: JSON.stringify({
      application_type: "web",
      client_name: "TaskApp Next.js",
      grant_types: ["authorization_code"],
      post_logout_redirect_uris: [postLogoutRedirectUri],
      redirect_uris: [redirectUri],
      response_types: ["code"],
      token_endpoint_auth_method: "none",
      userinfo_signed_response_alg: "RS256",
    }),
    cache: "no-store",
  });
  const registration = (await response.json()) as Record<string, unknown>;
  if (!response.ok || typeof registration.client_id !== "string") {
    throw new Error(`OIDC dynamic client registration failed: HTTP ${response.status}`);
  }
  return {
    clientId: registration.client_id,
    redirectUri,
    postLogoutRedirectUri,
    discovery,
  };
}

export function getRegisteredClient(origin: string): Promise<RegisteredClient> {
  const registrations = globalForOidc.taskAppRegistrations ??= new Map();
  let pending = registrations.get(origin);
  if (!pending) {
    pending = registerClient(origin).catch((error) => {
      registrations.delete(origin);
      throw error;
    });
    registrations.set(origin, pending);
  }
  return pending;
}
