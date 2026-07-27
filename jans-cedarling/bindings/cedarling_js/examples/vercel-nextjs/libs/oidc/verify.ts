import {
  createRemoteJWKSet,
  jwtVerify,
  type JWTPayload,
} from 'jose';
import type { OidcDiscovery } from './provider';

const globalForJwks = globalThis as typeof globalThis & {
  taskAppJwks?: Map<string, ReturnType<typeof createRemoteJWKSet>>;
};

function getJwks(jwksUri: string) {
  const stores = globalForJwks.taskAppJwks ?? (globalForJwks.taskAppJwks = new Map());
  const existing = stores.get(jwksUri);
  if (existing) return existing;
  const created = createRemoteJWKSet(new URL(jwksUri));
  stores.set(jwksUri, created);
  return created;
}

export async function verifyIdToken(
  token: string,
  discovery: OidcDiscovery,
  clientId: string,
  nonce: string,
): Promise<JWTPayload> {
  const { payload } = await jwtVerify(token, getJwks(discovery.jwks_uri), {
    algorithms: ['RS256'],
    issuer: discovery.issuer,
    audience: clientId,
    requiredClaims: ['sub', 'iat', 'exp', 'nonce'],
  });
  if (typeof payload.sub !== 'string' || payload.sub.length === 0) {
    throw new Error('ID token is missing sub');
  }
  if (payload.nonce !== nonce) {
    throw new Error('ID token nonce does not match the authorization request');
  }
  return payload;
}

export async function verifyUserinfoToken(
  token: string,
  discovery: OidcDiscovery,
  clientId: string,
  expectedSubject?: string,
): Promise<JWTPayload> {
  const { payload } = await jwtVerify(token, getJwks(discovery.jwks_uri), {
    algorithms: ['RS256'],
    issuer: discovery.issuer,
    audience: clientId,
    requiredClaims: ['sub', 'iat', 'exp'],
  });
  if (typeof payload.sub !== 'string' || payload.sub.length === 0) {
    throw new Error('Userinfo token is missing sub');
  }
  if (expectedSubject && payload.sub !== expectedSubject) {
    throw new Error('ID token and userinfo token subjects do not match');
  }
  return payload;
}
