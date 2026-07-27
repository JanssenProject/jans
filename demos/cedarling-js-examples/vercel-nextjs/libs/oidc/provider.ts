import type { NextRequest } from 'next/server';

const DEFAULT_ISSUER = 'http://localhost:9090';
const CORS_ORIGINS_METADATA = 'urn:custom:client:allowed-cors-origins';

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

interface RegistrationResponse {
  readonly client_id?: unknown;
}

const globalForOidc = globalThis as typeof globalThis & {
  taskAppDiscovery?: Promise<OidcDiscovery>;
  taskAppRegistrations?: Map<string, Promise<RegisteredClient>>;
};

function checkedUrl(value: string, label: string): URL {
  const url = new URL(value);
  if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password) {
    throw new Error(`${label} must be an HTTP(S) URL without credentials`);
  }
  return url;
}

function isLoopbackHostname(hostname: string): boolean {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '[::1]';
}

export function getRequestOrigin(request: NextRequest): string {
  const configuredOrigin = process.env.APP_ORIGIN;
  if (configuredOrigin) {
    return checkedUrl(configuredOrigin, 'APP_ORIGIN').origin;
  }

  const requestUrl = checkedUrl(request.url, 'request URL');
  if (process.env.NODE_ENV === 'production') {
    throw new Error('APP_ORIGIN is required in production');
  }
  if (requestUrl.protocol !== 'https:' && !isLoopbackHostname(requestUrl.hostname)) {
    throw new Error('Development HTTP origins must use a loopback hostname');
  }
  return requestUrl.origin;
}

function requireString(record: Record<string, unknown>, key: string): string {
  const value = record[key];
  if (typeof value !== 'string' || value.length === 0) {
    throw new Error(`OIDC discovery is missing ${key}`);
  }
  checkedUrl(value, `OIDC discovery ${key}`);
  return value;
}

async function discoverProvider(): Promise<OidcDiscovery> {
  const issuer = checkedUrl(process.env.OIDC_ISSUER ?? DEFAULT_ISSUER, 'OIDC_ISSUER');
  const discoveryUrl = new URL('/.well-known/openid-configuration', issuer);
  const response = await fetch(discoveryUrl, {
    headers: { Accept: 'application/json' },
    cache: 'no-store',
  });
  if (!response.ok) {
    throw new Error(`OIDC discovery failed with HTTP ${response.status}`);
  }

  const metadata = (await response.json()) as Record<string, unknown>;
  const discoveredIssuer = requireString(metadata, 'issuer');
  if (discoveredIssuer !== issuer.origin) {
    throw new Error('OIDC discovery issuer does not match OIDC_ISSUER');
  }

  return {
    issuer: discoveredIssuer,
    authorization_endpoint: requireString(metadata, 'authorization_endpoint'),
    token_endpoint: requireString(metadata, 'token_endpoint'),
    userinfo_endpoint: requireString(metadata, 'userinfo_endpoint'),
    jwks_uri: requireString(metadata, 'jwks_uri'),
    registration_endpoint: requireString(metadata, 'registration_endpoint'),
    end_session_endpoint:
      typeof metadata.end_session_endpoint === 'string'
        ? requireString(metadata, 'end_session_endpoint')
        : undefined,
  };
}

export async function getDiscovery(): Promise<OidcDiscovery> {
  if (!globalForOidc.taskAppDiscovery) {
    const pending = discoverProvider();
    globalForOidc.taskAppDiscovery = pending;
    void pending.catch(() => {
      if (globalForOidc.taskAppDiscovery === pending) {
        delete globalForOidc.taskAppDiscovery;
      }
    });
  }
  return globalForOidc.taskAppDiscovery;
}

async function registerClient(origin: string): Promise<RegisteredClient> {
  const discovery = await getDiscovery();
  const redirectUri = `${origin}/api/oidc/callback`;
  const postLogoutRedirectUri = `${origin}/api/oidc/logout/callback`;
  const response = await fetch(discovery.registration_endpoint, {
    method: 'POST',
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      client_name: 'TaskApp Next.js',
      application_type: 'web',
      redirect_uris: [redirectUri],
      post_logout_redirect_uris: [postLogoutRedirectUri],
      response_types: ['code'],
      grant_types: ['authorization_code'],
      token_endpoint_auth_method: 'none',
      userinfo_signed_response_alg: 'RS256',
      [CORS_ORIGINS_METADATA]: [origin],
    }),
    cache: 'no-store',
  });
  const registration = (await response.json()) as RegistrationResponse & {
    error?: unknown;
    error_description?: unknown;
  };
  if (!response.ok || typeof registration.client_id !== 'string') {
    const detail =
      typeof registration.error_description === 'string'
        ? registration.error_description
        : typeof registration.error === 'string'
          ? registration.error
          : `HTTP ${response.status}`;
    throw new Error(`OIDC dynamic client registration failed: ${detail}`);
  }

  return {
    clientId: registration.client_id,
    redirectUri,
    postLogoutRedirectUri,
    discovery,
  };
}

export async function getRegisteredClient(origin: string): Promise<RegisteredClient> {
  const registrations =
    globalForOidc.taskAppRegistrations ??
    (globalForOidc.taskAppRegistrations = new Map());
  const existing = registrations.get(origin);
  if (existing) return existing;

  const pending = registerClient(origin);
  registrations.set(origin, pending);
  void pending.catch(() => {
    if (registrations.get(origin) === pending) registrations.delete(origin);
  });
  return pending;
}
