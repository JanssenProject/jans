// Follows the shared multi-issuer fixtures: token tags drive policy decisions.
const issuers = ["Acme", "Dolphin"];
const issuerUrl = (name) => `https://${name.toLowerCase()}.example.invalid`;
const policyStore = {
  cedar_version: "v4.0.0",
  policy_stores: {
    qualification: {
      cedar_version: "v4.0.0",
      name: "MultiIssuerQualification",
      trusted_issuers: Object.fromEntries(
        issuers.map((name) => [
          name,
          {
            name,
            description: "Synthetic package-qualification issuer",
            openid_configuration_endpoint: `${issuerUrl(name)}/.well-known/openid-configuration`,
            token_metadata: {
              access_token: { entity_type_name: `${name}::Access_Token` },
            },
          },
        ]),
      ),
      policies: {
        matching_organizations: {
          description: "Require matching organization claims from both issuers",
          creation_date: "2026-01-01T00:00:00Z",
          policy_content: {
            encoding: "none",
            content_type: "cedar",
            body: `permit(principal, action == Qualification::Action::"Read", resource)
              when {
                context has tokens &&
                context.tokens has acme_access_token &&
                context.tokens has dolphin_access_token &&
                context.tokens.acme_access_token.hasTag("org_id") &&
                context.tokens.dolphin_access_token.hasTag("org_id") &&
                context.tokens.acme_access_token.getTag("org_id").contains(resource.org_id) &&
                context.tokens.dolphin_access_token.getTag("org_id").contains(resource.org_id)
              };`,
          },
        },
      },
      schema: {
        encoding: "none",
        content_type: "cedar",
        body: `${issuers
          .map(
            (name) => `namespace ${name} {
          entity TrustedIssuer = { issuer_entity_id: Qualification::Url };
          entity Access_Token = {
            token_type: String, jti: String, iss: TrustedIssuer,
            validated_at: Long, exp: Long, sub?: String, org_id?: String
          } tags Set<String>;
        }`,
          )
          .join("\n")}
        namespace Qualification {
          type Url = { host: String, path: String, protocol: String };
          entity Any;
          entity Resource = { org_id: String };
          action "Read" appliesTo {
            principal: [Any], resource: [Resource], context: Context
          };
        }
        type Context = {
          tokens: {
            acme_access_token?: Acme::Access_Token,
            dolphin_access_token?: Dolphin::Access_Token,
            total_token_count: Long
          }
        };`,
      },
    },
  },
};

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
  CEDARLING_POLICY_STORE_LOCAL: JSON.stringify(policyStore),
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
