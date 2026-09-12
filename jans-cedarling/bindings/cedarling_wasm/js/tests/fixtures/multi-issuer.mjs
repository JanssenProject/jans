// Follows the shared multi-issuer fixtures: token tags drive policy decisions.
const issuers = ["Acme", "Dolphin"];
const issuerUrl = (name) => `https://${name.toLowerCase()}.example.invalid`;
function base64Url(bytes) {
  return btoa(String.fromCharCode(...bytes))
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replace(/=+$/, "");
}

export async function testTokens() {
  const encoder = new TextEncoder();
  const encode = (value) => base64Url(encoder.encode(JSON.stringify(value)));
  // Public test-only key; signature verification is deliberately disabled below.
  const key = await crypto.subtle.importKey(
    "raw",
    encoder.encode("cedarling-package-qualification-test-only-key"),
    { name: "HMAC", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const now = Math.floor(Date.now() / 1000);
  return Promise.all(
    issuers.map(async (name) => {
      const data = `${encode({ alg: "HS256", typ: "JWT" })}.${encode({
        iss: issuerUrl(name),
        sub: "alice",
        jti: `qualification-${name}`,
        token_type: "Bearer",
        org_id: "example",
        iat: now - 60,
        exp: now + 3600,
      })}`;
      const signature = await crypto.subtle.sign(
        "HMAC",
        key,
        encoder.encode(data),
      );
      return {
        mapping: `${name}::Access_Token`,
        payload: `${data}.${base64Url(new Uint8Array(signature))}`,
      };
    }),
  );
}

export const multiIssuerConfig = {
  CEDARLING_APPLICATION_NAME: "multi-issuer-qualification",
  CEDARLING_LOG_TYPE: "memory",
  CEDARLING_LOG_TTL: 120,
  // Matches the binding examples: exercise mapping and decisions, not crypto validation.
  CEDARLING_JWT_SIG_VALIDATION: "disabled",
  CEDARLING_JWT_STATUS_VALIDATION: "disabled",
  CEDARLING_JWT_SIGNATURE_ALGORITHMS_SUPPORTED: ["HS256"],
};

export const multiIssuerRequest = (tokens, orgId = "example") =>
  JSON.stringify({
    tokens,
    action: 'Qualification::Action::"Read"',
    resource: {
      cedar_entity_mapping: {
        entity_type: "Qualification::Resource",
        id: "document",
      },
      org_id: orgId,
    },
    context: {},
  });
