const issuers = ["Acme", "Dolphin"];
const issuerUrl = (name) => `https://${name.toLowerCase()}.example.invalid`;

function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/, "");
}

export const exampleConfig = {
  CEDARLING_APPLICATION_NAME: "cedarling-services-example",
  CEDARLING_LOG_TYPE: "memory",
  CEDARLING_LOG_LEVEL: "INFO",
  CEDARLING_LOG_TTL: 120,
  // These generated tokens are only for running the local examples and tests.
  CEDARLING_JWT_SIG_VALIDATION: "disabled",
  CEDARLING_JWT_STATUS_VALIDATION: "disabled",
};

export function createTestTokens() {
  const encoder = new TextEncoder();
  const encode = (value) => base64Url(encoder.encode(JSON.stringify(value)));
  const now = Math.floor(Date.now() / 1000);
  return issuers.map((name) => ({
    mapping: `${name}::Access_Token`,
    payload: `${encode({ alg: "HS256", typ: "JWT" })}.${encode({
      iss: issuerUrl(name),
      sub: "alice",
      jti: `example-${name}`,
      token_type: "Bearer",
      org_id: "example",
      iat: now - 60,
      exp: now + 3600,
    })}.dGVzdA`,
  }));
}

export const issuerUrls = Object.fromEntries(
  issuers.map((name) => [name, issuerUrl(name)]),
);
