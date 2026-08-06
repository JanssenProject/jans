import type { JWTVerifyGetKey, JWTPayload } from "jose";

interface UserinfoVerification {
  audience: string;
  issuer: string;
  subject: string;
}

export async function verifySignedUserinfoToken(
  token: string,
  key: JWTVerifyGetKey,
  expected: UserinfoVerification,
): Promise<void> {
  const { jwtVerify } = await import("jose");
  const { payload } = await jwtVerify(token, key, {
    algorithms: ["RS256"],
    audience: expected.audience,
    issuer: expected.issuer,
    requiredClaims: ["sub", "iat", "exp"],
  });
  assertUserinfoSubject(payload, expected.subject);
}

export function assertUserinfoSubject(payload: JWTPayload, expected: string): void {
  if (payload.sub !== expected) {
    throw new Error("Authenticated UserInfo subject does not match the requested user");
  }
}

export async function remoteJwks(jwksUri: string): Promise<JWTVerifyGetKey> {
  const url = new URL(jwksUri);
  const loopback = ["localhost", "127.0.0.1", "[::1]"].includes(url.hostname);
  if (
    !["http:", "https:"].includes(url.protocol) ||
    url.username ||
    url.password ||
    (url.protocol !== "https:" && !loopback)
  ) {
    throw new Error("OIDC JWKS endpoint must use HTTPS (loopback HTTP is allowed)");
  }
  const { createRemoteJWKSet } = await import("jose");
  return createRemoteJWKSet(url);
}
